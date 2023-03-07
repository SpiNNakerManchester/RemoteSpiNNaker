/*
 * Copyright (c) 2014 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.cs.spinnaker.nmpi;

import java.io.IOException;

import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;

/**
 * An interface for things that listen for new jobs.
 */
public interface NMPIQueueListener {
    /**
     * Adds a job to the listener.
     *
     * @param job
     *            The job to add.
     * @throws IOException
     *             If anything goes wrong.
     */
    void addJob(Job job) throws IOException;
}
