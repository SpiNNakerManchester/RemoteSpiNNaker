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
