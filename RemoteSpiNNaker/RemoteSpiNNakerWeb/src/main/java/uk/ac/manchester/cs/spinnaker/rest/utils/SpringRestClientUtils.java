package uk.ac.manchester.cs.spinnaker.rest.utils;

import static org.springframework.security.core.context.SecurityContextHolder.getContext;
import static uk.ac.manchester.cs.spinnaker.rest.utils.RestClientUtils.createBearerClient;

import java.net.URL;

import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.springframework.security.authentication.ClientAuthenticationToken;

/**
 * Utilities for working with Spring Security.
 */
public abstract class SpringRestClientUtils {
    /**
     * Create a client that authenticates using the current connection's bearer
     * token.
     *
     * @param <T>
     *            The type of the client that will be created.
     * @param url
     *            The URL of the service to be a client to.
     * @param clazz
     *            The interface to proxy.
     * @return The proxy instance
     * @throws RuntimeException
     *             if there is no bearer token in the current connection's
     *             security context.
     */
    public static <T> T createOIDCClient(URL url, Class<T> clazz) {
        try {
            ClientAuthenticationToken clientAuth = (ClientAuthenticationToken)
                    getContext().getAuthentication();
            OidcProfile oidcProfile = (OidcProfile)
                    clientAuth.getUserProfile();
            return createBearerClient(url, oidcProfile.getIdTokenString(),
                    clazz);
        } catch (ClassCastException e) {
            throw new RuntimeException("Current Authentication is not OIDC");
        }
    }
}
