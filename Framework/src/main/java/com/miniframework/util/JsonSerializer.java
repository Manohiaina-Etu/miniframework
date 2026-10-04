package com.miniframework.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sérialiseur JSON minimal, par réflexion, sans dépendance externe.
 */
public final class JsonSerializer {

    private JsonSerializer() {
    }

    private record Property(String name, Method getter) {
    }

    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        Set<Object> enCours = Collections.newSetFromMap(new IdentityHashMap<>());
        write(value, sb, enCours);
        return sb.toString();
    }

    private static void write(Object v, StringBuilder sb, Set<Object> enCours) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof CharSequence || v instanceof Character || v instanceof TemporalAccessor) {
            writeString(String.valueOf(v), sb);
        } else if (v instanceof Enum<?> e) {
            writeString(e.name(), sb);
        } else if (v instanceof Boolean b) {
            sb.append(b.booleanValue());
        } else if (v instanceof Number n) {
            // NaN et Infinity n'existent pas en JSON
            boolean nonFini = (v instanceof Double d && (d.isNaN() || d.isInfinite()))
                    || (v instanceof Float f && (f.isNaN() || f.isInfinite()));
            sb.append(nonFini ? "null" : n.toString());
        } else {
            if (!enCours.add(v)) {
                throw new IllegalStateException("Cycle détecté pendant la sérialisation JSON : un objet de type "
                        + v.getClass().getName() + " se contient lui-même (directement ou indirectement).");
            }
            try {
                writeContainerOrBean(v, sb, enCours);
            } finally {
                enCours.remove(v);
            }
        }
    }

    private static void writeContainerOrBean(Object v, StringBuilder sb, Set<Object> enCours) {
        if (v instanceof Map<?, ?> map) {
            sb.append('{');
            boolean premier = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!premier) sb.append(',');
                premier = false;
                writeString(String.valueOf(entry.getKey()), sb);
                sb.append(':');
                write(entry.getValue(), sb, enCours);
            }
            sb.append('}');
        } else if (v instanceof Iterable<?> iterable) {
            sb.append('[');
            boolean premier = true;
            for (Object element : iterable) {
                if (!premier) sb.append(',');
                premier = false;
                write(element, sb, enCours);
            }
            sb.append(']');
        } else if (v.getClass().isArray()) {
            int n = java.lang.reflect.Array.getLength(v);
            sb.append('[');
            for (int i = 0; i < n; i++) {
                if (i > 0) sb.append(',');
                write(java.lang.reflect.Array.get(v, i), sb, enCours);
            }
            sb.append(']');
        } else {
            writeBean(v, sb, enCours);
        }
    }

    /** Objet Java : une propriété JSON par getter public (ou composant, pour un record). */
    private static void writeBean(Object bean, StringBuilder sb, Set<Object> enCours) {
        sb.append('{');
        boolean premier = true;
        for (Property p : collectProperties(bean.getClass())) {
            Object valeur;
            try {
                valeur = p.getter().invoke(bean);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new IllegalStateException(p.getter().getName() + "() de " + bean.getClass().getName()
                        + " a levé " + cause.getClass().getName() + " : " + cause.getMessage());
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Accès refusé à " + p.getter().getName() + "() de "
                        + bean.getClass().getName() + " (la classe et le getter doivent être public).");
            }
            if (!premier) sb.append(',');
            premier = false;
            writeString(p.name(), sb);
            sb.append(':');
            write(valeur, sb, enCours);
        }
        sb.append('}');
    }

    private static List<Property> collectProperties(Class<?> type) {
        List<Property> props = new ArrayList<>();

        if (type.isRecord()) {
            for (RecordComponent rc : type.getRecordComponents()) {
                props.add(new Property(rc.getName(), rc.getAccessor()));
            }
            return props;
        }

        for (Method m : type.getMethods()) {
            String nom = m.getName();
            if (Modifier.isStatic(m.getModifiers()) || m.isBridge() || m.getParameterCount() != 0
                    || m.getReturnType() == void.class || nom.equals("getClass")) {
                continue;
            }
            String propriete = null;
            if (nom.startsWith("get") && nom.length() > 3) {
                propriete = nom.substring(3);
            } else if (nom.startsWith("is") && nom.length() > 2
                    && (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class)) {
                propriete = nom.substring(2);
            }
            if (propriete == null) continue;

            String cle = Character.toLowerCase(propriete.charAt(0)) + propriete.substring(1);
            props.add(new Property(cle, m));
        }
        props.sort(Comparator.comparing(Property::name));
        return props;
    }

    /** Écrit une chaîne JSON avec échappement. Les accents restent tels quels (la réponse est en UTF-8). */
    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    // Les caractères de contrôle (< 0x20) sont interdits bruts en JSON
                    if (c < 0x20 || c == '\u2028' || c == '\u2029') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}