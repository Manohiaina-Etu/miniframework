package com.app.controller;

import com.app.model.Eleve;
import com.app.model.Inscription;
import com.miniframework.annotations.*;
import com.miniframework.model.ModelView;
import com.miniframework.model.UploadedFile;
import com.miniframework.util.AppConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Controller
public class EleveController {

    private Path dossier = Path.of(AppConfig.get("upload.dir"));

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

    // ------------------------------------------------------------------
    // Extension : @Param sur listes et objets (préfixe HTTP)
    // ------------------------------------------------------------------

    @PostMapping("/eleve/liste")
    public String liste(@Param("eleves") List<String> eleves) {
        String r = "liste -> " + eleves;
        System.out.println(r);
        return r;
    }

    @PostMapping("/eleve/notes")
    public String notes(@Param("notes") List<List<Integer>> notes) {
        String r = "notes -> " + notes;
        System.out.println(r);
        return r;
    }

    @PostMapping("/eleve/save-param")
    public String sauvegarderParam(@Param("eleve") Eleve eleve) {
        String r = "param -> " + eleve;
        System.out.println(r);
        return r;
    }

    @PostMapping("/eleve/lot")
    public String lot(@Param("eleves") List<Eleve> eleves) {
        String r = "lot -> " + eleves;
        System.out.println(r);
        return r;
    }

    @PostMapping("/inscription/save")
    public String inscription(@Param("inscription") Inscription inscription) {
        String r = "inscription -> " + inscription;
        System.out.println(r);
        return r;
    }

    @GetMapping("/inscription/form")
    public ModelView inscriptionForm() {
        return new ModelView("inscription");
    }

    @PostMapping("/inscription/save_eleve")
    public String uploadFile(@Param("eleve") Eleve eleve, @Param("fichier") UploadedFile fichier) throws IOException {
        StringBuilder sb = new StringBuilder("nom=" + eleve.getNom() + " | age=" + eleve.getAge() + " | ");
        sb.append(fichier.getName()).append(" (").append(fichier.getMimeType())
                .append(", ").append(fichier.getSize()).append(" octets) ");
//        fichier.saveTo(Path.of("C:/uploads/photo-" + fichier.getName() + ".png"));
        fichier.saveTo(Path.of(dossier + "/" + fichier.getName()));
        return sb.toString();
    }

    @PostMapping("/inscription/save_eleve_files")
    public String uploadFiles(@Param("eleve") Eleve eleve, @Param("fichiers") List<UploadedFile> fichiers) throws IOException {
        StringBuilder sb = new StringBuilder("nom=" + eleve.getNom() + " | age=" + eleve.getAge() + " | ");
        if(!fichiers.isEmpty()) System.out.println("==>NBR_FILES::" + fichiers.size());
        else System.out.println("==>NBR_FILES:: EMPTY");
        for (UploadedFile f : fichiers) {
            sb.append(f.getName()).append(" (").append(f.getMimeType())
                    .append(", ").append(f.getSize()).append(" octets) ");
            f.saveTo(Path.of("C:/uploads/photo-" + f.getName()));
        }
        return sb.toString();
    }
}