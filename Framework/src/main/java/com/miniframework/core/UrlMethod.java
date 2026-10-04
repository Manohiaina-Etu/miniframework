package com.miniframework.core;

import java.util.Objects;

/**
 * Clé composite (URL + verbe HTTP) de la table de routage.
 * "GET /eleve" et "POST /eleve" sont ainsi deux routes distinctes.
 */
public final class UrlMethod {

    private final String url;
    private final String method;

    public UrlMethod(String url, String method) {
        this.url = url;
        // Normalisation : "post" -> "POST"
        this.method = (method == null ? "GET" : method).toUpperCase();
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    // equals() et hashCode() : obligatoires pour servir de clé dans un HashMap
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UrlMethod)) return false;
        UrlMethod that = (UrlMethod) o;
        return Objects.equals(url, that.url) && Objects.equals(method, that.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, method);
    }

    @Override
    public String toString() {
        return method + " " + url;
    }
}