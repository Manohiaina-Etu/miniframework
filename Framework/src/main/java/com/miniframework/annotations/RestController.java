package com.miniframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indique que la valeur de retour est sérialisée en JSON dans le corps de la réponse.
 * - Sur une classe : toutes ses routes sont REST, et le scanner la détecte (comme @Controller).
 * - Sur une méthode d'une classe @Controller : seule cette méthode est REST.
 * RUNTIME est indispensable : sans lui, la réflexion ne voit pas l'annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RestController {
}