package uk.ac.manchester.cs.spinnaker.rest.utils;

import static org.apache.http.auth.AUTH.PROXY_AUTH_RESP;
import static org.apache.http.auth.AUTH.WWW_AUTH_RESP;
import static org.apache.http.auth.params.AuthParams.getCredentialCharset;
import static org.apache.http.client.protocol.ClientContext.AUTH_CACHE;
import static org.apache.http.client.protocol.ClientContext.CREDS_PROVIDER;
import static org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.RFC2617Scheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.slf4j.Logger;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * Manufactures clients for talking to other machines. This class does wicked
 * things, but it is because users keep insisting on getting their security
 * wrong. How difficult is it to just use LetsEncrypt? Apparently very because
 * users instead stand up impossible-to-trust self-signed certificates! The
 * infrastructure required to allow this to work securely requires a lot of
 * interface changes (they'd have to supply the server's public certificate
 * chain as part of the job submission) and that's just awkward.
 *
 * @author Donal Fellows
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
     * Default port for HTTPS.
     */
    private static final int HTTPS_PORT = 443;
    /**
     * The maximum total connections to allow.
     */
    private static final int MAX_CONNECTIONS = 2000;
    /**
     * The maximum total connections per route.
     */
    private static final int MAX_CONNECTIONS_PER_ROUTE = 200;

    /**
     * Logging.
     */
    private static Logger log = getLogger(RestClientUtils.class);

    /**
     * Manufacture a client.
     *
     * @param url
     *            What this client talks to.
     * @param credentials
     *            What this client will authenticate with.
     * @param authScheme
     *            The authentication scheme.
     * @return the client
     */
    protected static ResteasyClient createRestClient(final URL url,
            final Credentials credentials, final AuthScheme authScheme) {
        try {
            final SchemeRegistry schemeRegistry = getSchemeRegistry();
            final HttpContext localContext = getConnectionContext(url,
                    credentials, authScheme);

            // Set up the connection
            final PoolingClientConnectionManager cm =
                    new PoolingClientConnectionManager(schemeRegistry);
            cm.setMaxTotal(MAX_CONNECTIONS);
            cm.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);
            final DefaultHttpClient httpClient = new DefaultHttpClient(cm);
            final ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(
                    httpClient, localContext);

            // Create and return a client
            final ResteasyClient client = new ResteasyClientBuilder()
                    .httpEngine(engine).build();
            client.register(new ErrorCaptureResponseFilter());
            return client;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.error("Cannot find basic SSL algorithms - "
                    + "this suggests a broken Java installation...");
            throw new RuntimeException("unexpectedly broken security", e);
        }
    }

    /**
     * Set up a connection context.
     *
     * @param url
     *            Where will the connection be made to?
     * @param credentials
     *            What credentials will be used to connect?
     * @param authScheme
     *            The authentication scheme to use.
     * @return the configured context.
     */
    private static HttpContext getConnectionContext(final URL url,
            final Credentials credentials, final AuthScheme authScheme) {
        int port = url.getPort();
        if (port == -1) {
            port = url.getDefaultPort();
        }
        HttpHost targetHost = new HttpHost(url.getHost(), port,
                url.getProtocol());

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(targetHost.getHostName(), targetHost.getPort()),
                credentials);

        AuthCache authCache = new BasicAuthCache();
        authCache.put(targetHost, authScheme);

        HttpContext localContext = new BasicHttpContext();
        localContext.setAttribute(AUTH_CACHE, authCache);
        localContext.setAttribute(CREDS_PROVIDER, credsProvider);
        return localContext;
    }

    /**
     * Hey, we trust everything!
     *
     * @param certs A certificate chain.
     * @return Whether the certificate is trusted. It is! Always!
     */
    private static boolean checkTrusted(final X509Certificate[] certs) {
        return true;
    }

    /**
     * What issuers do we trust? None really, but we claim we do.
     *
     * @return The issuer certificate to trust. Or <tt>null</tt>.
     */
    private static X509Certificate getTrustedCert() {
        return null;
    }

    /**
     * A trust manager that believes everything it is told. This is how not to
     * write a trust manager.
     * @return the trusty trust manager
     * @see #getSchemeRegistry()
     */
    private static TrustManager getGullableTrustManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(final X509Certificate[] certs,
                    final String authType) throws CertificateException {
                // Does Nothing; we aren't deploying client-side certs
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] certs,
                    final String authType) throws CertificateException {
                if (!checkTrusted(certs)) {
                    throw new CertificateException("untrusted server");
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                X509Certificate cert = getTrustedCert();
                if (cert == null) {
                    return null;
                }
                return new X509Certificate[] {
                        cert };
            }
        };
    }

    /**
     * Set up HTTPS to ignore certificate errors. This method is doing wicked
     * things as it is using the gullable trust manager.
     * <p>
     * <i>Be aware that this method is doing very bad things; a trust-all trust
     * manager is <b>entirely</b> doing it wrong, but the reality of academic
     * security is that it is the only sane option.</i>
     *
     * @return The scheme registry
     * @throws NoSuchAlgorithmException If TLS is unsupported
     * @throws KeyManagementException If something goes wrong
     */
    private static SchemeRegistry getSchemeRegistry()
            throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance(SECURE_PROTOCOL);
        sslContext.init(null, new TrustManager[] {
                getGullableTrustManager() }, new SecureRandom());
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", HTTPS_PORT,
                new SSLSocketFactory(sslContext, ALLOW_ALL_HOSTNAME_VERIFIER)));
        return schemeRegistry;
    }

    /**
     * Create a REST client proxy for a class to the given URL.
     *
     * @param <T>
     *            The type of interface to proxy
     * @param url
     *            The URL of the REST service
     * @param credentials
     *            The credentials used to access the service
     * @param authScheme
     *            The authentication scheme in use
     * @param clazz
     *            The interface to proxy
     * @param providers
     *            The objects to register with the underlying client
     * @return The proxy instance
     */
    public static <T> T createClient(final URL url,
            final Credentials credentials, final AuthScheme authScheme,
            final Class<T> clazz, final Object... providers) {
        final ResteasyClient client = createRestClient(url, credentials,
                authScheme);
        for (final Object provider : providers) {
            client.register(provider);
        }
        if (providers.length == 0) {
            client.register(new JacksonJsonProvider());
        }
        return client.target(url.toString()).proxy(clazz);
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
        return createClient(url,
                new UsernamePasswordCredentials(username, password),
                new BasicScheme(), clazz, providers);
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
        return createClient(url,
                new UsernamePasswordCredentials(username, apiKey),
                new ConnectionIndependentScheme("ApiKey") {
                    @Override
                    protected Header authenticate(
                            final Credentials credentials) {
                        return new BasicHeader(getAuthHeaderName(),
                                "ApiKey " + username + ":" + apiKey);
                    }
                }, clazz, providers);
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
        return createClient(url, new UsernamePasswordCredentials("", token),
                new ConnectionIndependentScheme("Bearer") {
                    @Override
                    protected Header authenticate(
                            final Credentials credentials) {
                        return new BasicHeader(getAuthHeaderName(),
                                "Bearer " + token);
                    }
                }, clazz, providers);
    }

    /**
     * Base for authorisation schemes.
     */
    private abstract static class ConnectionIndependentScheme
            extends RFC2617Scheme {

        /**
         * True when complete.
         */
        private final boolean complete = false;

        /**
         * The name of the scheme.
         */
        private final String name;

        /**
         * Create a new scheme.
         * @param nameParam The name of the scheme
         */
        ConnectionIndependentScheme(final String nameParam) {
            this.name = nameParam;
        }

        @Override
        public String getSchemeName() {
            return name;
        }

        @Override
        public boolean isConnectionBased() {
            return false;
        }

        @Override
        public boolean isComplete() {
            return complete;
        }

        /**
         * Produce an authorisation header for the given set of
         * {@link Credentials}. The credentials and the connection will have
         * been sanity-checked prior to this call.
         *
         * @param credentials
         *            The credentials to be authenticated.
         * @return the header
         */
        protected abstract Header authenticate(Credentials credentials);

        /**
         * Give the header that we're supposed to generate, depending on whether
         * we're going by a proxy or not.
         *
         * @return the authentication header name
         */
        protected String getAuthHeaderName() {
            if (isProxy()) {
                return PROXY_AUTH_RESP;
            } else {
                return WWW_AUTH_RESP;
            }
        }

        @Override
        public Header authenticate(final Credentials credentials,
                final HttpRequest request) throws AuthenticationException {
            if (credentials == null) {
                throw new IllegalArgumentException(
                        "Credentials may not be null");
            }
            if (request == null) {
                throw new IllegalArgumentException(
                        "HTTP request may not be null");
            }
            final String charset = getCredentialCharset(request.getParams());
            if (charset == null) {
                throw new IllegalArgumentException("charset may not be null");
            }

            return authenticate(credentials);
        }
    }
}
