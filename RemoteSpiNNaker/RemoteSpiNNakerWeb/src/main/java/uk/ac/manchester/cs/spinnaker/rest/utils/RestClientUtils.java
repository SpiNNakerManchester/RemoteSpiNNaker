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
package uk.ac.manchester.cs.spinnaker.rest.utils;

import static org.apache.http.auth.AUTH.WWW_AUTH_RESP;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EncodingUtils;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder.HostnameVerificationPolicy;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestFilter;

/**
 * Manufactures clients for talking to other machines. This class does wicked
 * things, but it is because users keep insisting on getting their security
 * wrong. How difficult is it to just use LetsEncrypt? Apparently very because
 * users instead stand up impossible-to-trust self-signed certificates! The
 * infrastructure required to allow this to work securely requires a lot of
 * interface changes (they'd have to supply the server's public certificate
 * chain as part of the job submission) and that's just awkward.
 */
public abstract class RestClientUtils {

    /**
     * Stop instances being created.
     */
    private RestClientUtils() {
    } // No instances, please, we're British!

    /**
     * The security protocol that is requested for a secure connection.
     */
    public static final String SECURE_PROTOCOL = "TLS";
    /**
     * The maximum total connections to allow.
     */
    private static final int MAX_CONNECTIONS = 2000;
    /**
     * The maximum total connections per route.
     */
    private static final int MAX_CONNECTIONS_PER_ROUTE = 200;

    /**
     * The timeout of socket operation in seconds.
     */
    private static final long TIMEOUT = 60;

    /**
     * Create a client builder.
     *
     * @return A client builder.
     */
    public static ResteasyClientBuilder clientBuilder() {
        return (ResteasyClientBuilder) ClientBuilder.newBuilder();
    }

    /**
     * Manufacture a client.
     *
     * @param url
     *            What this client talks to.
     * @return the client
     */
    protected static ResteasyClient createRestClient(final URL url) {
        try {
            // Create and return a client
            var client = clientBuilder()
                    .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                    .connectionPoolSize(MAX_CONNECTIONS)
                    .maxPooledPerRoute(MAX_CONNECTIONS_PER_ROUTE)
                    .hostnameVerification(HostnameVerificationPolicy.ANY)
                    .sslContext(getSSLContext()).build();
            client.register(new ErrorCaptureResponseFilter());
            return client;
        } catch (NoSuchAlgorithmException | KeyManagementException
                | KeyStoreException e) {
            throw new RuntimeException("Unexpectedly broken security", e);
        }
    }

    /**
     * Create an SSL context with appropriate key store and management.
     *
     * @return The created context
     * @throws NoSuchAlgorithmException
     *             If the key store can't be read
     * @throws KeyStoreException
     *             If the key store can't be read
     * @throws KeyManagementException
     *             If the key store can't be read
     */
    private static SSLContext getSSLContext() throws NoSuchAlgorithmException,
            KeyStoreException, KeyManagementException {
        var builder = new SSLContextBuilder();
        builder.loadTrustMaterial(new TrustSelfSignedStrategy());
        var trustStore = System.getProperty("remotespinnaker.keystore", null);
        if (trustStore != null) {
            var password = System
                    .getProperty("remotespinnaker.keystore.password", "");
            try {
                builder.loadTrustMaterial(new File(trustStore),
                        password.toCharArray());
            } catch (IOException | CertificateException e) {
                throw new RuntimeException(
                        "Unexpected error loading certificates", e);
            }
        }
        return builder.build();
    }

    /**
     * Create a REST client proxy for a class to the given URL.
     *
     * @param <T>
     *            The type of interface to proxy
     * @param url
     *            The URL of the REST service
     * @param authorizationHeader
     *            The authorisation header to provide; if {@code null}, no
     *            header will be provided.
     * @param clazz
     *            The interface to proxy
     * @param providers
     *            The objects to register with the underlying client
     * @return The proxy instance
     */
    private static <T> T createClient(final URL url,
            final String authorizationHeader, final Class<T> clazz,
            final Object... providers) {
        var client = createRestClient(url);
        for (var provider : providers) {
            client.register(provider);
        }
        if (providers.length == 0) {
            client.register(new JacksonJsonProvider());
        }
        var target = client.target(url.toString());
        if (authorizationHeader != null) {
            return target.register((ClientRequestFilter) context -> {
                context.getHeaders().add(WWW_AUTH_RESP, authorizationHeader);
            }).proxy(clazz);
        } else {
            return target.proxy(clazz);
        }
    }

    /**
     * Create a new REST client with BASIC authentication.
     *
     * @param <T>
     *            The type of interface to proxy
     * @param url
     *            The URL of the REST service
     * @param username
     *            The user name of the user accessing the service
     * @param password
     *            The password for authentication
     * @param clazz
     *            The interface to proxy
     * @param providers
     *            The objects to register with the underlying client
     * @return The proxy instance
     */
    public static <T> T createBasicClient(final URL url, final String username,
            final String password, final Class<T> clazz,
            final Object... providers) {
        var userPass = Base64.encodeBase64String(
                EncodingUtils.getAsciiBytes(username + ":" + password));
        var authHeader = "Basic " + userPass;
        return createClient(url, authHeader, clazz, providers);
    }

    /**
     * Create a new REST client with APIKey authentication.
     *
     * @param <T>
     *            The type of interface to proxy
     * @param url
     *            The URL of the REST service
     * @param username
     *            The user name of the user accessing the service
     * @param apiKey
     *            The key to use to authenticate
     * @param clazz
     *            The interface to proxy
     * @param providers
     *            The objects to register with the underlying client
     * @return The proxy instance
     */
    public static <T> T createApiKeyClient(final URL url, final String username,
            final String apiKey, final Class<T> clazz,
            final Object... providers) {
        return createClient(url, "ApiKey " + username + ":" + apiKey, clazz,
                providers);
    }

    /**
     * Create a new REST client with Bearer authentication.
     *
     * @param <T>
     *            The type of interface to proxy
     * @param url
     *            The URL of the REST service
     * @param token
     *            The Bearer token for authentication
     * @param clazz
     *            The interface to proxy
     * @param providers
     *            The objects to register with the underlying client
     * @return The proxy instance
     */
    public static <T> T createBearerClient(final URL url, final String token,
            final Class<T> clazz, final Object... providers) {
        return createClient(url, "Bearer " + token, clazz, providers);
    }
}
