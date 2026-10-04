package com.miniframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Associe une méthode de contrôleur à une URL et à un verbe HTTP.
 * RUNTIME est indispensable : la réflexion doit voir l'annotation à l'exécution.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequestMapping {
    String value();
    String method() default "GET";
}