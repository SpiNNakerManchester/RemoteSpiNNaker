package uk.ac.manchester.cs.spinnaker;
import static uk.ac.manchester.cs.spinnaker.rest.utils.RestClientUtils.createApiKeyClient;

import java.net.URL;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;
import uk.ac.manchester.cs.spinnaker.job.nmpi.QueueEmpty;
import uk.ac.manchester.cs.spinnaker.job.nmpi.QueueNextResponse;
import uk.ac.manchester.cs.spinnaker.rest.NMPIQueue;
import uk.ac.manchester.cs.spinnaker.rest.utils.CustomJacksonJsonProvider;
import uk.ac.manchester.cs.spinnaker.rest.utils.PropertyBasedDeserialiser;

/**
 * Testing.
 *
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
        final CustomJacksonJsonProvider provider =
                new CustomJacksonJsonProvider();

        /**
         * How to understand messages coming from the queue.
         */
        @SuppressWarnings("serial")
        class QueueResponseDeserialiser
                extends PropertyBasedDeserialiser<QueueNextResponse> {
            /**
             * Make a deserialiser.
             */
            QueueResponseDeserialiser() {
                super(QueueNextResponse.class);
                register("id", Job.class);
                register("warning", QueueEmpty.class);
            }
        }
        provider.addDeserialiser(QueueNextResponse.class,
                new QueueResponseDeserialiser());

        URL nmpiUrl = new URL("https://nmpi.hbpneuromorphic.eu/");
        String nmpiUsername = "uman";
        String apiKey = "QWvPf6WzISelx7MhIJqzoi-BgZqj95PPJYnpBuLTKcGN5b8sbP9"
                + "fiUR2UQ6I--PHuoeOIeF0tmKptKC5rbIMRiRlGGG51zDvRDzqoIVTm4LU6L"
                + "fV8MXYRlzXi4Dc75w-";
        NMPIQueue queue = createApiKeyClient(nmpiUrl, nmpiUsername, apiKey,
                NMPIQueue.class, provider);
        QueueNextResponse response = queue.getNextJob("SpiNNaker");
        if (response instanceof QueueEmpty) {
            System.err.println("No items in queue");
        } else if (response instanceof Job) {
            ObjectMapper om = new ObjectMapper();
            System.err.println(
                    om.writerWithDefaultPrettyPrinter().writeValueAsString(
                            response));
        } else {
            throw new IllegalStateException();
        }
    }

}
