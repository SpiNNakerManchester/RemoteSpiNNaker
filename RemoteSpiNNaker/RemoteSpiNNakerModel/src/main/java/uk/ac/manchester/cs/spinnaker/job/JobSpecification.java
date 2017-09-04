package uk.ac.manchester.cs.spinnaker.job;

import uk.ac.manchester.cs.spinnaker.machine.SpinnakerMachine;

public class JobSpecification {
    private SpinnakerMachine machine;
    private JobParameters parameters;
    private int id;
    private String url;

    public JobSpecification() {
        // Does Nothing
    }

    public JobSpecification(final SpinnakerMachine machine,
            final JobParameters parameters, final int id, final String url) {
        this.machine = machine;
        this.parameters = parameters;
        this.id = id;
        this.url = url;
    }

    public SpinnakerMachine getMachine() {
        return machine;
    }

    public void setMachine(final SpinnakerMachine machine) {
        this.machine = machine;
    }

    public JobParameters getParameters() {
        return parameters;
    }

    public void setParameters(final JobParameters parameters) {
        this.parameters = parameters;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
