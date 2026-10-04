package com.miniframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Raccourci pour @RequestMapping(value = "...", method = "POST"). */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PostMapping {
    String value();
}