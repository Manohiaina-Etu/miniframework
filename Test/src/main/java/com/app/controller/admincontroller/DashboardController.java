package com.app.controller.admincontroller;

import com.miniframework.annotations.Controller;
import com.miniframework.annotations.RequestMapping;

@Controller
public class DashboardController {

    @RequestMapping("/admin")
    public void index() {
    }

    @RequestMapping(value = "/admin/liste", method = "GET")
    public void liste() {
    }

    @RequestMapping(value = "/admin/liste", method = "POST")
    public void poster() {
    }
}