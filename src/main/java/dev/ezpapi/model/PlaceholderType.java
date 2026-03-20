package dev.ezpapi.model;

public enum PlaceholderType {
    BOOLEAN,
    INTEGER,
    DOUBLE,
    STRING;

    public static PlaceholderType fromConfig(String input) {
        if (input == null) {
            return null;
        }

        try {
            return PlaceholderType.valueOf(input.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
