package com.miniframework.core;

import com.miniframework.annotations.GetMapping;
import com.miniframework.annotations.PostMapping;
import com.miniframework.annotations.RequestMapping;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class FrontController extends HttpServlet {

    private static final Set<String> SUPPORTED_VERBS = Set.of("GET", "POST");

    private final List<Class<?>> controllers = new ArrayList<>();
    private final Map<UrlMethod, MappingInfo> routeTable = new LinkedHashMap<>();

    // ------------------------------------------------------------------
    // Initialisation : scan + construction de la table de routage
    // ------------------------------------------------------------------
    @Override
    public void init() throws ServletException {
        String basePackage = getInitParameter("base-package");
        if (basePackage == null || basePackage.isBlank()) {
            throw new ServletException("init-param 'base-package' manquant dans web.xml");
        }

        try {
            controllers.addAll(ClasspathScanner.scan(basePackage));
        } catch (Exception e) {
            throw new ServletException("Échec du scan du package '" + basePackage + "'", e);
        }

        for (Class<?> controllerClass : controllers) {
            registerController(controllerClass);
        }
        System.out.println("[FrontController] " + routeTable.size() + " route(s) enregistrée(s) : "
                + routeTable.keySet());
    }

    private void registerController(Class<?> controllerClass) throws ServletException {
        Object instance;
        try {
            instance = controllerClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new ServletException("Impossible d'instancier " + controllerClass.getName()
                    + " (constructeur public sans argument requis)", e);
        }

        for (Method method : controllerClass.getDeclaredMethods()) {
            UrlMethod key = resolveRoute(controllerClass, method);
            if (key == null) continue; // méthode sans annotation de mapping

            MappingInfo existing = routeTable.get(key);
            if (existing != null) {
                throw new ServletException("Doublon de route " + key + " : "
                        + existing.getControllerClass().getName() + "." + existing.getMethod().getName()
                        + " et " + controllerClass.getName() + "." + method.getName());
            }
            routeTable.put(key, new MappingInfo(controllerClass, instance, method));
        }
    }

    /**
     * Lit @RequestMapping, @GetMapping ou @PostMapping et les ramène à une même clé UrlMethod.
     * Retourne null si la méthode n'est pas une route.
     */
    private UrlMethod resolveRoute(Class<?> controllerClass, Method method) throws ServletException {
        RequestMapping rm = method.getAnnotation(RequestMapping.class);
        GetMapping gm = method.getAnnotation(GetMapping.class);
        PostMapping pm = method.getAnnotation(PostMapping.class);

        int count = (rm != null ? 1 : 0) + (gm != null ? 1 : 0) + (pm != null ? 1 : 0);
        if (count == 0) return null;

        String where = controllerClass.getName() + "." + method.getName();
        if (count > 1) {
            throw new ServletException("Plusieurs annotations de mapping sur " + where);
        }

        String url;
        String verb;
        if (rm != null) {
            url = rm.value();
            verb = rm.method().toUpperCase(Locale.ROOT);
        } else if (gm != null) {
            url = gm.value();
            verb = "GET";
        } else {
            url = pm.value();
            verb = "POST";
        }

        if (!SUPPORTED_VERBS.contains(verb)) {
            throw new ServletException("Verbe '" + verb + "' non supporté sur " + where
                    + " (supportés : " + SUPPORTED_VERBS + ")");
        }
        if (!url.startsWith("/")) url = "/" + url;
        return new UrlMethod(url, verb);
    }

    // ------------------------------------------------------------------
    // Traitement de la requête (tous verbes confondus)
    // ------------------------------------------------------------------
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String url = request.getRequestURI().substring(request.getContextPath().length());
        if (url.isEmpty()) url = "/";
        String verb = request.getMethod().toUpperCase(Locale.ROOT);

        MappingInfo mapping = routeTable.get(new UrlMethod(url, verb));

        if (mapping == null) {
            Set<String> allowed = allowedVerbsFor(url);
            if (!allowed.isEmpty()) {
                response.setHeader("Allow", String.join(", ", allowed));
                writeHtml(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "405 - Méthode non autorisée",
                        "<p>Le verbe <b>" + escape(verb) + "</b> n'est pas supporté sur <code>"
                                + escape(url) + "</code>.</p><p>Verbes autorisés : " + allowed + "</p>");
            } else {
                writeHtml(response, HttpServletResponse.SC_NOT_FOUND, "404 - Route introuvable",
                        "<p>Aucune route pour <code>" + escape(verb + " " + url) + "</code>.</p>"
                                + "<p>Routes valides :</p>" + renderRoutes());
            }
            return;
        }

        invoke(mapping, response);
    }

    private Set<String> allowedVerbsFor(String url) {
        Set<String> verbs = new TreeSet<>();
        for (UrlMethod key : routeTable.keySet()) {
            if (key.getUrl().equals(url)) verbs.add(key.getMethod());
        }
        return verbs;
    }

    // ------------------------------------------------------------------
    // Invocation dynamique par réflexion
    // ------------------------------------------------------------------
    private void invoke(MappingInfo mapping, HttpServletResponse response) throws IOException {
        Method method = mapping.getMethod();
        String label = mapping.getControllerClass().getSimpleName() + "." + method.getName() + "()";

        if (method.getParameterCount() > 0) {
            writeHtml(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "500 - Erreur d'invocation",
                    "<p><code>" + escape(label) + "</code> attend " + method.getParameterCount()
                            + " paramètre(s) : le binding sera disponible au Sprint 4.</p>");
            return;
        }

        try {
            Object result = method.invoke(mapping.getControllerInstance());

            System.out.println("[FrontController] " + label + " -> " + result);

            writeHtml(response, HttpServletResponse.SC_OK, "Invocation réussie",
                    "<p>Classe : <code>" + escape(mapping.getControllerClass().getName()) + "</code></p>"
                            + "<p>Méthode : <code>" + escape(method.getName()) + "</code></p>"
                            + "<p>Retour (voir aussi les logs Tomcat) : <code>" + escape(String.valueOf(result))
                            + "</code></p>");

        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            cause.printStackTrace();
            writeHtml(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "500 - Exception dans le contrôleur",
                    "<p><code>" + escape(label) + "</code> a levé <b>" + escape(cause.getClass().getName())
                            + "</b> : " + escape(String.valueOf(cause.getMessage())) + "</p>");

        } catch (IllegalAccessException e) {
            e.printStackTrace();
            writeHtml(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "500 - Accès refusé",
                    "<p>Impossible d'accéder à <code>" + escape(label)
                            + "</code> : la méthode et la classe doivent être <b>public</b>.</p>");

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            writeHtml(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "500 - Arguments invalides",
                    "<p>Appel de <code>" + escape(label) + "</code> impossible : " + escape(e.getMessage()) + "</p>");
        }
    }

    // ------------------------------------------------------------------
    // Utilitaires d'affichage
    // ------------------------------------------------------------------
    private String renderRoutes() {
        StringBuilder sb = new StringBuilder("<ul>");
        for (UrlMethod key : routeTable.keySet()) {
            sb.append("<li>").append(escape(key.toString())).append("</li>");
        }
        return sb.append("</ul>").toString();
    }

    private void writeHtml(HttpServletResponse response, int status, String title, String body)
            throws IOException {
        if (response.isCommitted()) return;
        response.setStatus(status);
        response.setContentType("text/html; charset=UTF-8"); // avant getWriter()
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>" + escape(title)
                + "</title></head><body><h1>" + escape(title) + "</h1>" + body + "</body></html>");
    }

    /** Échappement HTML minimal (évite l'injection via l'URL reflétée dans la page). */
    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}