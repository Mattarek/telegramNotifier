package pl.mattarek.marketmind.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny builder for JSON Schema fragments used as tool input schemas.
 */
final class JsonSchema {

    private JsonSchema() {
    }

    static Map<String, Object> object(final Map<String, Object> properties, final String... required) {
        final Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of(required));
        return schema;
    }

    static Map<String, Object> string(final String description) {
        return Map.of("type", "string", "description", description);
    }

    static Map<String, Object> stringEnum(final String description, final String... values) {
        return Map.of("type", "string", "description", description, "enum", List.of(values));
    }

    static Map<String, Object> integerEnum(final String description, final Integer... values) {
        return Map.of("type", "integer", "description", description, "enum", List.of(values));
    }

    static Map<String, Object> integer(final String description) {
        return Map.of("type", "integer", "description", description);
    }

    static Map<String, Object> number(final String description) {
        return Map.of("type", "number", "description", description);
    }

    static Map<String, Object> number(final String description, final double minimum, final double maximum) {
        return Map.of("type", "number", "description", description, "minimum", minimum, "maximum", maximum);
    }

    static Map<String, Object> array(final String description, final Map<String, Object> items) {
        return Map.of("type", "array", "description", description, "items", items);
    }

    /**
     * Keeps the order of properties (Map.of does not), which makes the schema easier to read in logs.
     */
    static Map<String, Object> props(final Object... keysAndValues) {
        final Map<String, Object> properties = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            properties.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return properties;
    }
}
