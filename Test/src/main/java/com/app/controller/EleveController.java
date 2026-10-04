package com.app.controller;

import com.app.model.Eleve;
import com.miniframework.annotations.*;

@Controller
public class EleveController {

    @RequestMapping(value = "/eleve/legacy", method = "post")
    public String legacy() {
        return "Route déclarée avec method=\"post\"";
    }

    @GetMapping("/eleve/erreur")
    public String erreur() {
        throw new IllegalStateException("Erreur volontaire dans le contrôleur");
    }

    @PostMapping("/eleve/save")
    public String sauvegarder(@Param("nom") String nom, @Param("age") Integer age) {
        String r = "4a -> nom=" + nom + ", age=" + age;
        System.out.println(r);
        return r;
    }

    @PostMapping("/eleve/save-model")
    public String sauvegarderModel(Eleve eleve) {
        String r = "4b -> " + eleve;
        System.out.println(r);
        return r;
    }

    @GetMapping("/eleve/{id}")
    public String detail(@PathVariable("id") Integer id) {
        String r = "4c -> id=" + id;
        System.out.println(r);
        return r;
    }
}