package com.app.controller;

import com.miniframework.annotations.Controller;
import com.miniframework.annotations.GetMapping;
import com.miniframework.model.ModelView;

@Controller
public class HomeController {

    @GetMapping("/")
    public ModelView index() {
        ModelView mv = new ModelView("home");
        mv.addObject("message", "Bienvenue sur le Mini Framework MVC");
        return mv;
    }

    @GetMapping("/home")
    public ModelView home() {
        ModelView mv = new ModelView("home");
        mv.addObject("message", "Page /home");
        return mv;
    }
}