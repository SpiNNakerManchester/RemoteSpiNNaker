package uk.ac.manchester.cs.spinnaker.rest.utils;

import static org.springframework.security.core.context.SecurityContextHolder.getContext;
import static uk.ac.manchester.cs.spinnaker.rest.utils.RestClientUtils.createBearerClient;

import java.net.URL;

import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.springframework.security.authentication.ClientAuthenticationToken;

public abstract class SpringRestClientUtils {
	private SpringRestClientUtils() {
	}

	public static <T> T createOIDCClient(URL url, Class<T> clazz) {
		try {
			OidcProfile oidcProfile = (OidcProfile) getAuth().getUserProfile();
			return createBearerClient(url, oidcProfile.getIdTokenString(),
					clazz);
		} catch (ClassCastException e) {
			throw new RuntimeException("Current Authentication is not OIDC");
		}
	}

	private static ClientAuthenticationToken getAuth()
			throws ClassCastException {
		return (ClientAuthenticationToken) getContext().getAuthentication();
	}
}
