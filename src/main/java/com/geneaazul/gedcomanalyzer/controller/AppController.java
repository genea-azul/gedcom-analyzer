package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.service.DockerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AppController {

    private final DockerService dockerService;

    @Value("${project.version}")
    private String projectVersion;

    @GetMapping("/")
    public ModelAndView index() {
        dockerService.startDbContainer();
        Map<String, ?> params = Map.of("projectVersion", projectVersion);
        return new ModelAndView("index", params);
    }

    @GetMapping("/search-family/latest")
    public String searchFamily() {
        dockerService.startDbContainer();
        return "search-family/latest";
    }

}
