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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SERVER_ERROR;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.StringWriter;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
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
    private static final Logger logger =
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
        final var family = responseContext.getStatusInfo().getFamily();
        if ((family == CLIENT_ERROR) || (family == SERVER_ERROR)) {
            logger.trace("Error when sending request:");
            logger.trace(INDENT + "Headers:");
            final var headers = requestContext.getStringHeaders();
            for (final var headerName : headers.keySet()) {
                for (final var headerValue : headers.get(headerName)) {
                    logger.trace(IND2 + "{}: {}", headerName, headerValue);
                }
            }

            logger.trace(INDENT + "Entity:");
            logger.trace(IND2 + "{}", requestContext.getEntity());

            final var json = getRequestAsJSON(requestContext);
            if (nonNull(json)) {
                logger.trace(INDENT + "JSON version:");
                logger.trace(IND2 + "{}", json);
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
            final var jsonWriter = new StringWriter();
            try (var jsonOutput = new WriterOutputStream(jsonWriter, UTF_8)) {
                provider.writeTo(requestContext.getEntity(),
                        requestContext.getEntityClass(),
                        requestContext.getEntityType(),
                        requestContext.getEntityAnnotations(),
                        requestContext.getMediaType(),
                        requestContext.getHeaders(), jsonOutput);
            }
            return jsonWriter.toString();
        } catch (final Exception e) {
            logger.trace("problem when converting request to JSON", e);
            return null;
        }
    }
}
