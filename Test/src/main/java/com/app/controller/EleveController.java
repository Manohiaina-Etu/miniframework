package com.app.controller;

import com.miniframework.annotations.Controller;
import com.miniframework.annotations.GetMapping;
import com.miniframework.annotations.PostMapping;
import com.miniframework.annotations.RequestMapping;

@Controller
public class EleveController {

    @GetMapping("/eleve/form")
    public String form() {
        return "Formulaire élève (GET)";
    }

    @PostMapping("/eleve/save")
    public String sauvegarder() {
        return "Élève enregistré (POST, sans paramètres pour l'instant)";
    }

    @RequestMapping(value = "/eleve/legacy", method = "post")
    public String legacy() {
        return "Route déclarée avec method=\"post\"";
    }

    @GetMapping("/eleve/erreur")
    public String erreur() {
        throw new IllegalStateException("Erreur volontaire dans le contrôleur");
    }
}