package com.geneaazul.gedcomanalyzer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AppController {

    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

    @GetMapping("/search-family/latest")
    public String searchFamily(Model model) {
        return "search-family/latest";
    }

}
