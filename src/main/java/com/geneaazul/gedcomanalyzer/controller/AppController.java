package com.geneaazul.gedcomanalyzer.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
public class AppController {

    @Value("${project.version}")
    private String projectVersion;

    @GetMapping("/")
    public ModelAndView index() {
        Map<String, ?> params = Map.of("projectVersion", projectVersion);
        return new ModelAndView("index", params);
    }

    @GetMapping("/search-family/latest")
    public String searchFamily() {
        return "search-family/latest";
    }

}
