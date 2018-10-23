package uk.ac.manchester.cs.spinnaker.rest.utils;

import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SERVER_ERROR;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.output.WriterOutputStream;
import org.slf4j.Logger;

/**
 * Better logging of errors.
 */
// Only public because of the annotation
@Provider
public class ErrorCaptureResponseFilter implements ClientResponseFilter {

    /**
     * The JSON provider of the filter.
     */
    private final CustomJacksonJsonProvider provider =
            new CustomJacksonJsonProvider();

    /**
     * Logging.
     */
    private static final Logger LOG =
            getLogger(ErrorCaptureResponseFilter.class);

    /**
     * True if the log should be written.
     */
    private volatile boolean writeToLog = true;

    /**
     * Level 1 indent.
     */
    private static final String INDENT = "    "; // 4 spaces

    /**
     * Level 2 intent.
     */
    private static final String IND2 = INDENT + INDENT;

    @Override
    public void filter(final ClientRequestContext requestContext,
            final ClientResponseContext responseContext) throws IOException {
        if (!writeToLog) {
            return;
        }
        final Family family = responseContext.getStatusInfo().getFamily();
        if ((family == CLIENT_ERROR) || (family == SERVER_ERROR)) {
            LOG.trace("Error when sending request:");
            LOG.trace(INDENT + "Headers:");
            final MultivaluedMap<String, String> headers =
                    requestContext.getStringHeaders();
            for (final String headerName : headers.keySet()) {
                for (final String headerValue : headers.get(headerName)) {
                    LOG.trace(IND2 + headerName + ": " + headerValue);
                }
            }

            LOG.trace(INDENT + "Entity:");
            LOG.trace(IND2 + requestContext.getEntity());

            final String json = getRequestAsJSON(requestContext);
            if (json != null) {
                LOG.trace(INDENT + "JSON version:");
                LOG.trace(IND2 + json);
            }
        }
    }

    /**
     * Convert a request to a JSON object.
     * @param requestContext The context of the request
     * @return A JSON String
     */
    private String getRequestAsJSON(final ClientRequestContext requestContext) {
        try {
            final StringWriter jsonWriter = new StringWriter();
            try (OutputStream jsonOutput =
                    new WriterOutputStream(jsonWriter, "UTF-8")) {
                provider.writeTo(requestContext.getEntity(),
                        requestContext.getEntityClass(),
                        requestContext.getEntityType(),
                        requestContext.getEntityAnnotations(),
                        requestContext.getMediaType(),
                        requestContext.getHeaders(), jsonOutput);
            }
            return jsonWriter.toString();
        } catch (final Exception e) {
            LOG.trace("problem when converting request to JSON", e);
            return null;
        }
    }
}
