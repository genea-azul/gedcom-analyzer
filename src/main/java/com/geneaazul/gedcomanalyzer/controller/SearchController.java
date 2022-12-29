package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.SearchFamilyDto;
import com.geneaazul.gedcomanalyzer.model.SearchFamilyResultDto;
import com.geneaazul.gedcomanalyzer.service.FamilyService;
import com.geneaazul.gedcomanalyzer.service.GedcomParsingService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final FamilyService familyService;
    private final GedcomParsingService gedcomParsingService;
    private final GedcomAnalyzerProperties properties;

    @PostMapping("/family")
    public SearchFamilyResultDto searchFamily(@Valid @RequestBody SearchFamilyDto searchFamilyDto) throws IOException {
        if (properties.isStoreUploadedSearch()) {
            gedcomParsingService.storeSearch(searchFamilyDto);
        }
        return familyService.search(searchFamilyDto);
    }

}
