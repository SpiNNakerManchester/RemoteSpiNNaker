package uk.ac.manchester.cs.spinnaker.rest.utils;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES;
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

@Provider
@Consumes(WILDCARD)
@Produces(WILDCARD)
public class CustomJacksonJsonProvider extends JacksonJsonProvider {
    private final Set<ObjectMapper> registeredMappers = new HashSet<>();
    private final SimpleModule module = new SimpleModule();
    private final JodaModule jodaModule = new JodaModule();

    public <T> void addDeserialiser(final Class<T> type,
            final StdDeserializer<T> deserialiser) {
        module.addDeserializer(type, deserialiser);
    }

    private void registerMapper(final Class<?> type,
            final MediaType mediaType) {
        final ObjectMapper mapper = locateMapper(type, mediaType);
        if (!registeredMappers.contains(mapper)) {
            mapper.registerModule(module);
            mapper.setPropertyNamingStrategy(
                    CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
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
