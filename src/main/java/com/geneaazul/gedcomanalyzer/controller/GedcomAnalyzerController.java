package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.dto.GedcomAnalysisDto;
import com.geneaazul.gedcomanalyzer.model.dto.GedcomMetadataDto;
import com.geneaazul.gedcomanalyzer.service.GedcomAnalyzerService;
import com.geneaazul.gedcomanalyzer.service.GedcomParsingService;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/gedcom-analyzer")
@RequiredArgsConstructor
public class GedcomAnalyzerController {

    private final GedcomParsingService gedcomParsingService;
    private final GedcomAnalyzerService gedcomAnalyzerService;
    private final GedcomHolder gedcomHolder;
    private final GedcomAnalyzerProperties properties;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @Value("${project.version}")
    private String projectVersion;

    @GetMapping
    @CrossOrigin(originPatterns = { "http://localhost:[*]", "https://localhost:[*]", "http://geneaazul.com.ar:[*]", "https://geneaazul.com.ar:[*]" })
    public Map<String, Object> analyzeGedcom() {
        // Used for health check
        return Map.of(
                "env", activeProfiles,
                "version", projectVersion,
                "disableObfuscateLiving", properties.isDisableObfuscateLiving());
    }

    @GetMapping("/metadata")
    @CrossOrigin(originPatterns = { "http://localhost:[*]", "https://localhost:[*]", "http://geneaazul.com.ar:[*]", "https://geneaazul.com.ar:[*]" })
    public GedcomMetadataDto getGedcomMetadata() {
        return gedcomAnalyzerService.getGedcomMetadata(gedcomHolder.getGedcom());
    }

    @GetMapping("/reload")
    public GedcomMetadataDto reloadAndGetGedcomMetadata() {
        Instant startInstant = Instant.now();
        gedcomHolder.reloadFromStorage(true);
        Duration reloadDuration = Duration.between(startInstant, Instant.now());
        return gedcomAnalyzerService.getGedcomMetadata(gedcomHolder.getGedcom(), reloadDuration);
    }

    @PostMapping
    public GedcomAnalysisDto analyzeGedcom(@RequestPart MultipartFile gedcomFile) throws SAXParseException, IOException {
        EnrichedGedcom gedcom = gedcomParsingService.parse(gedcomFile);
        return gedcomAnalyzerService.analyze(gedcom);
    }

}
