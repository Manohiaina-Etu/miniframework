package com.app.controller;

import com.app.model.Eleve;
import com.miniframework.annotations.GetMapping;
import com.miniframework.annotations.PathVariable;
import com.miniframework.annotations.RestController;
import com.miniframework.model.ModelView;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ApiEleveController {

    private final List<Eleve> eleves = List.of(
            new Eleve("Rakoto", 18),
            new Eleve("Rabe", 20),
            new Eleve("Éloïse", 19));

    @GetMapping("/api/eleves")
    public List<Eleve> liste() {
        return eleves;
    }

    @GetMapping("/api/eleves/stats")
    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", eleves.size());
        m.put("ageMoyen", eleves.stream().mapToInt(Eleve::getAge).average().orElse(0));
        return m;
    }

    @GetMapping("/api/eleves/{id}")
    public Eleve detail(@PathVariable("id") Integer id) {
        if (id == null || id < 1 || id > eleves.size()) {
            return null;
        }
        return eleves.get(id - 1);
    }

    @GetMapping("/api/message")
    public String message() {
        return "Éloïse dit \"bonjour\" à l'équipe";
    }

    @GetMapping("/api/cycle")
    public Map<String, Object> cycle() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("self", m);
        return m;
    }

    @GetMapping("/api/mv")
    public ModelView mv() {
        return new ModelView("home");
    }
}