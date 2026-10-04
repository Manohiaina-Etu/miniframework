package com.miniframework.core;

import com.miniframework.annotations.Controller;
import com.miniframework.annotations.RequestMapping;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class FrontController extends HttpServlet {

    /** Table de routage : (URL + verbe) -> métadonnées de la méthode. */
    private final Map<UrlMethod, MappingInfo> routeTable = new HashMap<>();

    /** Contrôleurs détectés. */
    private List<Class<?>> controllers;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String basePackage = config.getInitParameter("base-package");
        if (basePackage == null || basePackage.isBlank()) {
            throw new ServletException("init-param 'base-package' manquant dans web.xml");
        }

        try {
            controllers = ClasspathScanner.scan(basePackage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Sprint 2 : construction de la table de routage
        buildRouteTable();
    }

    private void buildRouteTable() throws ServletException {
        for (Class<?> controllerClass : controllers) {
            if (!controllerClass.isAnnotationPresent(Controller.class)) {
                continue;
            }

            // Une seule instance par contrôleur, partagée par toutes ses routes
            Object instance;
            try {
                instance = controllerClass.getDeclaredConstructor().newInstance();
            } catch (NoSuchMethodException e) {
                throw new ServletException("Le contrôleur " + controllerClass.getName()
                        + " doit avoir un constructeur public sans argument", e);
            } catch (ReflectiveOperationException e) {
                throw new ServletException("Impossible d'instancier "
                        + controllerClass.getName() + " : " + e, e);
            }

            for (Method method : controllerClass.getDeclaredMethods()) {
                RequestMapping mapping = method.getAnnotation(RequestMapping.class);
                if (mapping == null) {
                    continue;
                }

                UrlMethod key = new UrlMethod(mapping.value(), mapping.method());
                MappingInfo info = new MappingInfo(controllerClass, instance, method);

                // Détection des doublons : put() écraserait silencieusement
                if (routeTable.containsKey(key)) {
                    MappingInfo existing = routeTable.get(key);
                    throw new ServletException("Route en double : " + key + " est déclarée par "
                            + describe(existing) + " ET par " + describe(info));
                }
                routeTable.put(key, info);
            }
        }
        log("Table de routage construite : " + routeTable.size() + " route(s)");
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");

        // URL de routage = URI sans le context path (/Test/home -> /home)
        String url = request.getRequestURI().substring(request.getContextPath().length());
        if (url.isEmpty()) {
            url = "/";
        }

        // Sprint 2 : seul GET est utilisé (le vrai verbe arrive au Sprint 3a)
        MappingInfo info = routeTable.get(new UrlMethod(url, "GET"));

        PrintWriter out = response.getWriter();
        if (info == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.println("<html><body>");
            out.println("<h1>404 - Aucune route pour " + escape(url) + "</h1>");
            out.println("<p>URLs valides :</p><ul>");
            // TreeMap : affichage trié par verbe + URL
            Map<String, MappingInfo> sorted = new TreeMap<>();
            routeTable.forEach((k, v) -> sorted.put(k.toString(), v));
            sorted.forEach((k, v) -> out.println("<li>" + escape(k) + " &rarr; "
                    + escape(describe(v)) + "</li>"));
            out.println("</ul></body></html>");
            return;
        }

        out.println("<html><body>");
        out.println("<h1>Route trouvée : "+ request.getMethod() + " " + escape(url) + "</h1>");
        out.println("<p>Classe : " + escape(info.getControllerClass().getName()) + "</p>");
        out.println("<p>Méthode : " + escape(info.getMethod().getName()) + "</p>");
        out.println("</body></html>");
    }

    private String describe(MappingInfo info) {
        return info.getControllerClass().getSimpleName() + "." + info.getMethod().getName() + "()";
    }

    /** Échappement HTML minimal (l'URL vient du client). */
    private String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}