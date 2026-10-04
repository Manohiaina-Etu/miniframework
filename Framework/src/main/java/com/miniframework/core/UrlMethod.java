package com.miniframework.core;

import java.util.Locale;
import java.util.Objects;

/** Clé composite de la table de routage : URL + verbe HTTP. */
public final class UrlMethod {

    private final String url;
    private final String method;

    public UrlMethod(String url, String method) {
        this.url = Objects.requireNonNull(url, "url");
        this.method = Objects.requireNonNull(method, "method").toUpperCase(Locale.ROOT);
    }

    public String getUrl() { return url; }
    public String getMethod() { return method; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UrlMethod)) return false;
        UrlMethod other = (UrlMethod) o;
        return url.equals(other.url) && method.equals(other.method);
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