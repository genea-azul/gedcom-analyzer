package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.dto.GedcomMetadataDto;
import com.geneaazul.gedcomanalyzer.service.GedcomAnalyzerService;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/gedcom-analyzer")
@RequiredArgsConstructor
public class GedcomAnalyzerController {

    private final GedcomAnalyzerService gedcomAnalyzerService;
    private final GedcomHolder gedcomHolder;
    private final GedcomAnalyzerProperties properties;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @Value("${project.version}")
    private String projectVersion;

    @GetMapping
    @CrossOrigin(originPatterns = {
            "http://geneaazul.com.ar:[*]",
            "https://geneaazul.com.ar:[*]",
            "http://*.geneaazul.com.ar:[*]",
            "https://*.geneaazul.com.ar:[*]",
    })
    public Map<String, Object> analyzeGedcom(HttpServletRequest request) {
        log.info("Accessing the API [ httpRequestId={} ]", request.getRequestId());
        return Map.of(
                "env", activeProfiles,
                "version", projectVersion,
                "disableObfuscateLiving", properties.isDisableObfuscateLiving());
    }

    @GetMapping("/health")
    public Map<String, Object> healthCheck(HttpServletRequest request) {
        log.debug("Health check [ httpRequestId={} ]", request.getRequestId());
        return Map.of("status", "ok");
    }

    @GetMapping("/metadata")
    @CrossOrigin(originPatterns = {
            "http://geneaazul.com.ar:[*]",
            "https://geneaazul.com.ar:[*]",
            "http://*.geneaazul.com.ar:[*]",
            "https://*.geneaazul.com.ar:[*]",
    })
    public GedcomMetadataDto getGedcomMetadata() {
        return gedcomAnalyzerService.getGedcomMetadata(gedcomHolder.getGedcom());
    }

}
