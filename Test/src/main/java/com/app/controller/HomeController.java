package com.app.controller;

import com.miniframework.annotations.Controller;
import com.miniframework.annotations.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "Accueil";
    }

    @GetMapping("/home")
    public String home() {
        return "Page home";
    }
}