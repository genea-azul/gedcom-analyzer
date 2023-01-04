package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyResultDto;
import com.geneaazul.gedcomanalyzer.service.FamilyService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final FamilyService familyService;
    private final GedcomAnalyzerProperties properties;

    @PostMapping("/family")
    public SearchFamilyResultDto searchFamily(@Valid @RequestBody SearchFamilyDto searchFamilyDto) {
        Optional<Long> searchId = Optional.empty();

        if (properties.isStoreFamilySearch()) {
            searchId = familyService.persistSearch(searchFamilyDto);
        }

        SearchFamilyResultDto searchFamilyResult = familyService.search(searchFamilyDto);

        searchId
                .filter(id -> properties.isStoreFamilySearch())
                .ifPresent(id -> familyService.updateSearch(id, searchFamilyResult.getPeople().size() > 0));

        return searchFamilyResult;
    }

}
