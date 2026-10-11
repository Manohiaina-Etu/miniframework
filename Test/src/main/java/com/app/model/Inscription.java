package com.app.model;

import java.util.List;

/** Objet de test pour le binding imbriqué : un objet, un sous-objet et une liste. */
public class Inscription {
    private Eleve eleve;
    private Adresse adresse;
    private List<Integer> notes;

    public Inscription() { } // obligatoire pour le model binding

    public Eleve getEleve() { return eleve; }
    public void setEleve(Eleve eleve) { this.eleve = eleve; }
    public Adresse getAdresse() { return adresse; }
    public void setAdresse(Adresse adresse) { this.adresse = adresse; }
    public List<Integer> getNotes() { return notes; }
    public void setNotes(List<Integer> notes) { this.notes = notes; }

    @Override
    public String toString() {
        return "Inscription{eleve=" + eleve + ", adresse=" + adresse + ", notes=" + notes + "}";
    }
}