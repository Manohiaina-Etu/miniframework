package com.miniframework.util;

/** Valeur HTTP non convertible vers le type demandé (=> réponse 400). */
public class TypeConversionException extends RuntimeException {
    public TypeConversionException(String message) {
        super(message);
    }
}