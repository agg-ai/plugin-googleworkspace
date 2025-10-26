package io.kestra.plugin.googleworkspace.helpers;

import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;

public class PropertyHelper {
    public static <T> T safeRender(RunContext runContext, Property<T> property, T defaultValue, Class<T> type) {
        if (property == null) {
            return defaultValue;
        }

        try {
            return runContext.render(property).as(type).orElse(defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
