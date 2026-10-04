package com.app.controller;

import com.app.model.Eleve;
import com.miniframework.annotations.*;
import com.miniframework.model.ModelView;

@Controller
public class EleveController {

    @GetMapping("/eleve/form")
    public ModelView form() {
        return new ModelView("eleve-form");
    }

    @GetMapping("/eleve/vide")
    public void vide() {
        System.out.println("vide() appelée");
    }

    @GetMapping("/eleve/vue-inconnue")
    public ModelView vueInconnue() {
        return new ModelView("nexiste-pas");
    }

    @RequestMapping(value = "/eleve/legacy", method = "post")
    public String legacy() {
        return "Route déclarée avec method=\"post\"";
    }

    @GetMapping("/eleve/erreur")
    public String erreur() {
        throw new IllegalStateException("Erreur volontaire dans le contrôleur");
    }

    @PostMapping("/eleve/save")
    public ModelView sauvegarder(@Param("nom") String nom, @Param("age") Integer age) {
        Eleve eleve = new Eleve(nom, age);
        System.out.println("Sprint 5 -> " + eleve);

        ModelView mv = new ModelView("confirmation");
        mv.addObject("eleve", eleve);
        mv.addObject("message", "Élève enregistré avec succès !");
        return mv;
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