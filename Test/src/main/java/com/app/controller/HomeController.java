package com.app.controller;

import com.miniframework.annotations.Controller;
import com.miniframework.annotations.RequestMapping;

@Controller
public class HomeController {

    @RequestMapping("/home")
    public void home() {
    }
}