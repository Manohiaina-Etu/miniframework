package com.miniframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Raccourci pour @RequestMapping(value = "...", method = "GET"). */
@Retention(RetentionPolicy.RUNTIME) // sinon invisible à la réflexion
@Target(ElementType.METHOD)
public @interface GetMapping {
    String value();
}