package com.miniframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lie un paramètre HTTP (request.getParameter) à un argument de méthode de type simple.
 * Obligatoire pour les types simples ; non utilisé pour les objets complexes.
 */
@Retention(RetentionPolicy.RUNTIME) // sinon invisible par réflexion
@Target(ElementType.PARAMETER)
public @interface Param {
    /** Nom du paramètre HTTP (ex : "nom").
     * ex: public String sauvegarder(@Param("nom") String nom, @Param("age") Integer age) */
    String value();
}