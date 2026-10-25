package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FamilyTree;
import com.geneaazul.gedcomanalyzer.model.FamilyTreeType;
import com.geneaazul.gedcomanalyzer.model.dto.SearchConnectionDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchConnectionResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnameResultDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnamesDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchSurnamesResultDto;
import com.geneaazul.gedcomanalyzer.service.ClientRequestRateLimiter;
import com.geneaazul.gedcomanalyzer.service.ConnectionService;
import com.geneaazul.gedcomanalyzer.service.FamilyService;
import com.geneaazul.gedcomanalyzer.service.SurnameService;
import com.geneaazul.gedcomanalyzer.service.familytree.FamilyTreeManager;
import com.geneaazul.gedcomanalyzer.service.familytree.FamilyTreeService;
import com.geneaazul.gedcomanalyzer.service.familytree.GraphJsonFamilyTreeService;
import com.geneaazul.gedcomanalyzer.service.familytree.PlainFamilyTreePdfService;
import com.geneaazul.gedcomanalyzer.utils.InetAddressUtils;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.util.InMemoryResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.time.Duration;
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
    private final ConnectionService connectionService;
    private final GedcomAnalyzerProperties properties;
    private final FamilyTreeManager familyTreeManager;
    private final PlainFamilyTreePdfService plainFamilyTreePdfService;
    private final GraphJsonFamilyTreeService graphJsonFamilyTreeService;
    private final ClientRequestRateLimiter clientRequestRateLimiter;

    @PostMapping("/family")
    @GeneaAzulCrossOrigin
    public SearchFamilyResultDto searchFamily(
            @Valid @RequestBody SearchFamilyDto searchFamilyDto,
            HttpServletRequest request) {

        Optional<String> clientIpAddress = InetAddressUtils.getRemoteAddress(request);

        boolean obfuscateLiving = properties.isObfuscateLiving();
        // default: true
        boolean onlySecondaryDescription = BooleanUtils.isNotFalse(searchFamilyDto.getOnlySecondaryDescription());

        boolean shouldPersist = properties.isStoreFamilySearch() && BooleanUtils.isNotFalse(searchFamilyDto.getPersist());

        // Checked before storing: rejected requests must not add rows (or a flood would grow the DB without bound)
        boolean allowed = clientIpAddress
                .map(ip -> familyService.isAllowedSearch(ip, false)
                        // Non-persisted searches (tree-builder live lookup) add nothing to the DB-based
                        // counter, so they are also counted in memory
                        && (shouldPersist || clientRequestRateLimiter.tryAcquire(
                                "family-lookup:" + ip,
                                properties.getMaxClientLookupRequestsCountThreshold(),
                                Duration.ofHours(properties.getMaxClientRequestsHoursThreshold()))))
                .orElse(true);
        if (!allowed) {
            log.info("Search family rejected: too many requests [ httpRequestId={} ]", request.getRequestId());
            return SearchFamilyResultDto.builder()
                    .errors(List.of("TOO-MANY-REQUESTS"))
                    .build();
        }

        Optional<Long> searchId = shouldPersist
                ? familyService.persistFamilySearch(searchFamilyDto, obfuscateLiving, clientIpAddress.orElse(null))
                : Optional.empty();

        SearchFamilyResultDto searchFamilyResult = familyService.search(searchFamilyDto);

        log.info("Search family [ searchId={}, obfuscateLiving={}, onlySecondaryDescription={}, peopleInResult={}, potentialResults={}, errors={}, httpRequestId={} ]",
                searchId.orElse(null),
                obfuscateLiving,
                onlySecondaryDescription,
                searchFamilyResult.getPeople().size(),
                searchFamilyResult.getPotentialResults(),
                searchFamilyResult.getErrors().size(),
                request.getRequestId());

        updateSearchResult(searchId, searchFamilyResult);

        // Pre-generate only the cheap graph JSON (2D/3D viewers); PDFs are generated when downloaded
        familyTreeManager.queueFamilyTreeGeneration(
                searchFamilyResult.getPeople(),
                obfuscateLiving,
                onlySecondaryDescription,
                List.of(FamilyTreeType.GRAPH_JSON));

        return searchFamilyResult;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void updateSearchResult(Optional<Long> searchId, SearchFamilyResultDto searchFamilyResult) {
        searchId
                .filter(_ -> properties.isStoreFamilySearch())
                .ifPresent(id -> familyService.updateFamilySearchResult(
                        id,
                        !searchFamilyResult.getPeople().isEmpty(),
                        searchFamilyResult.getPotentialResults(),
                        StringUtils.join(searchFamilyResult.getErrors(), " | ")));
    }

    @PostMapping("/surnames")
    @GeneaAzulCrossOrigin
    public SearchSurnamesResultDto searchSurnames(@Valid @RequestBody SearchSurnamesDto searchSurnamesDto, HttpServletRequest request) {

        Optional<String> clientIpAddress = InetAddressUtils.getRemoteAddress(request);

        if (!clientIpAddress
                .map(familyService::isAllowedSearch)
                .orElse(true)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
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

    @GetMapping("/family-tree/{personUuid}/plainPdf")
    @GeneaAzulCrossOrigin(exposedHeaders = { "Content-Disposition", "File-Name" })
    public ResponseEntity<Resource> getPlainFamilyTreePdf(
            @PathVariable UUID personUuid,
            @RequestParam @Nullable Boolean onlySecondaryDescription,
            HttpServletRequest request) throws IOException {
        return serveFamilyTree(plainFamilyTreePdfService, "Plain family tree", personUuid, onlySecondaryDescription, request);
    }

    @GetMapping("/family-tree/{personUuid}/graphJson")
    @GeneaAzulCrossOrigin
    public ResponseEntity<Resource> getGraphFamilyTreeJson(
            @PathVariable UUID personUuid,
            @RequestParam @Nullable Boolean onlySecondaryDescription,
            HttpServletRequest request) throws IOException {
        return serveFamilyTree(graphJsonFamilyTreeService, "Graph JSON family tree", personUuid, onlySecondaryDescription, request);
    }

    private ResponseEntity<Resource> serveFamilyTree(
            FamilyTreeService familyTreeService,
            String logLabel,
            UUID personUuid,
            @Nullable Boolean onlySecondaryDescription,
            HttpServletRequest request) throws IOException {

        // Each export is a full traversal plus file generation, and the trees include living people
        boolean allowed = InetAddressUtils.getRemoteAddress(request)
                .map(ip -> clientRequestRateLimiter.tryAcquire(
                        "family-tree:" + ip,
                        properties.getMaxClientFamilyTreeRequestsCountThreshold(),
                        Duration.ofHours(properties.getMaxClientRequestsHoursThreshold())))
                .orElse(true);
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        }

        boolean obfuscateLiving = properties.isObfuscateLiving();
        // default: true
        boolean onlySecondaryDescriptionEnabled = BooleanUtils.isNotFalse(onlySecondaryDescription);

        Optional<FamilyTree> maybeFamilyTree = familyTreeService
                .getFamilyTree(
                        personUuid,
                        obfuscateLiving,
                        onlySecondaryDescriptionEnabled);

        log.info("{} [ personUuid={}, personId={}, obfuscateLiving={}, onlySecondaryDescription={}, httpRequestId={} ]",
                logLabel,
                personUuid,
                maybeFamilyTree
                        .map(FamilyTree::person)
                        .map(EnrichedPerson::getId)
                        .orElse(null),
                obfuscateLiving,
                onlySecondaryDescriptionEnabled,
                request.getRequestId());

        if (maybeFamilyTree.isEmpty()) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body(new InMemoryResource("<h4>Identificador de persona inv&aacute;lido.</h4>"
                            + "<p>Por favor realiz&aacute; una nueva b&uacute;squeda.</p>"));
        }

        FamilyTree familyTree = maybeFamilyTree.get();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                        .filename(familyTree.filename())
                        .build()
                        .toString());
        headers.add(HttpHeaders.CONTENT_LANGUAGE, familyTree.locale().toString());
        headers.add("File-Name", familyTree.filename());

        FileSystemResource resource = new FileSystemResource(familyTree.path());

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(resource.contentLength())
                .contentType(familyTree.mediaType())
                .body(resource);
    }

    @PostMapping("/connection")
    @GeneaAzulCrossOrigin
    public SearchConnectionResultDto searchConnection(
            @Valid @RequestBody SearchConnectionDto searchConnectionDto,
            HttpServletRequest request) {

        Optional<String> clientIpAddress = InetAddressUtils.getRemoteAddress(request);

        if (!clientIpAddress
                .map(ip -> familyService.isAllowedSearch(ip, false))
                .orElse(true)) {
            log.info("Search connection rejected: too many requests [ httpRequestId={} ]", request.getRequestId());
            return SearchConnectionResultDto.builder()
                    .errors(List.of("TOO-MANY-REQUESTS"))
                    .build();
        }

        Optional<Long> searchId = properties.isStoreConnectionSearch()
                ? connectionService.persistConnectionSearch(searchConnectionDto, clientIpAddress.orElse(null))
                : Optional.empty();

        SearchConnectionResultDto searchConnectionResult = connectionService.search(searchConnectionDto);

        log.info("Search connection [ searchId={}, connectionsInResult={}, errors={}, httpRequestId={} ]",
                searchId.orElse(null),
                searchConnectionResult.getConnections().size(),
                searchConnectionResult.getErrors().size(),
                request.getRequestId());

        updateSearchResult(searchId, searchConnectionResult);

        return searchConnectionResult;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void updateSearchResult(Optional<Long> searchId, SearchConnectionResultDto searchConnectionResult) {
        searchId
                .filter(_ -> properties.isStoreConnectionSearch())
                .ifPresent(id -> {
                    boolean isMatch = !searchConnectionResult.getConnections().isEmpty();
                    @Nullable Integer distance = isMatch
                            ? searchConnectionResult.getConnections().size() - 1
                            : null;
                    connectionService.updateConnectionSearchResult(
                            id,
                            isMatch,
                            distance,
                            StringUtils.join(searchConnectionResult.getErrors(), " | "));
                });
    }

}
