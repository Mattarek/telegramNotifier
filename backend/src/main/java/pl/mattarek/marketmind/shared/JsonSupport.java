package pl.mattarek.marketmind.shared;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Small helper for the few columns we keep as JSON text (lists of factors, risks, ids).
 */
@Component
public class JsonSupport {

    private final ObjectMapper objectMapper;

    public JsonSupport(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(final Object value) {
        if (value == null) {
            return null;
        }
        return objectMapper.writeValueAsString(value);
    }

    public <T> List<T> readList(final String json, final Class<T> elementType) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
    }

    public <T> T convert(final Object source, final Class<T> targetType) {
        return objectMapper.convertValue(source, targetType);
    }
}
