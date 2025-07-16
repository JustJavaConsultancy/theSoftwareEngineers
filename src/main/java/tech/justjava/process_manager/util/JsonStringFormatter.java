package tech.justjava.process_manager.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import lombok.SneakyThrows;
import org.springframework.format.Formatter;


/**
 * Generic class for printing an Object as a JSON String and parsing a String to the given type.
 * Extends TypeReference to keep generic type information.
 */
public class JsonStringFormatter<T> extends TypeReference<T> implements Formatter<T> {

    private final ObjectMapper objectMapper;

    public JsonStringFormatter(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @SneakyThrows
    public T parse(final String text, final Locale locale) {
        return objectMapper.readValue(text, this);
    }

    @Override
    @SneakyThrows
    public String print(final T object, final Locale locale) {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

}
