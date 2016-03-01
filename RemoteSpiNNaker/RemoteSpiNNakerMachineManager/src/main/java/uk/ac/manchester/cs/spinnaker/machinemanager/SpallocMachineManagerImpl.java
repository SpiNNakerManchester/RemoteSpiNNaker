package uk.ac.manchester.cs.spinnaker.machinemanager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.manchester.cs.spinnaker.machine.SpinnakerMachine;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.Command;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.CreateJobCommand;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.DestroyJobCommand;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.GetJobMachineInfoCommand;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.GetJobStateCommand;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.JobKeepAliveCommand;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.NoNotifyJobCommand;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.NotifyJobCommand;
import uk.ac.manchester.cs.spinnaker.machinemanager.responses.JobMachineInfo;
import uk.ac.manchester.cs.spinnaker.machinemanager.responses.JobState;
import uk.ac.manchester.cs.spinnaker.machinemanager.responses.JobsChangedResponse;
import uk.ac.manchester.cs.spinnaker.machinemanager.responses.Response;
import uk.ac.manchester.cs.spinnaker.machinemanager.responses.ReturnResponse;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class SpallocMachineManagerImpl extends Thread
        implements MachineManager {

    private static final double CHIPS_PER_BOARD = 48.0;

    private static final String MACHINE_VERSION = "5";

    private String ipAddress = null;

    private int port = -1;

    private Socket socket = null;

    private BufferedReader reader = null;

    private PrintWriter writer = null;

    private ObjectMapper mapper = new ObjectMapper();

    private Queue<ReturnResponse> responses = new LinkedList<ReturnResponse>();

    private Queue<JobsChangedResponse> notifications =
        new LinkedList<JobsChangedResponse>();

    private Map<Integer, SpinnakerMachine> machinesAllocated =
        new HashMap<Integer, SpinnakerMachine>();

    private Map<SpinnakerMachine, Integer> jobByMachine =
        new HashMap<SpinnakerMachine, Integer>();

    private Map<Integer, JobState> machineState =
        new HashMap<Integer, JobState>();

    private Map<Integer, MachineNotificationReceiver> callbacks =
        new HashMap<Integer, MachineNotificationReceiver>();

    private Log logger = LogFactory.getLog(getClass());

    private Integer connectSync = new Integer(0);

    private boolean connected = false;

    private boolean done = false;

    private MachineNotificationReceiver callback = null;

    private String machine = null;

    private String owner = null;

    public SpallocMachineManagerImpl(String ipAddress, int port,
            String machine, String owner) {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Response.class, new ResponseBasedDeserializer());
        mapper.registerModule(module);
        mapper.setPropertyNamingStrategy(
            PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        mapper.configure(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.ipAddress = ipAddress;
        this.port = port;
        this.machine = machine;
        this.owner = owner;
    }

    private <T> T getNextResponse(Class<T> responseType) throws IOException {
        ReturnResponse response = null;
        synchronized (responses) {
            while (responses.isEmpty()) {
                try {
                    responses.wait();
                } catch (InterruptedException e) {

                    // Does Nothing
                }
            }
            response = responses.poll();
        }
        if (responseType != null) {
            return mapper.readValue(response.getReturnValue(), responseType);
        }
        return null;
    }

    private void waitForConnection() {
        synchronized (connectSync) {
            while (!connected) {
                logger.debug("Waiting for connection");
                try {
                    connectSync.wait();
                } catch (InterruptedException e) {

                    // Does Nothing
                }
            }
        }
    }

    private synchronized <T> T sendRequest(
            Command<?, ?> request, Class<T> responseType)
            throws IOException {
        waitForConnection();
        logger.trace("Sending message of type " + request.getCommand());
        writer.println(mapper.writeValueAsString(request));
        writer.flush();
        return getNextResponse(responseType);
    }

    private synchronized void sendRequest(Command<?, ?> request)
            throws IOException {
        waitForConnection();
        logger.trace("Sending message of type " + request.getCommand());
        writer.println(mapper.writeValueAsString(request));
        writer.flush();
        getNextResponse(null);
    }

    private SpinnakerMachine getMachineForJob(int jobId) throws IOException {
        JobMachineInfo info = sendRequest(
            new GetJobMachineInfoCommand(jobId), JobMachineInfo.class);
        return new SpinnakerMachine(
            info.getConnections().get(0).getHostname(),
            MACHINE_VERSION, info.getWidth(),
            info.getHeight(), "None");
    }

    private void waitForState(int jobId, int state) {
        synchronized (machineState) {
            while (!machineState.containsKey(jobId) ||
                    machineState.get(jobId).getState() != state) {
                logger.debug(
                    "Waiting for job " + jobId + " to get to state " + state);
                try {
                    machineState.wait();
                } catch (InterruptedException e) {

                    // Does Nothing
                }
            }
        }
    }

    @Override
    public SpinnakerMachine getNextAvailableMachine(int nChips) {
        double n_boards = (double) nChips / CHIPS_PER_BOARD;
        if (n_boards - (int) n_boards > 0.8) {
            n_boards += 1.0;
        }
        n_boards = Math.ceil(n_boards);
        if (n_boards > 1) {
            n_boards = Math.ceil(n_boards / 3) * 3;
        }

        SpinnakerMachine machineAllocated = null;
        while (machineAllocated == null) {
            try {
                int jobId = sendRequest(new CreateJobCommand(
                    (int) nChips, machine, owner), Integer.class);
                machineAllocated = getMachineForJob(jobId);
                machinesAllocated.put(jobId, machineAllocated);
                jobByMachine.put(machineAllocated, jobId);
                if (callback != null) {
                    callbacks.put(jobId, callback);
                }
                logger.debug(
                    "Got machine " + jobId + ", requesting notifications");
                sendRequest(new NotifyJobCommand(jobId));
                JobState state = sendRequest(
                    new GetJobStateCommand(jobId), JobState.class);
                synchronized (machineState) {
                    machineState.put(jobId, state);
                }
                logger.debug("Notifications for " + jobId + " are on");
                waitForState(jobId, JobState.READY);
            } catch (IOException e) {
                logger.error("Error getting machine - retrying", e);
            }
        }

        return machineAllocated;
    }

    @Override
    public void releaseMachine(SpinnakerMachine machine) {
        Integer jobId = jobByMachine.remove(machine);
        if (jobId != null) {
            try {
                logger.debug("Turning off notification for " + jobId);
                sendRequest(new NoNotifyJobCommand(jobId));
                logger.debug("Notifications for " + jobId + " are off");
                machinesAllocated.remove(jobId);
                callbacks.remove(jobId);
                sendRequest(new DestroyJobCommand(jobId));
                logger.debug("Job " + jobId + " destroyed");
            } catch (IOException e) {
                logger.error("Error releasing machine for " + jobId);
            }
        }
    }

    @Override
    public boolean isMachineAvailable(SpinnakerMachine machine) {
        Integer jobId = jobByMachine.remove(machine);
        if (jobId != null) {
            logger.debug("Job " + jobId + " still available");
            return true;
        }
        return false;
    }

    public void close() {
        done = true;
        connected = false;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {

                // Does Nothing
            }
        }

    }

    public void run() {

        NotificationHandler handler = new NotificationHandler();
        handler.start();

        ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
            new KeepAlive(), 5, 5, TimeUnit.SECONDS);

        while (!done) {
            try {
                synchronized (connectSync) {
                    socket = new Socket(ipAddress, port);
                    reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                    writer = new PrintWriter(socket.getOutputStream());

                    connected = true;
                    connectSync.notifyAll();
                }
                while (connected) {
                    try {
                        String line = reader.readLine();
                        if (line != null) {
                            Response response = mapper.readValue(
                                line, Response.class);
                            logger.trace(
                                "Received response of type " + response);
                            if (response instanceof ReturnResponse) {
                                synchronized (responses) {
                                    responses.add((ReturnResponse) response);
                                    responses.notifyAll();
                                }
                            } else if (response instanceof
                                    JobsChangedResponse) {
                                synchronized (notifications) {
                                    notifications.add(
                                        (JobsChangedResponse) response);
                                    notifications.notifyAll();
                                }
                            } else {
                                logger.error(
                                    "Unrecognized response: " + response);
                            }
                        } else {
                            synchronized (connectSync) {
                                connected = false;
                                connectSync.notifyAll();
                            }
                        }
                    } catch (IOException e) {
                        if (!done) {
                            logger.error("Error receiving", e);
                        }
                    }
                }
            } catch (IOException e) {
                if (!done) {
                    logger.error("Could not connect to machine server", e);
                }
            }
            if (!done) {
                logger.warn("Disconnected from machine server...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                    // Does Nothing
                }
            }
        }

        scheduler.shutdownNow();
        synchronized (notifications) {
            notifications.notifyAll();
        }
    }

    private class NotificationHandler extends Thread {

        public void notify(JobsChangedResponse response) {
            for (int job : response.getJobsChanged()) {
                try {
                    logger.debug("Getting state of " + job);
                    JobState state = sendRequest(
                        new GetJobStateCommand(job), JobState.class);
                    logger.debug(
                        "Job " + job + " is in state " + state.getState());
                    synchronized (machineState) {
                        machineState.put(job, state);
                        machineState.notifyAll();
                    }
                    if (state.getState() == JobState.DESTROYED) {
                        SpinnakerMachine machine =
                            machinesAllocated.remove(job);
                        if (machine != null) {
                            jobByMachine.remove(machine);
                            MachineNotificationReceiver callback =
                                callbacks.get(job);
                            if (callback != null) {
                                callback.machineUnallocated(machine);
                            }
                        } else {
                            logger.error("Unrecognized job: " + job);
                        }
                    }
                } catch (IOException e) {
                    logger.error("Error getting job state", e);
                }
            }
        }

        public void run() {
            while (!done) {

                JobsChangedResponse response = null;
                synchronized (notifications) {
                    while (!done && notifications.isEmpty()) {
                        try {
                            notifications.wait();
                        } catch (InterruptedException e) {

                            // Does Nothing
                        }
                    }
                    if (!done) {
                        response = notifications.poll();
                    }
                }

                if (response != null) {
                    notify(response);
                }
            }
        }
    }

    private class KeepAlive implements Runnable {
        public void run() {
            for (int jobId : machinesAllocated.keySet()) {
                try {
                    sendRequest(new JobKeepAliveCommand(jobId));
                } catch (IOException e) {
                    logger.error("Error keeping machine " + jobId + " alive");
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        SpallocMachineManagerImpl manager =
            new SpallocMachineManagerImpl(
                "10.0.0.3", 22244, "fake-48-board-machine", "test");
        manager.start();
        SpinnakerMachine machine = manager.getNextAvailableMachine(5);
        System.err.println(
            "Machine " + machine.getMachineName() + " allocated");
        Thread.sleep(20000);
        System.err.println(
            "Machine " + machine.getMachineName() + " is available: " +
            manager.isMachineAvailable(machine));
        manager.releaseMachine(machine);
        System.err.println(
            "Machine " + machine.getMachineName() + " deallocated");
        manager.close();
    }
}