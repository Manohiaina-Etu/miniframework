package com.miniframework.core;

import java.lang.reflect.Method;

/** Valeur de la table de routage : où se trouve le code à exécuter. */
public class MappingInfo {

    private final Class<?> controllerClass;
    private final Object controllerInstance;
    private final Method method;

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
}