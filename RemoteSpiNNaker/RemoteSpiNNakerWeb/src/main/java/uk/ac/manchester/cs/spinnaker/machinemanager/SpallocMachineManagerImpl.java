/*
 * Copyright (c) 2014-2019 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.cs.spinnaker.machinemanager;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.cs.spinnaker.machinemanager.responses.JobState.DESTROYED;
import static uk.ac.manchester.cs.spinnaker.machinemanager.responses.JobState.READY;
import static uk.ac.manchester.cs.spinnaker.utils.ThreadUtils.sleep;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import uk.ac.manchester.cs.spinnaker.machine.ChipCoordinates;
import uk.ac.manchester.cs.spinnaker.machine.SpinnakerMachine;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.Command;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.CreateJobCommand;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.DestroyJobCommand;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.GetJobMachineInfoCommand;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.GetJobStateCommand;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.JobKeepAliveCommand;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.ListMachinesCommand;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.NoNotifyJobCommand;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.NotifyJobCommand;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.PowerOffJobBoardsCommand;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.PowerOnJobBoardsCommand;
import uk.ac.manchester.cs.spinnaker.machinemanager.commands.WhereIsCommand;
import uk.ac.manchester.cs.spinnaker.machinemanager.responses.WhereIs;
import uk.ac.manchester.cs.spinnaker.machinemanager.responses.ExceptionResponse;
import uk.ac.manchester.cs.spinnaker.machinemanager.responses.JobMachineInfo;
import uk.ac.manchester.cs.spinnaker.machinemanager.responses.JobState;
import uk.ac.manchester.cs.spinnaker.machinemanager.responses.JobsChangedResponse;
import uk.ac.manchester.cs.spinnaker.machinemanager.responses.Machine;
import uk.ac.manchester.cs.spinnaker.machinemanager.responses.Response;
import uk.ac.manchester.cs.spinnaker.machinemanager.responses.ReturnResponse;
import uk.ac.manchester.cs.spinnaker.rest.utils.PropertyBasedDeserialiser;

/**
 * A machine manager that interfaces to the spalloc service.
 */
public class SpallocMachineManagerImpl implements MachineManager, Runnable {

    /**
     * The default version of a machine.
     */
    private static final String MACHINE_VERSION = "5";

    /**
     * The tag indicating the machine should be picked by default.
     */
    private static final String DEFAULT_TAG = "default";

    /**
     * The keep-alive period in seconds.
     */
    private static final int PERIOD = 5;

    /**
     * The scaling from width in triads to width in chips.
     */
    private static final int MACHINE_WIDTH_FACTOR = 12;

    /**
     * The scaling from height in triads to height in chips.
     */
    private static final int MACHINE_HEIGHT_FACTOR = 12;

    /**
     * The number of times to retry a spalloc request.
     */
    private static final int N_RETRIES = 3;

    /**
     * The time to wait for a response from spalloc.
     */
    private static final long TIMEOUT_SECONDS = 1;

    /**
     * Used for callbacks about machines.
     */
    public interface MachineNotificationReceiver {
        /**
         * Indicates that a machine is no longer allocated.
         *
         * @param machine
         *            The machine that is no longer allocated
         */
        void machineUnallocated(SpinnakerMachine machine);
    }

    /**
     * The spalloc server address.
     */
    @Value("${spalloc.server}")
    private String ipAddress;

    /**
     * The spalloc server port.
     */
    @Value("${spalloc.port}")
    private int port;

    /**
     * The owner to give spalloc jobs from this client.
     */
    @Value("${spalloc.user.name}")
    private String owner;

    /**
     * Unmarshaller of objects.
     */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * The machines that have been allocated by job ID.
     */
    private final Map<Integer, SpinnakerMachine> machinesAllocated =
            new HashMap<>();

    /**
     * A map from machine to job.
     */
    private final Map<SpinnakerMachine, SpallocJob> jobByMachine =
            new HashMap<>();

    /**
     * The state of the spalloc job by job ID.
     */
    private final Map<Integer, JobState> machineState = new HashMap<>();

    /**
     * The callback handler by job ID.
     */
    private final Map<Integer, MachineNotificationReceiver> callbacks =
            new HashMap<>();

    /**
     * Logging.
     */
    private final Logger logger = getLogger(getClass());

    /**
     * Communication management.
     */
    private final Comms comms = new Comms();

    /**
     * True when the manager is finished with.
     */
    private volatile boolean done = false;

    /**
     * General callback handler when one doesn't exist.
     */
    private final MachineNotificationReceiver callback = null;

    /**
     * Deserialiser for spalloc responses.
     */
    @SuppressWarnings("serial")
    private static class ResponseBasedDeserializer
            extends PropertyBasedDeserialiser<Response> {

        /**
         * Subclass initialiser.
         */
        ResponseBasedDeserializer() {
            super(Response.class);
            register("jobs_changed", JobsChangedResponse.class);
            register("return", ReturnResponse.class);
            register("exception", ExceptionResponse.class);
        }
    }

    /**
     * Make a machine manager that talks to Spalloc to do its work.
     */
    @SuppressWarnings("deprecation")
    public SpallocMachineManagerImpl() {
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(Response.class, new ResponseBasedDeserializer());
        mapper.registerModule(module);
        mapper.setPropertyNamingStrategy(
                CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Thread pool.
     */
    private ScheduledExecutorService scheduler;

    /**
     * Launch this manager's threads.
     */
    @PostConstruct
    private void startThreads() {
        final ThreadGroup group = new ThreadGroup("Spalloc");
        scheduler = newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable r) {
                return new Thread(group, r, "Spalloc Keep Alive Handler");
            }
        });

        new Thread(group, this, "Spalloc Comms Interface").start();

        final Thread t = new Thread(group, new Runnable() {
            @Override
            public void run() {
                updateStateOfJobs();
            }
        }, "Spalloc JobState Update Notification Handler");
        t.setDaemon(true);
        t.start();

        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                keepAllJobsAlive();
            }
        }, PERIOD, PERIOD, SECONDS);
    }

    // ------------------------------ COMMS ------------------------------

    /**
     * Wait for the given object.
     *
     * @param obj The object to wait for
     * @return True if the wait was interrupted, false otherwise
     */
    private static boolean waitfor(final Object obj) {
        try {
            obj.wait();
            return false;
        } catch (final InterruptedException e) {
            return true;
        }
    }

    /**
     * Communications API wrapper.
     */
    class Comms {

        /**
         * Internal exception for a timeout.
         */
        class TimeoutException extends IOException {

            private static final long serialVersionUID = 1L;

            public TimeoutException(String message) {
                super(message);
            }
        }

        /**
         * The responses from spalloc to be read.
         */
        private final BlockingQueue<Response> responses =
                new LinkedBlockingQueue<>();

        /**
         * The notifications from spalloc to be raised.
         */
        private final BlockingQueue<JobsChangedResponse> notifications =
                new LinkedBlockingQueue<>();

        /**
         * Connection to server.
         */
        private Socket socket;

        /**
         * Reader from server.
         */
        private BufferedReader reader;

        /**
         * Writer to server.
         */
        private PrintWriter writer;

        /**
         * True if connected.
         */
        private volatile boolean connected = false;

        /**
         * Get the next response.
         *
         * @param responseType The type expected.
         * @param <T> The type of the response.
         * @return The decoded response, or null if cancelled.
         * @throws IOException If the response failed to be read.
         */
        private <T> T getNextResponse(final Class<T> responseType)
                throws IOException {
            Response response;
            try {
                response = responses.poll(TIMEOUT_SECONDS, SECONDS);
                if (response == null) {
                    throw new TimeoutException(
                            "No response from spalloc server");
                }
            } catch (final InterruptedException e) {
                return null;
            }
            if (response instanceof ExceptionResponse) {
                throw new IOException(
                        ((ExceptionResponse) response).getException());
            }
            if (response instanceof ReturnResponse) {
                if (responseType == null) {
                    return null;
                }
                return mapper.readValue(
                        ((ReturnResponse) response).getReturnValue(),
                        responseType);
            }
            // Should never happen!
            throw new IOException("Unknown Response " + response);
        }

        /**
         * Wait for the connection to be established.
         */
        private synchronized void waitForConnection() {
            while (!connected) {
                logger.debug("Waiting for connection");
                if (waitfor(this)) {
                    break;
                }
            }
        }

        /**
         * Write the given command as a request to the server.
         *
         * @param request The request to issue
         * @throws IOException If an error occurs
         */
        private void writeRequest(final Command<?> request) throws IOException {
            String message = mapper.writeValueAsString(request);
            logger.trace("Sending message " + message);
            writer.println(message);
            writer.flush();
        }

        /**
         * Read a response from the server.
         *
         * @throws IOException if an error occurred
         */
        private void readResponse() throws IOException {
            // Note, assumes one response per line
            final String line = reader.readLine();
            if (line == null) {
                synchronized (this) {
                    connected = false;
                    notifyAll();
                }
                return;
            }

            logger.trace("Received response: " + line);
            final Response response = mapper.readValue(line, Response.class);
            logger.trace("Received response of type " + response);
            if (response instanceof ReturnResponse
                    || response instanceof ExceptionResponse) {
                responses.offer(response);
            } else if (response instanceof JobsChangedResponse) {
                notifications.offer((JobsChangedResponse) response);
            } else {
                logger.error("Unrecognized response: " + response);
            }
        }

        /**
         * How long to wait after disconnecting before reconnecting. In
         * milliseconds.
         */
        private static final int POST_DISCONNECT_PAUSE = 1000;

        /**
         * The main connection management loop. This will reconnect to the
         * server if it gets disconnected by surprise.
         */
        public void mainLoop() {
            while (!done) {
                try {
                    connect();
                } catch (final IOException e) {
                    if (!done) {
                        logger.error("Could not connect to machine server", e);
                    }
                }
                try {
                    while (connected) {
                        readResponse();
                    }
                } catch (final IOException e) {
                    if (!done) {
                        logger.error("Error receiving", e);
                        disconnect();
                    }
                }
                if (!done) {
                    logger.warn("Disconnected from machine server...");
                    sleep(POST_DISCONNECT_PAUSE);
                }
            }
        }

        /**
         * Connect to the Spalloc server.
         *
         * @throws IOException
         *             If anything goes wrong
         */
        public synchronized void connect() throws IOException {
            socket = new Socket(ipAddress, port);
            reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());

            connected = true;
            // Send an empty JCR over
            notifications.offer(new JobsChangedResponse());
            notifyAll();
        }

        /**
         * Close an object ignoring errors.
         * @param closable The object to close
         */
        private void closeQuietly(final Closeable closable) {
            try {
                closable.close();
            } catch (IOException e) {
                // Ignore error
            }
        }

        /**
         * Disconnect from the Spalloc server.
         */
        public void disconnect() {
            connected = false;
            closeQuietly(writer);
            closeQuietly(reader);
            closeQuietly(socket);
        }

        /**
         * Send a request that expects a response that needs to be deserialised.
         *
         * @param <T>
         *            The type of the response.
         * @param request
         *            The request to send.
         * @param responseType
         *            The expected type of response.
         * @return The response.
         * @throws IOException
         *             If anything goes wrong
         */
        public <T> T sendRequest(final Command<?> request,
                final Class<T> responseType) throws IOException {
            synchronized (SpallocMachineManagerImpl.this) {
                IOException lastError = null;
                for (int i = 0; i < N_RETRIES; i++) {
                    try {
                        waitForConnection();
                        writeRequest(request);
                        return getNextResponse(responseType);
                    } catch (IOException e) {
                        // Disconnect on an error to force reconnection
                        disconnect();
                        lastError = e;
                    }
                }
                throw lastError;
            }
        }

        /**
         * Send a request that doesn't expect a response that needs to be
         * deserialised (that is, a generic OK).
         *
         * @param request
         *            The request to send.
         * @throws IOException
         *             If anything goes wrong
         */
        public void sendRequest(final Command<?> request) throws IOException {
            sendRequest(request, null);
        }

        /**
         * Get the list of IDs of changed jobs from the next notification in the
         * notification queue.
         *
         * @return list of IDs
         * @throws InterruptedException
         *             if interrupted
         */
        public List<Integer> getJobsChanged() throws InterruptedException {
            return notifications.take().getJobsChanged();
        }
    }

    @Override
    public void close() {
        done = true;
        comms.disconnect();
    }

    @Override
    public void run() {
        try {
            comms.mainLoop();
        } finally {
            scheduler.shutdownNow();
        }
    }

    // ------------------------------ WIRE Job ------------------------------

    /**
     * Interface to an existing spalloc job.
     */
    final class SpallocJob {

        /**
         * Used for the Hash code.
         */
        private static final int MAGIC = 0xbadf00d;

        /** The spalloc job ID. */
        private final int id;


        /**
         * Make a job handle.
         *
         * @param jobId
         *            The ID code of the job.
         */
        SpallocJob(final int jobId) {
            this.id = jobId;
        }

        /**
         * Get what machine the job has been allocated to.
         *
         * @return The machine descriptor.
         * @throws IOException
         *             If anything goes wrong
         */
        JobMachineInfo getMachineInfo() throws IOException {
            return comms.sendRequest(new GetJobMachineInfoCommand(id),
                    JobMachineInfo.class);
        }

        /**
         * Get the state of the job.
         *
         * @return The state descriptor.
         * @throws IOException
         *             If anything goes wrong
         */
        JobState getState() throws IOException {
            return comms.sendRequest(new GetJobStateCommand(id),
                    JobState.class);
        }

        /**
         * Enable or disable notifications about this job's state changes.
         *
         * @param enable
         *            True to turn the notifications on.
         * @throws IOException
         *             If anything goes wrong
         */
        void notify(final boolean enable) throws IOException {
            if (enable) {
                comms.sendRequest(new NotifyJobCommand(id));
            } else {
                comms.sendRequest(new NoNotifyJobCommand(id));
            }
        }

        /**
         * Keep the job alive (by sending effectively a NOP).
         *
         * @throws IOException
         *             If anything goes wrong
         */
        void keepAlive() throws IOException {
            comms.sendRequest(new JobKeepAliveCommand(id));
        }

        /**
         * Destroy the job.
         *
         * @throws IOException
         *             If anything goes wrong
         */
        void destroy() throws IOException {
            comms.sendRequest(new DestroyJobCommand(id));
        }

        /**
         * Turn power on or off for a job's boards.
         *
         * @param powerOn
         *            True to turn the boards on, false to turn them off.
         * @throws IOException
         *             If anything goes wrong
         */
        void power(final boolean powerOn) throws IOException {
            if (powerOn) {
                comms.sendRequest(new PowerOnJobBoardsCommand(id));
            } else {
                comms.sendRequest(new PowerOffJobBoardsCommand(id));
            }
        }

        /**
         * Find one of his job's chips.
         *
         * @param chipX
         *            The x coordinate of the chip
         * @param chipY
         *            The y coordinate of the chip
         * @return The location description
         * @throws IOException
         *             If anything goes wrong
         */
        WhereIs whereIs(final int chipX, final int chipY) throws IOException {
            return comms.sendRequest(new WhereIsCommand(id, chipX, chipY),
                    WhereIs.class);
        }

        @Override
        public int hashCode() {
            return id | MAGIC;
        }

        @Override
        public boolean equals(final Object o) {
            return (o instanceof SpallocJob) && (((SpallocJob) o).id == id);
        }
    }

    /**
     * Get the machines known to the Spalloc server.
     *
     * @return The known machines
     * @throws IOException If anything goes wrong
     */
    final Machine[] listMachines() throws IOException {
        return comms.sendRequest(new ListMachinesCommand(), Machine[].class);
    }

    /**
     * Create a Spalloc job.
     *
     * @param nBoards
     *            The number of boards to ask for
     * @return The job handle
     * @throws IOException
     *             If anything goes wrong
     */
    final SpallocJob createJob(final int nBoards) throws IOException {
        return new SpallocJob(comms.sendRequest(
                new CreateJobCommand(nBoards, owner), Integer.class));
    }

    // ------------------------------ Job ------------------------------

    /**
     * Update the state of a job and notify any listeners.
     *
     * @param job The job to get the state of
     * @throws IOException If there is an error getting the state
     */
    private void updateJobState(final SpallocJob job) throws IOException {
        final JobState state;
        synchronized (machineState) {
            logger.debug("Getting state of " + job.id);
            state = job.getState();
            logger.debug("Job " + job + " is in state " + state.getState());
            machineState.put(job.id, state);
            machineState.notifyAll();
        }

        if (state.getState() == DESTROYED) {
            final SpinnakerMachine machine = machinesAllocated.remove(job.id);
            if (machine == null) {
                logger.error("Unrecognized job: " + job);
                return;
            }
            jobByMachine.remove(machine);
            final MachineNotificationReceiver acallback = callbacks.get(job.id);
            if (acallback != null) {
                acallback.machineUnallocated(machine);
            }
        }
    }

    /**
     * Get the machine of the job.
     *
     * @param job The job
     * @return The machine of the job
     * @throws IOException If an I/O error occurs
     */
    private SpinnakerMachine getMachineForJob(final SpallocJob job)
            throws IOException {
        final JobMachineInfo info = job.getMachineInfo();
        return new SpinnakerMachine(info.getConnections().get(0).getHostname(),
                MACHINE_VERSION, info.getWidth(), info.getHeight(),
                info.getConnections().size(), null);
    }

    /**
     * Wait for a job to reach one of a given set of states.
     *
     * @param job The job
     * @param states The states to wait for
     * @return The state the job has reached
     * @throws IOException If an I/O error occurs
     */
    private JobState waitForStates(final SpallocJob job,
            final Integer... states) throws IOException {
        final Set<Integer> set = new HashSet<>(asList(states));
        synchronized (machineState) {
            final JobState state = job.getState();
            machineState.put(job.id, state);
            while (!machineState.containsKey(job.id)
                    || !set.contains(machineState.get(job.id).getState())) {
                logger.debug("Waiting for job " + job.id + " to get to one of "
                        + Arrays.toString(states));
                if (waitfor(machineState)) {
                    return null;
                }
            }
            return machineState.get(job.id);
        }
    }

    @Override
    public List<SpinnakerMachine> getMachines() {
        try {
            final List<SpinnakerMachine> machines = new ArrayList<>();
            for (final Machine machine : listMachines()) {
                if (machine.getTags().contains(DEFAULT_TAG)) {
                    machines.add(new SpinnakerMachine(machine.getName(),
                            MACHINE_VERSION,
                            machine.getWidth() * MACHINE_WIDTH_FACTOR,
                            machine.getHeight() * MACHINE_HEIGHT_FACTOR,
                            machine.getWidth() * machine.getHeight(), null));
                }
            }
            return machines;
        } catch (final IOException e) {
            logger.error("Error getting machines", e);
            return null;
        }
    }

    @Override
    public SpinnakerMachine getNextAvailableMachine(final int nBoards) {
        SpallocJob job = null;
        SpinnakerMachine machineAllocated = null;

        while ((job == null) || (machineAllocated == null)) {
            try {
                job = createJob(nBoards);

                logger.debug(
                        "Got machine " + job.id + ", requesting notifications");
                job.notify(true);
                JobState state = job.getState();
                synchronized (machineState) {
                    machineState.put(job.id, state);
                    machineState.notifyAll();
                }
                logger.debug("Notifications for " + job.id + " are on");

                state = waitForStates(job, READY, DESTROYED);
                if (state.getState() == DESTROYED) {
                    throw new RuntimeException(state.getReason());
                }

                machineAllocated = getMachineForJob(job);
            } catch (final IOException e) {
                logger.error("Error getting machine - retrying", e);
            }
        }

        machinesAllocated.put(job.id, machineAllocated);
        jobByMachine.put(machineAllocated, job);
        if (callback != null) {
            callbacks.put(job.id, callback);
        }
        return machineAllocated;
    }

    @Override
    public void releaseMachine(final SpinnakerMachine machine) {
        final SpallocJob job = jobByMachine.remove(machine);
        if (job != null) {
            try {
                logger.debug("Turning off notification for " + job.id);
                job.notify(false);
                logger.debug("Notifications for " + job.id + " are off");
                machinesAllocated.remove(job.id);
                synchronized (machineState) {
                    machineState.remove(job.id);
                }
                callbacks.remove(job.id);
                job.destroy();
                logger.debug("Job " + job.id + " destroyed");
            } catch (final IOException e) {
                logger.error("Error releasing machine for " + job.id);
            }
        }
    }

    @Override
    public boolean isMachineAvailable(final SpinnakerMachine machine) {
        final SpallocJob job = jobByMachine.get(machine);
        if (job == null) {
            return false;
        }
        logger.debug("Job " + job.id + " still available");
        return true;
    }

    @Override
    public boolean waitForMachineStateChange(final SpinnakerMachine machine,
            final int waitTime) {
        final SpallocJob job = jobByMachine.get(machine);
        if (job == null) {
            return true;
        }

        synchronized (machineState) {
            final JobState state = machineState.get(job.id);
            try {
                machineState.wait(waitTime);
            } catch (final InterruptedException e) {
                // Does Nothing
            }
            final JobState newState = machineState.get(job.id);
            return (newState != null) && newState.equals(state);
        }
    }

    @Override
    public void setMachinePower(final SpinnakerMachine machine,
            final boolean powerOn) {
        final SpallocJob job = jobByMachine.get(machine);
        if (job == null) {
            return;
        }
        try {
            job.power(powerOn);
            if (powerOn) {
                logger.debug("Waiting for powered on machine");
                waitForStates(job, READY, DESTROYED);
                logger.debug("Machine ready");
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not change power state", e);
        }
    }

    @Override
    public ChipCoordinates getChipCoordinates(final SpinnakerMachine machine,
            final int x, final int y) {
        final SpallocJob job = jobByMachine.get(machine);
        if (job == null) {
            return null;
        }
        try {
            WhereIs whereIs = job.whereIs(x, y);
            int[] location = whereIs.getPhysical();
            return new ChipCoordinates(location[0], location[1], location[2]);
        } catch (IOException e) {
            throw new RuntimeException("Error getting coordinates", e);
        }
    }

    /**
     * Call keep-alive on any active jobs.
     */
    private void keepAllJobsAlive() {
        List<Integer> jobIds;
        synchronized (machineState) {
            jobIds = new ArrayList<>(machineState.keySet());
        }
        for (final int jobId : jobIds) {
            try {
                new SpallocJob(jobId).keepAlive();
            } catch (final IOException e) {
                logger.error("Error keeping machine " + jobId + " alive");
            }
        }
    }

    /**
     * Update the state of any active jobs.
     */
    private void updateStateOfJobs() {
        try {
            while (!done) {
                for (final int jobId : comms.getJobsChanged()) {
                    try {
                        updateJobState(new SpallocJob(jobId));
                    } catch (final IOException e) {
                        logger.error("Error getting job state", e);
                    }
                }
            }
        } catch (final InterruptedException e) {
            logger.warn("interrupt of job state updating");
        }
    }
}
