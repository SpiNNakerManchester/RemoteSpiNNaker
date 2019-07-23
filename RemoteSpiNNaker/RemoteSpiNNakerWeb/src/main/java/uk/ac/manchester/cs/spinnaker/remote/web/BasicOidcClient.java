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
package uk.ac.manchester.cs.spinnaker.remote.web;

import static com.nimbusds.jose.JWSAlgorithm.RS256;

import org.pac4j.oidc.client.OidcClient;
import org.springframework.beans.factory.annotation.Value;

/**
 * Simple client that uses basic auth.
 */
class BasicOidcClient extends OidcClient {
    @Override
    @Value("${oidc.clientId}")
    public void setClientID(final String clientId) {
        super.setClientID(clientId);
    }

    @Override
    @Value("${oidc.secret}")
    public void setSecret(final String secret) {
        super.setSecret(secret);
    }

    @Override
    @Value("${oidc.discovery.uri}")
    public void setDiscoveryURI(final String oidcDiscoveryUri) {
        super.setDiscoveryURI(oidcDiscoveryUri);
    }

    /** Default constructor. */
    BasicOidcClient() {
        setScope("openid profile hbp.collab");
        setPreferredJwsAlgorithm(RS256);
    }
}
