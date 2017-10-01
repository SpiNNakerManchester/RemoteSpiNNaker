package uk.ac.manchester.cs.spinnaker.jobprocessmanager;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.jboss.resteasy.util.Base64.encodeBytes;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import uk.ac.manchester.cs.spinnaker.job.JobManagerInterface;

public abstract class RemoteSpiNNakerAPI {
    private RemoteSpiNNakerAPI() {
    }

    private static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * How to talk to the main website.
     *
     * @param url
     *            Where the main website is located.
     * @param authToken
     *            How to authenticate to the main website, or <tt>null</tt> to
     *            not provide auth. If given, Should be the concatenation of the
     *            username, a colon (<tt>:</tt>), and the password.
     */
    public static JobManagerInterface createJobManager(final String url,
            final String authToken) {
        final ResteasyClientBuilder builder = new ResteasyClientBuilder();
        // TODO Add https trust store, etc.
        final ResteasyClient client = builder.build();
        JacksonJsonProvider provider = new JacksonJsonProvider();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());
        provider.setMapper(mapper);
        client.register(provider);
        if (authToken != null) {
            client.register(getBasicAuthFilter(authToken));
        }
        return client.target(url).proxy(JobManagerInterface.class);
    }

    private static ClientRequestFilter
            getBasicAuthFilter(final String authToken) {
        final String payload = "Basic " + encodeBytes(authToken.getBytes(UTF8));
        return new ClientRequestFilter() {
            @Override
            public void filter(final ClientRequestContext requestContext)
                    throws IOException {
                requestContext.getHeaders().add(AUTHORIZATION, payload);
            }
        };
    }
}
