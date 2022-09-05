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
