package uk.ac.manchester.cs.spinnaker.jobmanager;

import static com.xensource.xenapi.Session.loginWithPassword;
import static com.xensource.xenapi.Session.logout;
import static com.xensource.xenapi.Types.VbdMode.RW;
import static com.xensource.xenapi.Types.VbdType.DISK;
import static com.xensource.xenapi.Types.VdiType.USER;
import static com.xensource.xenapi.Types.VmPowerState.HALTED;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.cs.spinnaker.job.JobManagerInterface.JOB_PROCESS_MANAGER_ZIP;
import static uk.ac.manchester.cs.spinnaker.jobmanager.JobManager.JOB_PROCESS_MANAGER_JAR;
import static uk.ac.manchester.cs.spinnaker.utils.ThreadUtils.sleep;

import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.UUID;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Types.VmPowerState;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VM;

/**
 * Executes jobs on a Xen Virtual Machine.
 */
public class XenVMExecuterFactory implements JobExecuterFactory {

    /** Bytes in a gigabyte. Well, a gibibyte, but that's a nasty word. */
    private static final long GB = 1024L * 1024L * 1024L;
    /** Time (in ms) between checks to see if a VM is running. */
    private static final int VM_POLL_INTERVAL = 10000;

    /**
     * Lock object used for synchronisation.
     */
    private final Object lock = new Object();

    /**
     * The thread group of any threads.
     */
    private final ThreadGroup threadGroup;

    /**
     * Logging.
     */
    private final Logger logger = getLogger(getClass());

    /**
     * The URL of the Xen Server.
     */
    @Value("${xen.server.url}")
    private URL xenServerUrl;

    /**
     * The username under which to control Xen.
     */
    @Value("${xen.server.username}")
    private String username;

    /**
     * The password for the username.
     */
    @Value("${xen.server.password}")
    private String password;

    /**
     * The name of the Xen Template VM.
     */
    @Value("${xen.server.templateVm}")
    private String templateLabel;

    /**
     * True if the VMs should be deleted on shutdown of the VM.
     */
    @Value("${deleteJobsOnExit}")
    private boolean deleteOnExit;

    /**
     * True if the Xen VM should shutdown on exit of the job.
     */
    @Value("${xen.server.shutdownOnExit}")
    private boolean shutdownOnExit;

    /**
     * True if the log of the job should upload as it is output.
     */
    @Value("${liveUploadOutput}")
    private boolean liveUploadOutput;

    /**
     * True if a spinnaker machine should be requested.
     */
    @Value("${requestSpiNNakerMachine}")
    private boolean requestSpiNNakerMachine;

    /**
     * The default size of the disk to attach to the Xen VM.
     */
    @Value("${xen.server.diskspaceInGbs}")
    private long defaultDiskSizeInGbs;

    /**
     * The maximum number of VMs to create.
     */
    @Value("${xen.server.maxVms}")
    private int maxNVirtualMachines;

    /**
     * The current number of VMs.
     */
    private int nVirtualMachines = 0;

    /**
     * Create a new Xen Executor Factory.
     */
    public XenVMExecuterFactory() {
        this.threadGroup = new ThreadGroup("XenVM");
    }

    /**
     * Create a job executor for a job.
     */
    @Override
    public JobExecuter createJobExecuter(final JobManager manager,
            final URL baseUrl) throws IOException {
        requireNonNull(manager);
        requireNonNull(baseUrl);
        waitToClaimVM();

        try {
            return new Executer(manager, baseUrl);
        } catch (final Exception e) {
            throw new IOException("Error creating VM", e);
        }
    }

    /**
     * Wait for the VM to come up.
     */
    private void waitToClaimVM() {
        synchronized (lock) {
            logger.debug(nVirtualMachines + " of " + maxNVirtualMachines
                    + " in use");
            while (nVirtualMachines >= maxNVirtualMachines) {
                logger.debug("Waiting for a VM to become available ("
                        + nVirtualMachines + " of " + maxNVirtualMachines
                        + " in use)");
                try {
                    lock.wait();
                } catch (final InterruptedException e) {
                    // Does Nothing
                }
            }
            nVirtualMachines++;
        }
    }

    /**
     * Indicate that a VM has exited.
     */
    protected void executorFinished() {
        synchronized (lock) {
            nVirtualMachines--;
            logger.debug(nVirtualMachines + " of " + maxNVirtualMachines
                    + " now in use");
            lock.notifyAll();
        }
    }

    /** Taming the Xen API a bit. */
    class XenConnection implements AutoCloseable {

        /**
         * The Xen connection.
         */
        private Connection conn;

        /**
         * The ID of the executor on the VM.
         */
        private final String id;

        /**
         * Create a connection to Xen.
         *
         * @param idParam The ID of the executor.
         * @throws XenAPIException If there is an error logging in
         * @throws XmlRpcException If there is an error speaking to Xen
         */
        XenConnection(final String idParam)
                throws XenAPIException, XmlRpcException {
            this.id = idParam;
            conn = new Connection(xenServerUrl);
            loginWithPassword(conn, username, password);
        }

        /**
         * Get a VM from Xen.
         * @return The created VM.
         * @throws XmlRpcException If there is an error speaking to Xen
         * @throws IOException If there is a general error speaking to Xen
         */
        VM getVirtualMachine() throws XmlRpcException, IOException {
            final Set<VM> vmsWithLabel = VM.getByNameLabel(conn, templateLabel);
            if (vmsWithLabel.isEmpty()) {
                throw new IOException("No template with name " + templateLabel
                        + " was found");
            }
            final VM template = vmsWithLabel.iterator().next();
            return template.createClone(conn, templateLabel + "_" + id);
        }

        /**
         * Get the virtual block device of a VM.
         * @param vm The VM.
         * @return The block device.
         * @throws XmlRpcException If there is an error speaking to Xen
         * @throws IOException If there is a general error speaking to Xen
         */
        VBD getVirtualBlockDevice(final VM vm)
                throws XmlRpcException, IOException {
            final Set<VBD> disks = vm.getVBDs(conn);
            if (disks.isEmpty()) {
                throw new IOException("No disks found on " + templateLabel);
            }
            return disks.iterator().next();
        }

        /**
         * Get the label to give to a disk based on the base label of another.
         *
         * @param vdi The VDI
         * @param suffix The suffix to add to the label
         * @return The label
         * @throws XmlRpcException If there is an error speaking to Xen
         * @throws XenAPIException If there is an error getting the label
         */
        String getLabel(final VDI vdi, final String suffix)
                throws XmlRpcException, XenAPIException {
            return vdi.getNameLabel(conn) + "_" + id + "_" + suffix;
        }

        /**
         * Get the Base Virtual Disk Image from a disk.
         * @param disk The Virtual Block Device to get the VDI from.
         * @return The VDI.
         * @throws XmlRpcException If there is an error speaking to Xen
         * @throws XenAPIException If there is an error getting the label
         */
        VDI getBaseVDI(final VBD disk) throws XmlRpcException, XenAPIException {
            final VDI vdi = disk.getVDI(conn);
            vdi.setNameLabel(conn, getLabel(vdi, "base"));
            return vdi;
        }

        /**
         * Create a new Virtual Disk Image for a VM.
         * @param baseVDI The base Virtual Disk Image to copy the setting from
         * @return The VDI
         * @throws XmlRpcException If there is an error speaking to Xen
         * @throws XenAPIException If there is an error getting the label
         */
        VDI createVDI(final VDI baseVDI)
                throws XenAPIException, XmlRpcException {
            final VDI.Record descriptor = new VDI.Record();
            descriptor.nameLabel = getLabel(baseVDI, "storage");
            descriptor.type = USER;
            descriptor.SR = baseVDI.getSR(conn);
            descriptor.virtualSize = defaultDiskSizeInGbs * GB;

            return VDI.create(conn, descriptor);
        }

        /**
         * Create a Virtual Block Device for a VM.
         *
         * @param vm The VM to create the device for
         * @param vdi The Disk Image to put in the VBD
         * @return The VBD created
         * @throws XmlRpcException If there is an error speaking to Xen
         * @throws XenAPIException If there is an error in the API call
         */
        VBD createVBD(final VM vm, final VDI vdi)
                throws XenAPIException, XmlRpcException {
            final VBD.Record descriptor = new VBD.Record();
            descriptor.VM = vm;
            descriptor.VDI = vdi;
            descriptor.userdevice = "1";
            descriptor.mode = RW;
            descriptor.type = DISK;

            return VBD.create(conn, descriptor);
        }

        /**
         * Add VM data that can be read by the VM (used for config parameters).
         *
         * @param vm The VM to add the data to.
         * @param key The key of the data.
         * @param value The value of the data
         * @throws XmlRpcException If there is an error speaking to Xen
         * @throws XenAPIException If there is an error in the API call
         */
        void addData(final VM vm, final String key, final Object value)
                throws XenAPIException, XmlRpcException {
            vm.addToXenstoreData(conn, key, value.toString());
        }

        /**
         * Start a VM.
         *
         * @param vm The VM to start.
         * @throws XmlRpcException If there is an error speaking to Xen
         * @throws XenAPIException If there is an error in the API call
         */
        void start(final VM vm) throws XenAPIException, XmlRpcException {
            vm.start(conn, false, true);
        }

        /**
         * Delete a VM from existence.
         *
         * @param vm The VM to delete.
         * @throws XmlRpcException If there is an error speaking to Xen
         * @throws XenAPIException If there is an error in the API call
         */
        void destroy(final VM vm) throws XenAPIException, XmlRpcException {
            vm.destroy(conn);
        }

        /**
         * Delete a virtual block device from existence.
         *
         * @param vm The device to delete.
         * @throws XmlRpcException If there is an error speaking to Xen
         * @throws XenAPIException If there is an error in the API call
         */
        void destroy(final VBD vm) throws XenAPIException, XmlRpcException {
            vm.destroy(conn);
        }

        /**
         * Delete a virtual disk image.
         *
         * @param vm The disk to delete
         * @throws XmlRpcException If there is an error speaking to Xen
         * @throws XenAPIException If there is an error in the API call
         */
        void destroy(final VDI vm) throws XenAPIException, XmlRpcException {
            vm.destroy(conn);
        }

        /**
         * Get the current power state of a VM (i.e. on or off).
         *
         * @param vm The VM to query
         * @return The state of the VM.
         * @throws XmlRpcException If there is an error speaking to Xen
         * @throws XenAPIException If there is an error in the API call
         */
        VmPowerState getState(final VM vm)
                throws XenAPIException, XmlRpcException {
            return vm.getPowerState(conn);
        }

        @Override
        public void close() {
            try {
                logout(conn);
            } catch (XenAPIException | XmlRpcException e) {
                logger.error("problem when closing connection; "
                        + "resource potentially leaked", e);
            } finally {
                conn = null;
            }
        }
    }

    /**
     * The executor for Xen.
     */
    class Executer implements JobExecuter, Runnable {
        // Parameters from constructor
        /**
         * The Job Manager to report to.
         */
        private final JobManager jobManager;

        /**
         * The UUID to assign to this executor.
         */
        private final String uuid;

        /**
         * The URL to give to the job process manager for it to speak to the
         * job manager.
         */
        private final URL jobProcessManagerUrl;

        /**
         * The arguments for the job process manager.
         */
        private final String args;

        // Internal entities
        /**
         * The VM of the job.
         */
        private VM clonedVm;

        /**
         * The disk of the cloned VM.
         */
        private VBD disk;

        /**
         * The Virtual Disk Image of the cloned VM.
         */
        private VDI vdi;

        /**
         * The Virtual Disk Image added to the VM.
         */
        private VDI extraVdi;

        /**
         * The disk added to the VM.
         */
        private VBD extraDisk;

        /**
         * Create the executer.
         *
         * @param jobManagerParam The job manager to report to.
         * @param baseUrl The base URL of the REST service
         * @throws XmlRpcException If there is an error speaking to Xen
         * @throws IOException If there is general IO error
         */
        Executer(final JobManager jobManagerParam, final URL baseUrl)
                throws XmlRpcException, IOException {
            this.jobManager = jobManagerParam;
            uuid = UUID.randomUUID().toString();
            jobProcessManagerUrl =
                    new URL(baseUrl, "job/" + JOB_PROCESS_MANAGER_ZIP);

            final StringBuilder execArgs = new StringBuilder("-jar ");
            execArgs.append(JOB_PROCESS_MANAGER_JAR);
            execArgs.append(" --serverUrl ");
            execArgs.append(baseUrl);
            execArgs.append(" --executerId ");
            execArgs.append(uuid);
            if (deleteOnExit) {
                execArgs.append(" --deleteOnExit");
            }
            if (liveUploadOutput) {
                execArgs.append(" --liveUploadOutput");
            }
            if (requestSpiNNakerMachine) {
                execArgs.append(" --requestMachine");
            }
            this.args = execArgs.toString();
        }

        @Override
        public String getExecuterId() {
            return uuid;
        }

        @Override
        public void startExecuter() {
            new Thread(threadGroup, this, "Executer (" + uuid + ")").start();
        }

        /**
         * Create a VM.
         *
         * @return The connection to Xen used to create the VM
         * @throws XmlRpcException If there is an error speaking to Xen
         * @throws IOException If there is a general IO error
         */
        synchronized XenConnection createVm()
                throws XmlRpcException, IOException {
            final XenConnection conn = new XenConnection(uuid);
            clonedVm = conn.getVirtualMachine();
            disk = conn.getVirtualBlockDevice(clonedVm);
            vdi = conn.getBaseVDI(disk);
            extraVdi = conn.createVDI(vdi);
            extraDisk = conn.createVBD(clonedVm, extraVdi);
            conn.addData(clonedVm, "vm-data/nmpiurl", jobProcessManagerUrl);
            conn.addData(clonedVm, "vm-data/nmpifile", JOB_PROCESS_MANAGER_ZIP);
            conn.addData(clonedVm, "vm-data/nmpiargs", args);
            if (shutdownOnExit) {
                conn.addData(clonedVm, "vm-data/shutdown", true);
            }
            conn.start(clonedVm);
            return conn;
        }

        /**
         * Delete a VM for a job.
         *
         * @param conn The connection to the Xen server to use.
         * @throws XenAPIException If there is a Xen API issue
         * @throws XmlRpcException If there is an error speaking to Xen
         */
        private synchronized void deleteVm(final XenConnection conn)
                throws XenAPIException, XmlRpcException {
            if (conn == null) {
                return;
            }
            if (disk != null) {
                conn.destroy(disk);
            }
            if (extraDisk != null) {
                conn.destroy(extraDisk);
            }
            if (vdi != null) {
                conn.destroy(vdi);
            }
            if (extraVdi != null) {
                conn.destroy(extraVdi);
            }
            if (clonedVm != null) {
                conn.destroy(clonedVm);
            }
        }

        @Override
        public void run() {
            try (XenConnection conn = createVm()) {
                try {
                    VmPowerState powerState;
                    do {
                        sleep(VM_POLL_INTERVAL);
                        powerState = conn.getState(clonedVm);
                        logger.debug("VM for " + uuid + " is in state "
                                + powerState);
                    } while (powerState != HALTED);
                } catch (final Exception e) {
                    logger.error("Could not get VM power state, assuming off",
                            e);
                } finally {
                    jobManager.setExecutorExited(uuid, null);
                }

                try {
                    if (deleteOnExit) {
                        deleteVm(conn);
                    }
                } catch (final Exception e) {
                    logger.error("Error deleting VM");
                }
            } catch (final Exception e) {
                logger.error("Error setting up VM", e);
                jobManager.setExecutorExited(uuid, e.getMessage());
            } finally {
                executorFinished();
            }
        }
    }
}
