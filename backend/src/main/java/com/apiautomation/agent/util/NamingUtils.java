package com.apiautomation.agent.util;

public final class NamingUtils {

    private NamingUtils() {
    }

    public static String toPascalCase(String input) {
        if (input == null || input.isBlank()) return "Unknown";
        String[] parts = input.split("(?=[A-Z])|[\\s_\\-/{}]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(part.substring(0, 1).toUpperCase());
                if (part.length() > 1) {
                    sb.append(part.substring(1));
                }
            }
        }
        String result = sb.toString();
        return result.isEmpty() ? "Unknown" : result;
    }

    public static String toCamelCase(String input) {
        if (input == null || input.isBlank()) return "unknown";
        String pascal = toPascalCase(input);
        return pascal.substring(0, 1).toLowerCase() + pascal.substring(1);
    }

    public static String toEnumName(String input) {
        if (input == null || input.isBlank()) return "UNKNOWN";
        String result = input.replaceAll("([a-z])([A-Z])", "$1_$2")
                .replaceAll("[\\s\\-/{}]+", "_")
                .toUpperCase();
        return result;
    }
}
