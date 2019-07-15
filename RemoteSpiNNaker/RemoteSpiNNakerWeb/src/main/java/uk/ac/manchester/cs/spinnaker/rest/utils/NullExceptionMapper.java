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

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.slf4j.LoggerFactory.getLogger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

/**
 * Convert NPEs into complaints about their most likely causes, bad inputs.
 *
 * @author Donal Fellows
 */
@Provider
public class NullExceptionMapper
        implements ExceptionMapper<NullPointerException> {

    /**
     * Log.
     */
    private final Logger log = getLogger(getClass());

    @Override
    public Response toResponse(final NullPointerException exception) {
        String msg = exception.getMessage();
        if ((msg == null) || msg.isEmpty()) {
            msg = "bad parameter";
        }
        log.info("trapped exception in service method", exception);
        return status(BAD_REQUEST).entity(msg).build();
    }
}
