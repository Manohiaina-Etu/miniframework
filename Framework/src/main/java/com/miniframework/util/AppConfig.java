package com.miniframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Configuration de l'application (fichier .properties du classpath de la webapp).
 * Chargée UNE fois par le FrontController dans init(), puis en lecture seule.
 */
public final class AppConfig {

    private static volatile Properties properties;

    private AppConfig() {}

    /** Charge le fichier depuis le classpath de la webapp (WEB-INF/classes). */
    public static void load(String resourceName) throws IOException {
        System.out.println("==>AppConfig.load.resourceName::" + resourceName);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream in = loader.getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IOException("Fichier de configuration introuvable : " + resourceName
                        + " (attendu dans WEB-INF/classes)");
            }
            Properties p = new Properties();
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                p.load(reader);
            }
            properties = p;
        }
    }

    /** Valeur obligatoire : clé absente = erreur claire. */
    public static String get(String key) {
        String value = get(key, null);
        if (value == null) {
            throw new IllegalStateException("Clé de configuration manquante : '" + key + "'");
        }
        return value;
    }

    /** Valeur avec défaut si la clé est absente. */
    public static String get(String key, String defaultValue) {
        Properties p = properties;
        if (p == null) {
            throw new IllegalStateException(
                    "AppConfig n'est pas chargée (AppConfig.load doit être appelée dans FrontController.init()).");
        }
        String value = p.getProperty(key);
        return value == null ? defaultValue : value.trim();
    }
}
