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
package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to turn on the boards associated with a job.
 */
public class PowerOnJobBoardsCommand extends Command<Integer> {
    /**
     * Create a request to turn on a job's allocated boards.
     *
     * @param jobId
     *            The job to request about.
     */
    public PowerOnJobBoardsCommand(final int jobId) {
        super("power_on_job_boards");
        addArg(jobId);
    }
}
