package com.miniframework.core;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** Valeur de la table de routage : où se trouve le code à exécuter. */
public class MappingInfo {

    private final Class<?> controllerClass;
    private final Object controllerInstance;
    private final Method method;

    private Pattern pattern;
    private List<String> pathVariableNames = Collections.emptyList();

    public MappingInfo(Class<?> controllerClass, Object controllerInstance, Method method) {
        this.controllerClass = controllerClass;
        this.controllerInstance = controllerInstance;
        this.method = method;
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Object getControllerInstance() {
        return controllerInstance;
    }

    public Method getMethod() {
        return method;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public List<String> getPathVariableNames() {
        return pathVariableNames;
    }

    public void setPathVariableNames(List<String> pathVariableNames) {
        this.pathVariableNames = pathVariableNames;
    }
}