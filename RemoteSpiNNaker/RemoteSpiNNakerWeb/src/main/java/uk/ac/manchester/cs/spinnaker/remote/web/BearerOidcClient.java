package uk.ac.manchester.cs.spinnaker.remote.web;

import static com.nimbusds.openid.connect.sdk.UserInfoResponse.parse;
import static com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata.parse;
import static org.pac4j.core.client.ClientType.HEADER_BASED;
import static org.pac4j.core.context.HttpConstants.AUTHORIZATION_HEADER;
import static org.pac4j.core.context.HttpConstants.DEFAULT_CONNECT_TIMEOUT;
import static org.pac4j.core.context.HttpConstants.DEFAULT_READ_TIMEOUT;
import static org.pac4j.core.exception.RequiresHttpAction.unauthorized;

import java.io.IOException;
import java.net.URL;

import javax.annotation.PostConstruct;

import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.ClientType;
import org.pac4j.core.client.DirectClient;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.oidc.profile.OidcProfile;
import org.springframework.beans.factory.annotation.Value;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.DefaultResourceRetriever;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

/**
 * HBP token bearer authentication client.
 */
public class BearerOidcClient
        extends
            DirectClient<BearerOidcClient.BearerCredentials, OidcProfile> {
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${oidc.discovery.uri}")
    private URL discoveryURI;
    @Value("${oidc.realm:}")
    private String realmName;
    private OIDCProviderMetadata oidcProvider;

    @PostConstruct
    private OIDCProviderMetadata getOIDCProvider() {
        try {
            if (oidcProvider == null) {
                oidcProvider = parse(new DefaultResourceRetriever(
                        DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT)
                                .retrieveResource(discoveryURI).getContent());
            }
        } catch (final Exception e) {
            logger.error("Could not contact OIDC provider; "
                    + "Bearer authentication will not work", e);
        }
        return oidcProvider;
    }

    public BearerOidcClient() {
    }

    private BearerOidcClient(final URL discoveryURI, final String realmName) {
        this.discoveryURI = discoveryURI;
        this.realmName = realmName;
        /*
         * Try to make the read immediately; otherwise we'll postpone until it's
         * needed.
         */
        getOIDCProvider();
    }

    @Override
    protected void internalInit(final WebContext webContext) {
        // Does Nothing
    }

    @Override
    public BearerCredentials getCredentials(final WebContext context)
            throws RequiresHttpAction {
        final String authorization =
                context.getRequestHeader(AUTHORIZATION_HEADER);
        if ((authorization == null)
                || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        // Verify the access token
        final String accessToken =
                authorization.substring(BEARER_PREFIX.length());
        if (accessToken.trim().isEmpty()) {
            return null;
        }
        try {
            final BearerAccessToken token = new BearerAccessToken(accessToken);
            if (getOIDCProvider().getUserInfoEndpointURI() == null) {
                logger.error("No User Info Endpoint!");
                return null;
            }

            return convertTokenToCredentials(context, accessToken, token);
        } catch (final Exception e) {
            throw new TechnicalException(e);
        }
    }

    private BearerCredentials convertTokenToCredentials(
            final WebContext context, final String accessToken,
            final BearerAccessToken token)
            throws IOException, ParseException, RequiresHttpAction {
        final UserInfoRequest userInfoRequest = new UserInfoRequest(
                getOIDCProvider().getUserInfoEndpointURI(), token);
        final HTTPRequest userInfoHttpRequest = userInfoRequest.toHTTPRequest();
        userInfoHttpRequest.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        userInfoHttpRequest.setReadTimeout(DEFAULT_READ_TIMEOUT);
        final HTTPResponse httpResponse = userInfoHttpRequest.send();
        logger.debug("Token response: status={}, content={}",
                httpResponse.getStatusCode(), httpResponse.getContent());

        final UserInfoResponse userInfoResponse = parse(httpResponse);

        if (userInfoResponse instanceof UserInfoErrorResponse) {
            logger.error("Bad User Info response, error={}",
                    ((UserInfoErrorResponse) userInfoResponse)
                            .getErrorObject());
            throw unauthorized("", context, realmName);
        }

        final OidcProfile profile = new OidcProfile(token);
        final UserInfoSuccessResponse userInfoSuccessResponse =
                (UserInfoSuccessResponse) userInfoResponse;
        final UserInfo userInfo = userInfoSuccessResponse.getUserInfo();
        if (userInfo != null) {
            profile.addAttributes(userInfo.toJWTClaimsSet().getClaims());
        }

        return new BearerCredentials(accessToken, profile);
    }

    @Override
    protected OidcProfile retrieveUserProfile(
            final BearerCredentials credentials, final WebContext context) {
        return credentials.getProfile();
    }

    @Override
    protected BaseClient<BearerCredentials, OidcProfile> newClient() {
        try {
            return new BearerOidcClient(discoveryURI, realmName);
        } catch (final Exception e) {
            throw new TechnicalException(e);
        }
    }

    @Override
    public ClientType getClientType() {
        return HEADER_BASED;
    }

    /**
     * OIDC bearer credentials. Consists of an access token and a profile.
     */
    static class BearerCredentials extends Credentials {
        private static final long serialVersionUID = 5585200812175851776L;

        private String accessToken;
        private OidcProfile profile;

        BearerCredentials(String accessToken, OidcProfile profile) {
            this.accessToken = accessToken;
            this.profile = profile;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public OidcProfile getProfile() {
            return profile;
        }

        @Override
        public void clear() {
            accessToken = null;
            profile = null;
        }
    }
}
