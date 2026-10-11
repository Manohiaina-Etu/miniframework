package com.miniframework.core;

import com.miniframework.annotations.GetMapping;
import com.miniframework.annotations.Param;
import com.miniframework.annotations.PathVariable;
import com.miniframework.annotations.PostMapping;
import com.miniframework.annotations.RequestMapping;
import com.miniframework.annotations.RestController;
import com.miniframework.model.ModelView;
import com.miniframework.util.JsonSerializer;
import com.miniframework.util.TypeConversionException;
import com.miniframework.util.TypeConverter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FrontController extends HttpServlet {

    private static final Set<String> SUPPORTED_VERBS = Set.of("GET", "POST");

    private static final Pattern PATH_VAR = Pattern.compile("\\{([^/{}]+)}");

    /** Indice maximal accepté dans une liste (ex : notes[1000]) : évite les allocations géantes. */
    private static final int MAX_INDEX = 1000;

    private final List<Class<?>> controllers = new ArrayList<>();

    private final Map<UrlMethod, MappingInfo> routeTable = new LinkedHashMap<>();

    private final Map<UrlMethod, MappingInfo> dynamicRoutes = new LinkedHashMap<>();

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
        System.out.println("[FrontController] " + (routeTable.size() + dynamicRoutes.size())
                + " route(s) enregistrée(s) : statiques=" + routeTable.keySet()
                + " dynamiques=" + dynamicRoutes.keySet());
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

            registerRoute(key, new MappingInfo(controllerClass, instance, method));
        }
    }

    /**
     * Range la route dans la table statique ou dynamique et détecte les doublons.
     * Une URL contenant '{' est dynamique : elle est compilée en regex.
     */
    private void registerRoute(UrlMethod key, MappingInfo info) throws ServletException {
        boolean dynamic = key.getUrl().contains("{");
        if (dynamic) {
            compileDynamicRoute(key.getUrl(), info);
        }

        MappingInfo existing = routeTable.get(key);
        if (existing == null) existing = dynamicRoutes.get(key);

        if (existing == null && dynamic) {
            for (Map.Entry<UrlMethod, MappingInfo> e : dynamicRoutes.entrySet()) {
                if (e.getKey().getMethod().equals(key.getMethod())
                        && e.getValue().getPattern().pattern().equals(info.getPattern().pattern())) {
                    existing = e.getValue();
                    break;
                }
            }
        }

        if (existing != null) {
            throw new ServletException("Doublon de route " + key + " : "
                    + existing.getControllerClass().getName() + "." + existing.getMethod().getName()
                    + " et " + info.getControllerClass().getName() + "." + info.getMethod().getName());
        }

        if (dynamic) {
            dynamicRoutes.put(key, info);
        } else {
            routeTable.put(key, info);
        }
    }

    /**
     * Transforme "/eleve/{id}" en "^\Q/eleve/\E([^/]+)$" et mémorise la liste ["id"].
     * Les parties fixes sont protégées par Pattern.quote (un '.' ou un '?' dans l'URL
     * ne doit pas être interprété comme un opérateur de regex).
     */
    private void compileDynamicRoute(String url, MappingInfo info) {
        Matcher m = PATH_VAR.matcher(url);
        StringBuilder regex = new StringBuilder("^");
        List<String> names = new ArrayList<>();
        int last = 0;
        while (m.find()) {
            regex.append(Pattern.quote(url.substring(last, m.start())));
            regex.append("([^/]+)");
            names.add(m.group(1));
            last = m.end();
        }
        regex.append(Pattern.quote(url.substring(last))).append("$");

        info.setPattern(Pattern.compile(regex.toString()));
        info.setPathVariableNames(names);
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

        Map<String, String> pathVars = new LinkedHashMap<>();

        MappingInfo mapping = resolve(url, verb, pathVars);

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

        invoke(mapping, request, response, pathVars);
    }

    /**
     * Résolution en deux temps :
     * 1. recherche exacte dans la table statique (prioritaire : /eleve/form gagne sur /eleve/{id}) ;
     * 2. sinon parcours des routes dynamiques ayant le même verbe.
     * Les valeurs capturées sont écrites dans pathVars.
     */
    private MappingInfo resolve(String url, String verb, Map<String, String> pathVars) {
        MappingInfo exact = routeTable.get(new UrlMethod(url, verb));
        if (exact != null) return exact;

        for (Map.Entry<UrlMethod, MappingInfo> e : dynamicRoutes.entrySet()) {
            if (!e.getKey().getMethod().equals(verb)) continue;

            MappingInfo info = e.getValue();
            Matcher m = info.getPattern().matcher(url);
            if (m.matches()) {
                List<String> names = info.getPathVariableNames();
                for (int i = 0; i < names.size(); i++) {
                    pathVars.put(names.get(i), m.group(i + 1)); // le groupe 0 = tout le match
                }
                return info;
            }
        }
        return null;
    }

    private Set<String> allowedVerbsFor(String url) {
        Set<String> verbs = new TreeSet<>();
        for (UrlMethod key : routeTable.keySet()) {
            if (key.getUrl().equals(url)) verbs.add(key.getMethod());
        }
        for (Map.Entry<UrlMethod, MappingInfo> e : dynamicRoutes.entrySet()) {
            if (e.getValue().getPattern().matcher(url).matches()) verbs.add(e.getKey().getMethod());
        }
        return verbs;
    }

    // ------------------------------------------------------------------
    // Binding des paramètres (Sprint 4 + extension @Param liste/objet)
    // ------------------------------------------------------------------

    /**
     * Construit le tableau d'arguments à partir des annotations de chaque paramètre :
     * - @Param + type simple        -> paramètre HTTP converti ;
     * - @Param + List / objet       -> binding récursif avec le nom comme préfixe (bindValue) ;
     * - @PathVariable               -> variable de chemin ;
     * - sans annotation : type simple ou liste = erreur, objet = model binding sans préfixe.
     */
    private Object[] buildArgs(Method method, HttpServletRequest request, Map<String, String> pathVars) {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];
            Class<?> type = p.getType();
            Param param = p.getAnnotation(Param.class);
            PathVariable pathVar = p.getAnnotation(PathVariable.class);

            try {
                if (param != null) {
                    if (TypeConverter.isSimpleType(type)) {
                        args[i] = TypeConverter.convert(request.getParameter(param.value()), type);
                    } else {
                        args[i] = bindValue(p.getParameterizedType(), param.value(), request);
                    }
                } else if (pathVar != null) {
                    args[i] = TypeConverter.convert(pathVars.get(pathVar.value()), type);
                } else if (TypeConverter.isSimpleType(type)) {
                    throw new IllegalStateException("Paramètre '" + p.getName() + "' de "
                            + method.getDeclaringClass().getSimpleName() + "." + method.getName()
                            + "() : @Param ou @PathVariable obligatoire pour un type simple.");
                } else if (!estTypeObjet(type)) {
                    throw new IllegalStateException("Paramètre '" + p.getName() + "' de "
                            + method.getDeclaringClass().getSimpleName() + "." + method.getName()
                            + "() : le type " + type.getSimpleName() + " ne peut pas être lié sans annotation"
                            + " (pour une liste, utilise @Param(\"nom\")).");
                } else {
                    args[i] = bindModel(type, "", request); // objet complexe sans préfixe (ex : Eleve)
                }
            } catch (TypeConversionException e) {
                String nom = param != null ? param.value()
                        : (pathVar != null ? pathVar.value() : p.getName());
                throw new TypeConversionException("Paramètre '" + nom + "' : " + e.getMessage());
            }
        }
        return args;
    }

    private Object bindValue(Type type, String cle, HttpServletRequest request) {
        Class<?> raw = rawClass(type);

        if (TypeConverter.isSimpleType(raw)) {
            try {
                return TypeConverter.convert(request.getParameter(cle), raw);
            } catch (TypeConversionException e) {
                throw new TypeConversionException("Champ '" + cle + "' : " + e.getMessage());
            }
        }

        if (raw == List.class || raw == ArrayList.class) {
            return bindList(type, cle, request);
        }

        if (!estTypeObjet(raw)) {
            throw new IllegalStateException("Type non supporté pour le binding de '" + cle + "' : "
                    + raw.getName() + " (supportés : types simples, List<...> et objets avec"
                    + " constructeur par défaut).");
        }

        if (!present(cle, request)) return null;
        return bindModel(raw, cle, request);
    }

    private List<Object> bindList(Type type, String cle, HttpServletRequest request) {
        if (!(type instanceof ParameterizedType pt)) {
            throw new IllegalStateException("Liste sans type générique pour '" + cle
                    + "' (écris List<String> et non List tout court).");
        }
        Type elemType = pt.getActualTypeArguments()[0];
        Class<?> elemRaw = rawClass(elemType); // refuse List<?> ou List<T>

        List<Object> liste = new ArrayList<>();

        SortedSet<Integer> indices = collectIndices(cle, request);
        if (!indices.isEmpty()) {
            for (int idx : indices) {
                liste.add(bindValue(elemType, cle + "[" + idx + "]", request));
            }
            return liste;
        }

        String[] valeurs = request.getParameterValues(cle);
        if (valeurs != null) {
            if (!TypeConverter.isSimpleType(elemRaw)) {
                throw new IllegalStateException("La clé répétée '" + cle + "' ne convient qu'aux éléments"
                        + " simples ; pour des objets, utilise des indices (" + cle + "[0].champ=...).");
            }
            for (String valeur : valeurs) {
                try {
                    liste.add(TypeConverter.convert(valeur, elemRaw));
                } catch (TypeConversionException e) {
                    throw new TypeConversionException("Champ '" + cle + "' : " + e.getMessage());
                }
            }
        }
        return liste;
    }

    private SortedSet<Integer> collectIndices(String cle, HttpServletRequest request) {
        Pattern pattern = Pattern.compile("^" + Pattern.quote(cle) + "\\[(\\d+)]");
        SortedSet<Integer> indices = new TreeSet<>();

        Enumeration<String> noms = request.getParameterNames();
        while (noms.hasMoreElements()) {
            Matcher m = pattern.matcher(noms.nextElement());
            if (!m.find()) continue;
            String chiffres = m.group(1);
            if (chiffres.length() > 9 || Integer.parseInt(chiffres) > MAX_INDEX) {
                throw new TypeConversionException("Indice trop grand pour '" + cle + "' (maximum "
                        + MAX_INDEX + ")");
            }
            indices.add(Integer.parseInt(chiffres));
        }
        return indices;
    }

    /**
     * Vrai si au moins un paramètre HTTP concerne cette clé : "cle", "cle.xxx" ou "cle[...]".
     * Le séparateur est inclus pour que "eleves[1]" ne corresponde pas à "eleves[10].nom".
     */
    private boolean present(String cle, HttpServletRequest request) {
        String pointe = cle + ".";
        String crochet = cle + "[";
        Enumeration<String> noms = request.getParameterNames();
        while (noms.hasMoreElements()) {
            String nom = noms.nextElement();
            if (nom.equals(cle) || nom.startsWith(pointe) || nom.startsWith(crochet)) return true;
        }
        return false;
    }

    /** Extrait la classe d'un Type : Eleve -> Eleve.class ; List<String> -> List.class. */
    private Class<?> rawClass(Type type) {
        if (type instanceof Class<?> classe) {
            return classe;
        }
        if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> brute) {
            return brute;
        }
        throw new IllegalStateException("Type générique non supporté pour le binding : "
                + type.getTypeName());
    }

    private boolean estTypeObjet(Class<?> type) {
        return !(type.isInterface() || type.isArray() || type.isPrimitive()
                || Modifier.isAbstract(type.getModifiers())
                || Collection.class.isAssignableFrom(type)
                || Map.class.isAssignableFrom(type));
    }

    /**
     * Model binding : instancie l'objet par son constructeur par défaut, puis appelle chaque
     * setter dont la clé (prefixe + "." + propriété, ou la propriété seule si prefixe vide)
     * correspond à au moins un paramètre HTTP. Le type de chaque setter peut être simple,
     * une liste ou un objet imbriqué : bindValue gère la récursion.
     */
    private Object bindModel(Class<?> type, String prefixe, HttpServletRequest request) {
        Object instance;
        try {
            instance = type.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("La classe " + type.getName()
                    + " doit avoir un constructeur par défaut (sans argument) public.");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Impossible d'instancier " + type.getName()
                    + " : " + e.getMessage());
        }

        for (Method setter : type.getMethods()) {
            if (!setter.getName().startsWith("set") || setter.getName().length() <= 3
                    || setter.getParameterCount() != 1) {
                continue;
            }

            String prop = Character.toLowerCase(setter.getName().charAt(3)) + setter.getName().substring(4);
            String cle = prefixe.isEmpty() ? prop : prefixe + "." + prop;
            if (!present(cle, request)) continue; // aucun paramètre HTTP pour ce champ

            Type paramType = setter.getGenericParameterTypes()[0];
            try {
                setter.invoke(instance, bindValue(paramType, cle, request));
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new IllegalStateException(setter.getName() + "() de " + type.getName()
                        + " a levé " + cause.getClass().getName() + " : " + cause.getMessage());
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Accès refusé à " + setter.getName() + "() de "
                        + type.getName());
            }
        }
        return instance;
    }

    // ------------------------------------------------------------------
    // Invocation dynamique par réflexion
    // ------------------------------------------------------------------
    private void invoke(MappingInfo mapping, HttpServletRequest request, HttpServletResponse response,
                        Map<String, String> pathVars) throws IOException, ServletException {
        Method method = mapping.getMethod();
        String label = mapping.getControllerClass().getSimpleName() + "." + method.getName() + "()";

        Object[] args;
        try {
            args = buildArgs(method, request, pathVars);
        } catch (TypeConversionException e) {
            writeHtml(response, HttpServletResponse.SC_BAD_REQUEST, "400 - Requête invalide",
                    "<p>" + escape(e.getMessage()) + "</p>");
            return;
        } catch (IllegalStateException e) {
            writeHtml(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "500 - Erreur de binding",
                    "<p>" + escape(e.getMessage()) + "</p>");
            return;
        }

        Object result;
        try {
            result = method.invoke(mapping.getControllerInstance(), args);

        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            cause.printStackTrace();
            writeHtml(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "500 - Exception dans le contrôleur",
                    "<p><code>" + escape(label) + "</code> a levé <b>" + escape(cause.getClass().getName())
                            + "</b> : " + escape(String.valueOf(cause.getMessage())) + "</p>");
            return;

        } catch (IllegalAccessException e) {
            // Méthode non publique / classe non accessible
            e.printStackTrace();
            writeHtml(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "500 - Accès refusé",
                    "<p>Impossible d'accéder à <code>" + escape(label)
                            + "</code> : la méthode et la classe doivent être <b>public</b>.</p>");
            return;

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            writeHtml(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "500 - Arguments invalides",
                    "<p>Appel de <code>" + escape(label) + "</code> impossible : " + escape(e.getMessage()) + "</p>");
            return;
        }

        renderResult(mapping, result, request, response);
    }

    /**
     * Choisit le rendu selon la valeur retournée par le contrôleur :
     * - void / null  -> 200 sans corps ;
     * - ModelView    -> copie dans les attributs de requête puis forward vers la JSP ;
     */
    private void renderResult(MappingInfo mapping, Object result, HttpServletRequest request,
                              HttpServletResponse response) throws IOException, ServletException {
        Method method = mapping.getMethod();
        String label = mapping.getControllerClass().getSimpleName() + "." + method.getName() + "()";
        boolean rest = isRest(mapping);

        if (method.getReturnType() == void.class || (result == null && !rest)) {
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_OK);
            }
            return;
        }

        if (rest) {
            renderJson(label, result, response);
            return;
        }

        if (result instanceof ModelView mv) {
            forwardToView(mv, label, request, response);
            return;
        }

        System.out.println("[FrontController] " + label + " -> " + result);
        writeHtml(response, HttpServletResponse.SC_OK, "Invocation réussie",
                "<p>Classe : <code>" + escape(mapping.getControllerClass().getName()) + "</code></p>"
                        + "<p>Méthode : <code>" + escape(method.getName()) + "</code></p>"
                        + "<p>Retour : <code>" + escape(String.valueOf(result)) + "</code></p>");
    }

    /**
     * Une route est REST si @RestController est sur la classe OU sur la méthode.
     */
    private boolean isRest(MappingInfo mapping) {
        return mapping.getControllerClass().isAnnotationPresent(RestController.class)
                || mapping.getMethod().isAnnotationPresent(RestController.class);
    }

    /**
     * Sérialise le résultat en JSON puis l'écrit. La sérialisation se fait AVANT d'écrire quoi que ce
     * soit : en cas d'erreur (cycle, getter qui plante), on peut encore répondre proprement en 500.
     */
    private void renderJson(String label, Object result, HttpServletResponse response) throws IOException {
        if (result instanceof ModelView) {
            writeHtml(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "500 - ModelView interdit en REST",
                    "<p><code>" + escape(label) + "</code> est une route REST : elle ne peut pas retourner "
                            + "un <code>ModelView</code> (retourne un objet, une liste ou une Map).</p>");
            return;
        }

        String json;
        try {
            json = JsonSerializer.toJson(result);
        } catch (IllegalStateException e) {
            writeHtml(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "500 - Erreur de sérialisation JSON",
                    "<p><code>" + escape(label) + "</code> : " + escape(e.getMessage()) + "</p>");
            return;
        }

        if (response.isCommitted()) return;
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json; charset=UTF-8"); // avant getWriter()
        response.getWriter().write(json);
    }

    /**
     * Injecte le modèle dans la requête et fait un FORWARD interne (pas un redirect :
     * l'URL du navigateur ne change pas). Grâce au mapping "/", le forward vers *.jsp est
     * traité par la JspServlet de Tomcat et ne repasse pas par le FrontController.
     */
    private void forwardToView(ModelView mv, String label, HttpServletRequest request,
                               HttpServletResponse response) throws IOException, ServletException {
        String viewName = mv.getViewName();
        if (viewName == null || viewName.isBlank()) {
            writeHtml(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "500 - Vue manquante",
                    "<p><code>" + escape(label) + "</code> a retourné un ModelView sans nom de vue.</p>");
            return;
        }

        String path = "/WEB-INF/views/" + viewName + ".jsp";

        if (getServletContext().getResource(path) == null) {
            writeHtml(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "500 - Vue introuvable",
                    "<p><code>" + escape(label) + "</code> demande la vue <code>" + escape(path)
                            + "</code>, qui n'existe pas.</p>");
            return;
        }

        mv.getData().forEach(request::setAttribute);
        request.getRequestDispatcher(path).forward(request, response);
    }

    // ------------------------------------------------------------------
    // Utilitaires d'affichage
    // ------------------------------------------------------------------
    private String renderRoutes() {
        StringBuilder sb = new StringBuilder("<ul>");
        for (UrlMethod key : routeTable.keySet()) {
            sb.append("<li>").append(escape(key.toString())).append("</li>");
        }
        for (UrlMethod key : dynamicRoutes.keySet()) {
            sb.append("<li>").append(escape(key.toString())).append(" (dynamique)</li>");
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