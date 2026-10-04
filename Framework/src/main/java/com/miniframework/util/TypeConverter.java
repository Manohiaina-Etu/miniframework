package com.miniframework.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Convertit une String HTTP vers String, Integer/int, Long/long, Double/double,
 * Boolean/boolean et LocalDate (ISO yyyy-MM-dd).
 * Valeur absente ou vide : null (wrapper) ou valeur par défaut (primitif).
 */
public final class TypeConverter {

    private TypeConverter() {
    }

    public static boolean isSimpleType(Class<?> type) {
        return type == String.class
                || type == Integer.class || type == int.class
                || type == Long.class || type == long.class
                || type == Double.class || type == double.class
                || type == Boolean.class || type == boolean.class
                || type == LocalDate.class;
    }

    public static Object convert(String raw, Class<?> target) {
        if (target == String.class) {
            return raw;
        }
        if (raw == null || raw.isBlank()) {
            return defaultValue(target);
        }
        String value = raw.trim();
        try {
            if (target == Integer.class || target == int.class) {
                return Integer.valueOf(value);
            }
            if (target == Long.class || target == long.class) {
                return Long.valueOf(value);
            }
            if (target == Double.class || target == double.class) {
                return Double.valueOf(value);
            }
            if (target == Boolean.class || target == boolean.class) {
                if (value.equalsIgnoreCase("true")) return Boolean.TRUE;
                if (value.equalsIgnoreCase("false")) return Boolean.FALSE;
                throw new TypeConversionException(
                        "'" + value + "' n'est pas un booléen (attendu : true ou false)");
            }
            if (target == LocalDate.class) {
                return LocalDate.parse(value); // ISO : yyyy-MM-dd
            }
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new TypeConversionException(
                    "'" + value + "' n'est pas convertible en " + target.getSimpleName());
        }
        throw new TypeConversionException("Type non supporté : " + target.getName());
    }

    private static Object defaultValue(Class<?> target) {
        if (target == int.class) return 0;
        if (target == long.class) return 0L;
        if (target == double.class) return 0.0;
        if (target == boolean.class) return false;
        return null;
    }
}