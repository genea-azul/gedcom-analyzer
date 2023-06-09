package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.FamilyTree;
import com.geneaazul.gedcomanalyzer.model.dto.FamilyTreeDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDetailsDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnameResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnamesDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnamesResultDto;
import com.geneaazul.gedcomanalyzer.service.DockerService;
import com.geneaazul.gedcomanalyzer.service.FamilyService;
import com.geneaazul.gedcomanalyzer.service.PersonService;
import com.geneaazul.gedcomanalyzer.service.SurnameService;
import com.geneaazul.gedcomanalyzer.utils.InetAddressUtils;

import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.util.InMemoryResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
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
    private final PersonService personService;
    private final GedcomAnalyzerProperties properties;
    private final DockerService dockerService;

    @PostMapping("/family")
    public SearchFamilyResultDto searchFamily(@Valid @RequestBody SearchFamilyDto searchFamilyDto, HttpServletRequest request) {
        dockerService.startDbContainer();

        Optional<String> clientIpAddress = InetAddressUtils.getRemoteAddress(request);

        if (!clientIpAddress
                .map(familyService::isAllowedSearch)
                .orElse(true)) {
            return SearchFamilyResultDto.builder()
                    .errors(List.of("TOO-MANY-REQUESTS"))
                    .build();
        }

        Optional<Long> searchId = Optional.empty();

        if (properties.isStoreFamilySearch()) {
            searchId = familyService.persistSearch(searchFamilyDto, clientIpAddress.orElse(null));
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

        Optional<String> clientIpAddress = InetAddressUtils.getRemoteAddress(request);

        if (!clientIpAddress
                .map(familyService::isAllowedSearch)
                .orElse(true)) {
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

    @PostMapping("/family-tree/plain")
    public ResponseEntity<Resource> getPlainFamilyTree(
            @Valid @RequestBody FamilyTreeDto familyTreeDto,
            HttpServletRequest request) throws IOException {

        Optional<FamilyTree> maybeFamilyTree = personService.getFamilyTree(familyTreeDto.getPersonUuid(), true);

        if (maybeFamilyTree.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new InMemoryResource("Invalid person!"));
        }

        FamilyTree familyTree = maybeFamilyTree.get();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + familyTree.filename());
        headers.add(HttpHeaders.CONTENT_LANGUAGE, familyTree.locale().toString());
        headers.add("File-Name", familyTree.filename());

        log.info("Search family tree [ personUuid={}, personId={}, httpRequestId={} ]",
                familyTreeDto.getPersonUuid(),
                familyTree.person().getId(),
                request.getRequestId());

        PathResource resource = new PathResource(familyTree.path());

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(Files.size(familyTree.path()))
                .contentType(familyTree.mediaType())
                .body(resource);
    }

}
