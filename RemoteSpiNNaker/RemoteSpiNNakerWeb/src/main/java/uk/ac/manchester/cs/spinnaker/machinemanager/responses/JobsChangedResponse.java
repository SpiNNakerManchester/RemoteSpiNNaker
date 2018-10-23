package uk.ac.manchester.cs.spinnaker.machinemanager.responses;

import static java.util.Collections.emptyList;

import java.util.List;

/**
 * A response that describes what jobs have changed state.
 */
public class JobsChangedResponse implements Response {

    /**
     * The list of jobs that have changed.
     */
    private List<Integer> jobsChanged = emptyList();

    /**
     * Get the jobs that have changed.
     *
     * @return The list of job ids
     */
    public List<Integer> getJobsChanged() {
        return jobsChanged;
    }

    /**
     * Set the jobs that have changed.
     *
     * @param jobsChangedParam The list of job ids
     */
    public void setJobsChanged(final List<Integer> jobsChangedParam) {
        this.jobsChanged = jobsChangedParam;
    }
}
