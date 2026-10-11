package com.app.model;

public class Adresse {
    private String rue;
    private String ville;

    public Adresse() { } // obligatoire pour le model binding

    public String getRue() { return rue; }
    public void setRue(String rue) { this.rue = rue; }
    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    @Override
    public String toString() { return "Adresse{rue=" + rue + ", ville=" + ville + "}"; }
}