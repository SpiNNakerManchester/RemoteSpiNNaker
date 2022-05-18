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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * A deserialiser which deserialises classes based on unique properties that
 * they have. The classes to be deserialised need to be registered with a unique
 * property using the "register" function.
 *
 * @param <T>
 *      The type of values being deserialised.
 */
public class PropertyBasedDeserialiser<T> extends StdDeserializer<T> {
    private static final long serialVersionUID = 1L;

    /**
     * The registry of known elements.
     */
    private final Map<String, Class<? extends T>> registry = new HashMap<>();

    /**
     * Creates a new deserialiser.
     *
     * @param type
     *          The (super)class of the values that will be produced.
     */
    public PropertyBasedDeserialiser(final Class<T> type) {
        super(type);
    }

    /**
     * Registers a type against a property in the deserialiser.
     *
     * @param propertyName
     *            The name of the unique property that identifies the class.
     *            This is the JSON name.
     * @param type
     *            The class to register against the property.
     */
    public void register(final String propertyName,
            final Class<? extends T> type) {
        if (propertyName == null) {
            throw new IllegalArgumentException("propertyName must be non-null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must be non-null");
        }

        registry.put(propertyName, type);
    }

    @Override
    public T deserialize(final JsonParser parser,
            final DeserializationContext context)
            throws IOException, JsonProcessingException {
        final var root = parser.readValueAsTree();
        final var elementsIterator = root.fieldNames();
        while (elementsIterator.hasNext()) {
            var c = registry.get(elementsIterator.next());
            if (c != null) {
                return parser.getCodec().treeToValue(root, c);
            }
        }
        return null;
    }
}
