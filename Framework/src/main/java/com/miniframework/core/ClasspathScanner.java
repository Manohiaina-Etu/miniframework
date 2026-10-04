package com.miniframework.core;

import com.miniframework.annotations.Controller;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Parcourt le classpath à partir d'un package racine et retourne
 * les classes annotées avec une des annotations de contrôleur.
 */
public class ClasspathScanner {

    private static final List<Class<? extends Annotation>> ANNOTATIONS_CIBLES =
            List.of(Controller.class);

    private ClasspathScanner() {}

    public static List<Class<?>> scan(String basePackage) throws IOException {
        if (basePackage == null || basePackage.isBlank()) {
            throw new IllegalArgumentException("Le package racine à scanner est vide.");
        }

        List<Class<?>> resultat = new ArrayList<>();
        String chemin = basePackage.replace('.', '/');

        // Le ContextClassLoader de Tomcat voit WEB-INF/classes de la webapp
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> ressources = loader.getResources(chemin);

        while (ressources.hasMoreElements()) {
            URL url = ressources.nextElement();
            if ("file".equals(url.getProtocol())) {
                try {
                    // toURI() décode les %20 (espaces) contrairement à url.getPath()
                    File dossier = new File(url.toURI());
                    scannerDossier(dossier, basePackage, loader, resultat);
                } catch (URISyntaxException e) {
                    throw new IOException("URL de package invalide : " + url, e);
                }
            }
        }
        return resultat;
    }

    private static void scannerDossier(File dossier, String packageCourant,
                                       ClassLoader loader, List<Class<?>> resultat) {
        File[] fichiers = dossier.listFiles();
        if (fichiers == null) {
            return;
        }
        for (File fichier : fichiers) {
            if (fichier.isDirectory()) {
                // Sous-package : on descend récursivement
                scannerDossier(fichier, packageCourant + "." + fichier.getName(), loader, resultat);
            } else if (fichier.getName().endsWith(".class")) {
                String nomSimple = fichier.getName().substring(0, fichier.getName().length() - 6);
                String nomComplet = packageCourant + "." + nomSimple;
                try {
                    Class<?> classe = Class.forName(nomComplet, false, loader);
                    if (estControleur(classe)) {
                        resultat.add(classe);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    System.err.println("[ClasspathScanner] Classe ignorée : " + nomComplet
                            + " (" + e + ")");
                }
            }
        }
    }

    private static boolean estControleur(Class<?> classe) {
        if (classe.isAnnotation() || classe.isInterface()) {
            return false;
        }
        for (Class<? extends Annotation> annotation : ANNOTATIONS_CIBLES) {
            if (classe.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return false;
    }
}