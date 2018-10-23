package uk.ac.manchester.cs.spinnaker.remote.web;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.pac4j.core.client.Client;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.springframework.security.authentication.ClientAuthenticationToken;
import org.pac4j.springframework.security.exception.AuthenticationCredentialsException;
import org.slf4j.Logger;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authentication filter that supports HBP authentication mechanisms.
 */
public class DirectClientAuthenticationFilter extends OncePerRequestFilter {
    /**
     * The header that indicates authentication is required.
     */
    private static final String MUST_AUTH_HEADER = "WWW-Authenticate";

    /**
     * The payload of the header that indicates Bearer authentication is
     * required.
     */
    private static final String MUST_AUTH_PAYLOAD = "Bearer realm=\"%s\"";

    /**
     * The default authentication realm. "<tt>SpiNNaker</tt>"
     */
    public static final String DEFAULT_REALM = "SpiNNaker";

    /**
     * Logging.
     */
    private final Logger logger = getLogger(getClass());

    /**
     * The client used to perform authentication.
     */
    private Client<?, ?> client;

    /**
     * The name of the realm authenticated to.
     */
    private final String realmName = DEFAULT_REALM;

    /**
     * The entry point of authentication to return to.
     */
    private AuthenticationEntryPoint authenticationEntryPoint;

    /**
     * The source of the details.
     */
    private final WebAuthenticationDetailsSource detailsSource;

    /**
     * The authentication manager.
     */
    private final AuthenticationManager authenticationManager;

    /**
     * Make an instance of the filter.
     *
     * @param authenticationManagerParam
     *            The authentication manager that takes the decisions.
     */
    public DirectClientAuthenticationFilter(
            final AuthenticationManager authenticationManagerParam) {
        this.authenticationManager = requireNonNull(authenticationManagerParam);
        detailsSource = new WebAuthenticationDetailsSource();
    }

    /**
     * Ensure that this bean will behave sanely in service.
     */
    @PostConstruct
    private void checkForSanity() {
        requireNonNull(client);
        if (authenticationEntryPoint == null) {
            authenticationEntryPoint = new AuthenticationEntryPoint() {
                @Override
                public void commence(final HttpServletRequest request,
                        final HttpServletResponse response,
                        final AuthenticationException authException)
                        throws IOException {
                    commenceBearerAuth(response, authException);
                }
            };
        }
    }

    /**
     * Start the authentication process.
     * @param response The response to fill in
     * @param authException The exception to get the error message from.
     * @throws IOException If there is a problem
     */
    private void commenceBearerAuth(final HttpServletResponse response,
            final AuthenticationException authException) throws IOException {
        response.addHeader(MUST_AUTH_HEADER,
                format(MUST_AUTH_PAYLOAD, realmName));
        response.sendError(SC_UNAUTHORIZED, authException.getMessage());
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
            final HttpServletResponse response, final FilterChain filterChain)
            throws ServletException, IOException {
        // context
        final WebContext context = new J2EContext(request, response);

        // get credentials
        Credentials credentials = null;
        try {
            credentials = client.getCredentials(context);
        } catch (final RequiresHttpAction e) {
            logger.info("Requires additionnal HTTP action", e);
        } catch (final CredentialsException ce) {
            throw new AuthenticationCredentialsException(
                    "Error retrieving credentials", ce);
        }

        logger.debug("credentials : {}", credentials);

        // if credentials/profile is not null, do more
        if (credentials != null) {
            authenticateCredentials(request, response, credentials);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Authenticate the given credentials.
     * @param request The request to authenticate.
     * @param response The response to pass back.
     * @param credentials The credentials being authenticated with.
     * @throws IOException If there is an issue
     * @throws ServletException If there is an issue
     */
    private void authenticateCredentials(final HttpServletRequest request,
            final HttpServletResponse response, final Credentials credentials)
            throws IOException, ServletException {
        // create token from credential
        final ClientAuthenticationToken token =
                new ClientAuthenticationToken(credentials, client.getName());
        token.setDetails(detailsSource.buildDetails(request));

        try {
            // authenticate
            final Authentication auth =
                    authenticationManager.authenticate(token);
            logger.debug("authentication: {}", auth);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (final AuthenticationException e) {
            authenticationEntryPoint.commence(request, response, e);
        }
    }

    /**
     * Get the client used.
     * @return The client
     */
    public Client<?, ?> getClient() {
        return client;
    }

    /**
     * Set the client.
     * @param clientParam The client to set
     */
    public void setClient(final Client<?, ?> clientParam) {
        this.client = clientParam;
    }

    /**
     * Set the authentication entry point.
     * @param authenticationEntryPointParam The entry point to set.
     */
    public void setAuthenticationEntryPoint(
            final AuthenticationEntryPoint authenticationEntryPointParam) {
        this.authenticationEntryPoint = authenticationEntryPointParam;
    }
}
