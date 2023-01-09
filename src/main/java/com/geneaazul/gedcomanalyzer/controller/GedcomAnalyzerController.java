package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.dto.GedcomAnalysisDto;
import com.geneaazul.gedcomanalyzer.service.GedcomAnalyzerService;
import com.geneaazul.gedcomanalyzer.service.GedcomParsingService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXParseException;

import java.io.IOException;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/gedcom-analyzer")
@RequiredArgsConstructor
public class GedcomAnalyzerController {

    private final GedcomParsingService gedcomParsingService;
    private final GedcomAnalyzerService gedcomAnalyzerService;

    @GetMapping
    public void analyzeGedcom() {
        // Used for health check
    }

    @PostMapping
    public GedcomAnalysisDto analyzeGedcom(@RequestPart MultipartFile gedcomFile) throws SAXParseException, IOException {
        EnrichedGedcom gedcom = gedcomParsingService.parse(gedcomFile);
        return gedcomAnalyzerService.analyze(gedcom);
    }

}
