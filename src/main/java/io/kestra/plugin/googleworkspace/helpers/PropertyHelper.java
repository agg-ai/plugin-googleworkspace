package io.kestra.plugin.googleworkspace.helpers;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;

public class PropertyHelper {
    private static final ObjectMapper OBJECT_MAPPER = JacksonMapper.ofJson();

    public static <T> T safeRender(RunContext runContext, Property<T> property, T defaultValue, Class<T> type) {
        return safeRender(runContext, property, defaultValue, type, false);
    }

    public static <T> T safeRender(RunContext runContext, Property<T> property, T defaultValue, Class<T> type,
            boolean tryDeserialize) {
        if (property == null) {
            return defaultValue;
        }

        try {
            return runContext.render(property).as(type).orElse(defaultValue);
        } catch (Exception e) {
            if (tryDeserialize) {
                return tryDeserializeFromJson(runContext, property, defaultValue, type);
            }
            return defaultValue;
        }
    }

    public static <T> List<T> safeRenderList(RunContext runContext, Property<List<T>> property, List<T> defaultValue,
            Class<T> elementType) {
        return safeRenderList(runContext, property, defaultValue, elementType, false);
    }

    public static <T> List<T> safeRenderList(RunContext runContext, Property<List<T>> property, List<T> defaultValue,
            Class<T> elementType, boolean tryDeserialize) {
        if (property == null) {
            return defaultValue;
        }

        try {
            return runContext.render(property).asList(elementType);
        } catch (Exception e) {
            if (tryDeserialize) {
                return tryDeserializeListFromJson(runContext, property, defaultValue, elementType);
            }
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T tryDeserializeFromJson(RunContext runContext, Property<T> property, T defaultValue,
            Class<T> type) {
        try {
            Property<String> stringProperty = (Property<String>) property;
            var stringResult = runContext.render(stringProperty).as(String.class);

            if (stringResult.isPresent()) {
                String jsonString = stringResult.get();
                return OBJECT_MAPPER.readValue(jsonString, type);
            }

            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> tryDeserializeListFromJson(RunContext runContext, Property<List<T>> property,
            List<T> defaultValue, Class<T> elementType) {
        try {
            // Try to render as String and deserialize as JSON array
            // Use raw property type to allow casting
            Property<?> rawProperty = property;
            var stringResult = runContext.render((Property<String>) rawProperty).as(String.class);

            if (stringResult.isPresent()) {
                String jsonString = stringResult.get();
                return OBJECT_MAPPER.readValue(jsonString,
                        OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
            }
        } catch (Exception e) {
            // If JSON deserialization fails, return default value
        }
        return defaultValue;
    }
}
