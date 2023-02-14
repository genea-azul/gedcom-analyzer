package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDetailsDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyResultDto;
import com.geneaazul.gedcomanalyzer.service.FamilyService;
import com.geneaazul.gedcomanalyzer.utils.InetAddressUtils;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final FamilyService familyService;
    private final GedcomAnalyzerProperties properties;

    @PostMapping("/family")
    public SearchFamilyResultDto searchFamily(@Valid @RequestBody SearchFamilyDto searchFamilyDto, HttpServletRequest request) {
        String clientIpAddress = InetAddressUtils.getRemoteAddress(request);

        if (!familyService.isAllowedSearch(clientIpAddress)) {
            return SearchFamilyResultDto.builder()
                    .errors(List.of("TOO-MANY-REQUESTS"))
                    .build();
        }

        Optional<Long> searchId = Optional.empty();

        if (properties.isStoreFamilySearch()) {
            searchId = familyService.persistSearch(searchFamilyDto, clientIpAddress);
        }

        SearchFamilyResultDto searchFamilyResult = familyService.search(searchFamilyDto);

        searchId
                .filter(id -> properties.isStoreFamilySearch())
                .ifPresent(id -> familyService.updateSearch(id, searchFamilyResult.getPeople().size() > 0));

        return searchFamilyResult;
    }

    @GetMapping("/family/latest")
    public List<SearchFamilyDetailsDto> getLatest(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return familyService.getLatest(page, size);
    }

}
