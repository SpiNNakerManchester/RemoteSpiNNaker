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
package uk.ac.manchester.cs.spinnaker.jobprocessmanager;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.jboss.resteasy.util.Base64.encodeBytes;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import uk.ac.manchester.cs.spinnaker.job.JobManagerInterface;

/**
 * Manufactures communication proxies for the main service web site.
 */
public abstract class RemoteSpiNNakerAPI {

    /**
     * Makes this impossible to instantiate.
     */
    private RemoteSpiNNakerAPI() {
    }

    /**
     * The character set for the authorisation payload.
     */
    private static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * The timeout of socket operation in seconds.
     */
    private static final long TIMEOUT = 60;

    /**
     * How to talk to the main web site.
     *
     * @param url
     *            Where the main web site is located.
     * @param authToken
     *            How to authenticate to the main web site, or {@code null} to
     *            not provide authorisation. If given, Should be the
     *            concatenation of the username, a colon ({@code :}), and the
     *            password.
     * @return the proxy for the job manager service
     */
    public static JobManagerInterface createJobManager(final String url,
            final String authToken) {
        final ResteasyClientBuilder builder = new ResteasyClientBuilder()
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT, TimeUnit.SECONDS);
        // TODO Add HTTPS trust store, etc.
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

    /**
     * Creates a filter for BASIC authorisation.
     *
     * @param authToken The token for authorisation
     *     (encoded BASE64 username{@code :}password)
     * @return The filter
     */
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
