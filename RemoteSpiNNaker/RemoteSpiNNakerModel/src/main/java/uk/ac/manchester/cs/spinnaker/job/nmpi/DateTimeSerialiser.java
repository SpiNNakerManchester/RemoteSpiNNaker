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
package uk.ac.manchester.cs.spinnaker.job.nmpi;

import java.io.IOException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Simple serialiser for ISO 8601 dates, as Java likes them to be.
 */
@SuppressWarnings("serial")
public class DateTimeSerialiser extends StdSerializer<DateTime> {

    /**
     * The format of the Date and Time.
     */
    private static final DateTimeFormatter FORMAT =
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    /**
     * Create a new serialiser.
     */
    public DateTimeSerialiser() {
        super(DateTime.class);
    }

    /**
     * Perform serialisation.
     */
    @Override
    public void serialize(final DateTime value, final JsonGenerator jgen,
            final SerializerProvider provider)
            throws IOException, JsonGenerationException {
        jgen.writeString(FORMAT.print(value));
    }
}
