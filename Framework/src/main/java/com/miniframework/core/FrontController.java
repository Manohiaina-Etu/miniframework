package com.miniframework.core;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Point d'entrée unique du framework (mappé sur "/").
 */
public class FrontController extends HttpServlet {

    private static final String PARAM_BASE_PACKAGE = "base-package";

    private List<Class<?>> controllers = new ArrayList<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String basePackage = config.getInitParameter(PARAM_BASE_PACKAGE);
        if (basePackage == null || basePackage.isBlank()) {
            throw new ServletException("init-param '" + PARAM_BASE_PACKAGE
                    + "' manquant dans web.xml");
        }

        try {
            controllers = ClasspathScanner.scan(basePackage);
        } catch (IOException e) {
            throw new ServletException("Échec du scan du package '" + basePackage + "'", e);
        }

        System.out.println("[FrontController] " + controllers.size()
                + " contrôleur(s) détecté(s) dans " + basePackage);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">"
                + "<title>Sprint 1</title></head><body>");
        out.println("<h1>Contrôleurs détectés</h1>");
        if (controllers.isEmpty()) {
            out.println("<p>Aucun contrôleur trouvé.</p>");
        } else {
            out.println("<ul>");
            for (Class<?> c : controllers) {
                out.println("<li>" + c.getName() + "</li>");
            }
            out.println("</ul>");
        }
        out.println("</body></html>");
    }
}