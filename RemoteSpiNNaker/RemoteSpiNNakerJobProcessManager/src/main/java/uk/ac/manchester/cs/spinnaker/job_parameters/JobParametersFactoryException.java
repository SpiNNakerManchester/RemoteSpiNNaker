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
package uk.ac.manchester.cs.spinnaker.job_parameters;

/**
 * Indicates that whilst the job type was supported, there was an error
 * converting the job to parameters.
 */
public class JobParametersFactoryException extends Exception {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Create an exception with a message.
     *
     * @param message The message
     */
    public JobParametersFactoryException(final String message) {
        super(message);
    }

    /**
     * Create an exception with a message and cause.
     *
     * @param message The message
     * @param cause The cause
     */
    public JobParametersFactoryException(final String message,
            final Throwable cause) {
        super(message, cause);
    }
}
