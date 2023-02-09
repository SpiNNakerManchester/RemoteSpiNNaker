/*
 * Copyright (c) 2014-2019 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.cs.spinnaker.job.nmpi;

/**
 * A message indicating that the queue is empty.
 */
public class QueueEmpty implements QueueNextResponse {

    /**
     * Any warning returned.
     */
    private String warning;

    /**
     * Get the warning.
     *
     * @return The warning
     */
    public String getWarning() {
        return warning;
    }

    /**
     * Set the warning.
     *
     * @param warningParam The warning to set
     */
    public void setWarning(final String warningParam) {
        this.warning = warningParam;
    }
}
