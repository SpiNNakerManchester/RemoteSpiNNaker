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
public class DateTimeSerialiser extends StdSerializer<DateTime> {
	private static final DateTimeFormatter FORMAT = DateTimeFormat
			.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

	/**
	 * Default constructor.
	 */
	public DateTimeSerialiser() {
		super(DateTime.class);
	}

	@Override
	public void serialize(DateTime value, JsonGenerator jgen,
			SerializerProvider provider)
			throws IOException, JsonGenerationException {
		jgen.writeString(FORMAT.print(value));
	}
}
