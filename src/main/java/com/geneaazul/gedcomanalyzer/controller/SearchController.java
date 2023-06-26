package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.FamilyTree;
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

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.util.InMemoryResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.annotation.Nullable;
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
    public SearchFamilyResultDto searchFamily(
            @Valid @RequestBody SearchFamilyDto searchFamilyDto,
            HttpServletRequest request) {
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

        log.info("Search family [ searchId={}, obfuscateLiving={}, peopleInResult={}, potentialResults={}, errors={}, httpRequestId={} ]",
                searchId.orElse(null),
                searchFamilyDto.getObfuscateLiving(),
                searchFamilyResult.getPeople().size(),
                searchFamilyResult.getPotentialResults(),
                searchFamilyResult.getErrors().size(),
                request.getRequestId());

        searchId
                .filter(id -> properties.isStoreFamilySearch())
                .ifPresent(id -> familyService.updateSearchIsMatch(id, searchFamilyResult.getPeople().size() > 0));

        return searchFamilyResult;
    }

    @GetMapping("/family/{searchId}/reviewed")
    public SearchFamilyDetailsDto markFamilyReviewed(@PathVariable Long searchId) {
        dockerService.startDbContainer();

        log.info("Mark family reviewed [ searchId={} ]", searchId);

        return familyService.updateSearchIsReviewed(searchId, Boolean.TRUE);
    }

    @GetMapping("/family/latest")
    public List<SearchFamilyDetailsDto> getLatest(
            @RequestParam @Nullable Boolean isMatch,
            @RequestParam @Nullable Boolean isReviewed,
            @RequestParam @Nullable Boolean hasContact,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        dockerService.startDbContainer();

        log.info("Search family latest [ isMatch={}, isReviewed={}, hasContact={}, page={}, size={} ]",
                isMatch, isReviewed, hasContact, page, size);

        String context = StringUtils.substringBefore(request.getRequestURL().toString(), "/api");
        return familyService.getLatest(isMatch, isReviewed, hasContact, page, size, context);
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

    @GetMapping("/family-tree/{personUuid}/plain")
    public ResponseEntity<Resource> getPlainFamilyTree(
            @PathVariable UUID personUuid,
            @RequestParam(defaultValue = "true") Boolean obfuscateLiving,
            HttpServletRequest request) throws IOException {

        Optional<FamilyTree> maybeFamilyTree = personService.getFamilyTree(personUuid, Boolean.TRUE.equals(obfuscateLiving));

        if (maybeFamilyTree.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new InMemoryResource("Invalid person!"));
        }

        FamilyTree familyTree = maybeFamilyTree.get();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + familyTree.filename());
        headers.add(HttpHeaders.CONTENT_LANGUAGE, familyTree.locale().toString());
        headers.add("File-Name", familyTree.filename());

        log.info("Search family tree [ personUuid={}, personId={}, obfuscateLiving={}, httpRequestId={} ]",
                personUuid,
                familyTree.person().getId(),
                obfuscateLiving,
                request.getRequestId());

        PathResource resource = new PathResource(familyTree.path());

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(resource.contentLength())
                .contentType(familyTree.mediaType())
                .body(resource);
    }

}
