package com.miniframework.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Résultat d'un contrôleur : nom de la vue (sans préfixe ni suffixe) + données à exposer à la vue.
 * Le FrontController copie "data" dans les attributs de la requête puis fait un forward
 * vers /WEB-INF/views/{viewName}.jsp.
 */
public class ModelView {

    private final String viewName;
    private final Map<String, Object> data = new LinkedHashMap<>();

    public ModelView(String viewName) {
        this.viewName = viewName;
    }

    public String getViewName() {
        return viewName;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void addObject(String key, Object value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("La clé d'un ModelView ne peut pas être vide");
        }
        data.put(key, value);
    }
}