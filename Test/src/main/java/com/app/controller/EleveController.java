package com.app.controller;

import com.miniframework.annotations.Controller;
import com.miniframework.annotations.RequestMapping;

@Controller
public class EleveController {

    @RequestMapping("/eleve/form")
    public void formulaire() {
    }

    @RequestMapping(value = "/eleve/liste", method = "GET")
    public void liste() {
    }

    @RequestMapping(value = "/eleve/liste", method = "POST")
    public void poster() {
    }
}