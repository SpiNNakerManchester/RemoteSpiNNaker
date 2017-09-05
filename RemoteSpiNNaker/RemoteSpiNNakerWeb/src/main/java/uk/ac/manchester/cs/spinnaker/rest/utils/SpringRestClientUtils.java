package uk.ac.manchester.cs.spinnaker.rest.utils;

import static org.springframework.security.core.context.SecurityContextHolder.getContext;
import static uk.ac.manchester.cs.spinnaker.rest.utils.RestClientUtils.createBearerClient;

import java.net.URL;

import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.springframework.security.authentication.ClientAuthenticationToken;

public class SpringRestClientUtils {
    public static <T> T createOIDCClient(final URL url, final Class<T> clazz) {
        try {
            final ClientAuthenticationToken clientAuth =
                    (ClientAuthenticationToken) getContext()
                            .getAuthentication();
            final OidcProfile oidcProfile =
                    (OidcProfile) clientAuth.getUserProfile();
            return createBearerClient(url, oidcProfile.getIdTokenString(),
                    clazz);
        } catch (final ClassCastException e) {
            throw new RuntimeException("Current Authentication is not OIDC");
        }
    }
}
