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
package uk.ac.manchester.cs.spinnaker;

import static uk.ac.manchester.cs.spinnaker.rest.utils.RestClientUtils.createApiKeyClient;

import java.net.URL;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;
import uk.ac.manchester.cs.spinnaker.job.nmpi.QueueEmpty;
import uk.ac.manchester.cs.spinnaker.rest.NMPIQueue;

/**
 * Testing.
 */
public final class TestRestClient {

    /**
     * Does Nothing.
     */
    private TestRestClient() {
        // Does Nothing
    }

    /**
     * Main method.
     * @param args Args
     * @throws Exception if anything goes wrong
     */
    public static void main(final String[] args) throws Exception {
        var nmpiUrl = new URL("https://nmpi.hbpneuromorphic.eu/");
        var nmpiUsername = "uman";
        var apiKey = "QWvPf6WzISelx7MhIJqzoi-BgZqj95PPJYnpBuLTKcGN5b8sbP9"
                + "fiUR2UQ6I--PHuoeOIeF0tmKptKC5rbIMRiRlGGG51zDvRDzqoIVTm4LU6L"
                + "fV8MXYRlzXi4Dc75w-";
        var queue = createApiKeyClient(nmpiUrl, nmpiUsername, apiKey,
                NMPIQueue.class, NMPIQueue.createProvider());
        var response = queue.getNextJob("SpiNNaker");
        if (response instanceof QueueEmpty) {
            System.err.println("No items in queue");
        } else if (response instanceof Job) {
            var om = new ObjectMapper();
            System.err.println(
                    om.writerWithDefaultPrettyPrinter().writeValueAsString(
                            response));
        } else {
            throw new IllegalStateException();
        }
    }

}
