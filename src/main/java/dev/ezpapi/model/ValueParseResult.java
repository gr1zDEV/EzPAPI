package dev.ezpapi.model;

public record ValueParseResult(boolean success, Object value, String errorType) {
    public static ValueParseResult success(Object value) {
        return new ValueParseResult(true, value, null);
    }

    public static ValueParseResult failure(String errorType) {
        return new ValueParseResult(false, null, errorType);
    }
}
