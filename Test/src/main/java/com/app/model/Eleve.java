package com.app.model;

public class Eleve {
    private String nom;
    private Integer age;

    public Eleve() { } // obligatoire pour le model binding

    public Eleve(String nom, Integer age) {
        this.nom = nom;
        this.age = age;
    }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    @Override
    public String toString() { return "Eleve{nom=" + nom + ", age=" + age + "}"; }
}