package dev.ezpapi.model;

import java.util.Objects;

public final class PlaceholderDefinition {
    private final String key;
    private final PlaceholderType type;
    private final Object defaultValue;
    private final String trueFormat;
    private final String falseFormat;
    private final String format;

    public PlaceholderDefinition(String key, PlaceholderType type, Object defaultValue, String trueFormat, String falseFormat, String format) {
        this.key = Objects.requireNonNull(key, "key");
        this.type = Objects.requireNonNull(type, "type");
        this.defaultValue = defaultValue;
        this.trueFormat = trueFormat;
        this.falseFormat = falseFormat;
        this.format = format;
    }

    public String key() {
        return key;
    }

    public PlaceholderType type() {
        return type;
    }

    public Object defaultValue() {
        return defaultValue;
    }

    public String trueFormat() {
        return trueFormat;
    }

    public String falseFormat() {
        return falseFormat;
    }

    public String format() {
        return format;
    }

    public boolean isBoolean() {
        return type == PlaceholderType.BOOLEAN;
    }

    public String formatValue(Object value) {
        if (value == null) {
            value = defaultValue;
        }

        if (isBoolean()) {
            boolean boolValue = Boolean.TRUE.equals(value);
            String selected = boolValue ? trueFormat : falseFormat;
            return selected != null ? selected : String.valueOf(boolValue);
        }

        if (format != null && !format.isBlank()) {
            return format.replace("%value%", String.valueOf(value));
        }

        return String.valueOf(value);
    }
}
