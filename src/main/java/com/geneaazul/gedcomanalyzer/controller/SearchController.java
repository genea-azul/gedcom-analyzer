package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDetailsDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnameResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnamesDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnamesResultDto;
import com.geneaazul.gedcomanalyzer.service.DockerService;
import com.geneaazul.gedcomanalyzer.service.FamilyService;
import com.geneaazul.gedcomanalyzer.service.SurnameService;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final FamilyService familyService;
    private final SurnameService surnameService;
    private final GedcomAnalyzerProperties properties;
    private final DockerService dockerService;

    @PostMapping("/family")
    public SearchFamilyResultDto searchFamily(@Valid @RequestBody SearchFamilyDto searchFamilyDto, HttpServletRequest request) {
        dockerService.startDbContainer();

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

        log.info("Search family [ id={}, peopleInResult={}, potentialResults={}, errors={}, httpRequestId={} ]",
                searchId.orElse(null),
                searchFamilyResult.getPeople().size(),
                searchFamilyResult.getPotentialResults(),
                searchFamilyResult.getErrors().size(),
                request.getRequestId());

        searchId
                .filter(id -> properties.isStoreFamilySearch())
                .ifPresent(id -> familyService.updateSearch(id, searchFamilyResult.getPeople().size() > 0));

        return searchFamilyResult;
    }

    @GetMapping("/family/latest")
    public List<SearchFamilyDetailsDto> getLatest(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        dockerService.startDbContainer();
        return familyService.getLatest(page, size);
    }

    @PostMapping("/surnames")
    public SearchSurnamesResultDto searchSurnames(@Valid @RequestBody SearchSurnamesDto searchSurnamesDto, HttpServletRequest request) {
        dockerService.startDbContainer();

        String clientIpAddress = InetAddressUtils.getRemoteAddress(request);

        if (!familyService.isAllowedSearch(clientIpAddress)) {
            return SearchSurnamesResultDto.builder()
                    .build();
        }

        SearchSurnamesResultDto searchSurnamesResult = surnameService.search(searchSurnamesDto);

        log.info("Search surnames [ surnamesInResult={}, totalFrequency={}, httpRequestId={} ]",
                searchSurnamesResult.getSurnames().size(),
                searchSurnamesResult.getSurnames()
                        .stream()
                        .mapToInt(SearchSurnameResultDto::getFrequency)
                        .sum(),
                request.getRequestId());

        return searchSurnamesResult;
    }

}
