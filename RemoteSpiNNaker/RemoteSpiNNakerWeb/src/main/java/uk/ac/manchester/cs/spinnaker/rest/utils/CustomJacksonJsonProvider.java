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

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static javax.ws.rs.core.MediaType.WILDCARD;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * Extended JSON serialisation handler.
 */
@Provider
@Consumes(WILDCARD)
@Produces(WILDCARD)
public class CustomJacksonJsonProvider extends JacksonJsonProvider {
    /**
     * Mapper objects of this provider.
     */
    private final Set<ObjectMapper> registeredMappers = new HashSet<>();

    /**
     * The module of the provider.
     */
    private final SimpleModule module = new SimpleModule();

    /**
     * The date-time module of the provider.
     */
    private final JodaModule jodaModule = new JodaModule();

    /**
     * Add a deserialiser for a specific type.
     *
     * @param <T>
     *            The type that will be deserialised.
     * @param type
     *            The type.
     * @param deserialiser
     *            The deserialiser.
     */
    public <T> void addDeserialiser(final Class<T> type,
            final StdDeserializer<T> deserialiser) {
        module.addDeserializer(type, deserialiser);
    }

    /**
     * Register a new mapper.
     * @param type The class of the mapper
     * @param mediaType The media type to handle
     */
    private void registerMapper(final Class<?> type,
            final MediaType mediaType) {
        final var mapper = locateMapper(type, mediaType);
        if (!registeredMappers.contains(mapper)) {
            mapper.registerModule(module);
            mapper.setPropertyNamingStrategy(SNAKE_CASE);
            mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.registerModule(jodaModule);
            registeredMappers.add(mapper);
        }
    }

    @Override
    public Object readFrom(final Class<Object> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType,
            final MultivaluedMap<String, String> httpHeaders,
            final InputStream entityStream) throws IOException {
        registerMapper(type, mediaType);
        return super.readFrom(type, genericType, annotations, mediaType,
                httpHeaders, entityStream);
    }

    @Override
    public void writeTo(final Object value, final Class<?> type,
            final Type genericType, final Annotation[] annotations,
            final MediaType mediaType,
            final MultivaluedMap<String, Object> httpHeaders,
            final OutputStream entityStream) throws IOException {
        registerMapper(type, mediaType);
        super.writeTo(value, type, genericType, annotations, mediaType,
                httpHeaders, entityStream);
    }
}
