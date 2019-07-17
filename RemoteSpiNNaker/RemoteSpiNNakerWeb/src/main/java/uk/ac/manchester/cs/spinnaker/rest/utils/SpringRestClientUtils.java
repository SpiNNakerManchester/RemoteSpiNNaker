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
    public static <T> T createOIDCClient(final URL url, final Class<T> clazz) {
        try {
            OidcProfile oidcProfile = (OidcProfile) getAuth().getUserProfile();
            return createBearerClient(url, oidcProfile.getIdTokenString(),
                    clazz);
        } catch (ClassCastException e) {
            throw new RuntimeException("Current Authentication is not OIDC");
        }
    }

    /**
     * Gets the current authentication token from the Spring security context.
     *
     * @return The current authentication token.
     * @throws ClassCastException
     *             If an unexpected type of authentication token is present,
     *             which indicates that we shouldn't be authenticating using
     *             this.
     */
    private static ClientAuthenticationToken getAuth()
            throws ClassCastException {
        return (ClientAuthenticationToken) getContext().getAuthentication();
    }
}
