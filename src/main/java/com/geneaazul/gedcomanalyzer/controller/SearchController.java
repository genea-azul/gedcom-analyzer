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
import com.geneaazul.gedcomanalyzer.service.ConnectionService;
import com.geneaazul.gedcomanalyzer.service.FamilyService;
import com.geneaazul.gedcomanalyzer.service.SurnameService;
import com.geneaazul.gedcomanalyzer.service.familytree.FamilyTreeManager;
import com.geneaazul.gedcomanalyzer.service.familytree.GraphJsonFamilyTreeService;
import com.geneaazul.gedcomanalyzer.service.familytree.PlainFamilyTreePdfService;
import com.geneaazul.gedcomanalyzer.utils.InetAddressUtils;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.util.InMemoryResource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

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
    private final ConnectionService connectionService;
    private final GedcomAnalyzerProperties properties;
    private final FamilyTreeManager familyTreeManager;
    private final PlainFamilyTreePdfService plainFamilyTreePdfService;
    private final GraphJsonFamilyTreeService graphJsonFamilyTreeService;

    @PostMapping("/family")
    @CrossOrigin(originPatterns = { "http://geneaazul.com.ar:[*]", "https://geneaazul.com.ar:[*]", "http://*.geneaazul.com.ar:[*]", "https://*.geneaazul.com.ar:[*]" })
    public SearchFamilyResultDto searchFamily(
            @Valid @RequestBody SearchFamilyDto searchFamilyDto,
            HttpServletRequest request) {

        Optional<String> clientIpAddress = InetAddressUtils.getRemoteAddress(request);

        // default: true
        boolean obfuscateLiving = !properties.isDisableObfuscateLiving()
                && BooleanUtils.isNotFalse(searchFamilyDto.getObfuscateLiving());
        // default: true
        boolean onlySecondaryDescription = BooleanUtils.isNotFalse(searchFamilyDto.getOnlySecondaryDescription());
        // default: false
        boolean forceRewrite = BooleanUtils.isTrue(searchFamilyDto.getIsForceRewrite());

        Optional<Long> searchId = Optional.empty();

        boolean shouldPersist = properties.isStoreFamilySearch() && BooleanUtils.isNotFalse(searchFamilyDto.getPersist());

        if (shouldPersist) {
            searchId = familyService.persistFamilySearch(searchFamilyDto, obfuscateLiving, clientIpAddress.orElse(null));
        }

        if (shouldPersist && !clientIpAddress
                .map(familyService::isAllowedSearch)
                .orElse(true)) {
            SearchFamilyResultDto searchFamilyResult =  SearchFamilyResultDto.builder()
                    .errors(List.of("TOO-MANY-REQUESTS"))
                    .build();

            updateSearchResult(searchId, searchFamilyResult);

            return searchFamilyResult;
        }

        SearchFamilyResultDto searchFamilyResult = familyService.search(searchFamilyDto);

        log.info("Search family [ searchId={}, obfuscateLiving={}, onlySecondaryDescription={}, forceRewrite={}, peopleInResult={}, potentialResults={}, errors={}, httpRequestId={} ]",
                searchId.orElse(null),
                obfuscateLiving,
                onlySecondaryDescription,
                forceRewrite,
                searchFamilyResult.getPeople().size(),
                searchFamilyResult.getPotentialResults(),
                searchFamilyResult.getErrors().size(),
                request.getRequestId());

        updateSearchResult(searchId, searchFamilyResult);

        // Queue PDF Family Tree, HTML Pyvis Network and graph JSON generation
        familyTreeManager.queueFamilyTreeGeneration(
                searchFamilyResult.getPeople(),
                obfuscateLiving,
                onlySecondaryDescription,
                forceRewrite,
                List.of(FamilyTreeType.PLAIN_PDF, FamilyTreeType.NETWORK, FamilyTreeType.GRAPH_JSON));

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
    @CrossOrigin(originPatterns = { "http://geneaazul.com.ar:[*]", "https://geneaazul.com.ar:[*]", "http://*.geneaazul.com.ar:[*]", "https://*.geneaazul.com.ar:[*]" })
    public SearchSurnamesResultDto searchSurnames(@Valid @RequestBody SearchSurnamesDto searchSurnamesDto, HttpServletRequest request) {

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

    @GetMapping("/family-tree/{personUuid}/plainPdf")
    @CrossOrigin(originPatterns = { "http://geneaazul.com.ar:[*]", "https://geneaazul.com.ar:[*]", "http://*.geneaazul.com.ar:[*]", "https://*.geneaazul.com.ar:[*]" }, exposedHeaders = { "Content-Disposition", "File-Name" })
    public ResponseEntity<Resource> getPlainFamilyTreePdf(
            @PathVariable UUID personUuid,
            @RequestParam @Nullable Boolean obfuscateLiving,
            @RequestParam @Nullable Boolean onlySecondaryDescription,
            @RequestParam @Nullable Boolean forceRewrite,
            HttpServletRequest request) throws IOException {

        // default: true
        boolean obfuscateLivingEnabled = !properties.isDisableObfuscateLiving()
                && BooleanUtils.isNotFalse(obfuscateLiving);
        // default: true
        boolean onlySecondaryDescriptionEnabled = BooleanUtils.isNotFalse(onlySecondaryDescription);
        // default: false
        boolean forceRewriteEnabled = BooleanUtils.isTrue(forceRewrite);

        Optional<FamilyTree> maybeFamilyTree = plainFamilyTreePdfService
                .getFamilyTree(
                        personUuid,
                        obfuscateLivingEnabled,
                        onlySecondaryDescriptionEnabled,
                        forceRewriteEnabled);

        log.info("Plain family tree [ personUuid={}, personId={}, obfuscateLiving={}, onlySecondaryDescriptionEnabled={}, forceRewrite={}, httpRequestId={} ]",
                personUuid,
                maybeFamilyTree
                        .map(FamilyTree::person)
                        .map(EnrichedPerson::getId)
                        .orElse(null),
                obfuscateLivingEnabled,
                onlySecondaryDescriptionEnabled,
                forceRewriteEnabled,
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

    @GetMapping("/family-tree/{personUuid}/graphJson")
    @CrossOrigin(originPatterns = { "http://geneaazul.com.ar:[*]", "https://geneaazul.com.ar:[*]", "http://*.geneaazul.com.ar:[*]", "https://*.geneaazul.com.ar:[*]" })
    public ResponseEntity<Resource> getGraphFamilyTreeJson(
            @PathVariable UUID personUuid,
            @RequestParam @Nullable Boolean obfuscateLiving,
            @RequestParam @Nullable Boolean onlySecondaryDescription,
            @RequestParam @Nullable Boolean forceRewrite,
            HttpServletRequest request) throws IOException {

        // default: true
        boolean obfuscateLivingEnabled = !properties.isDisableObfuscateLiving()
                && BooleanUtils.isNotFalse(obfuscateLiving);
        // default: true
        boolean onlySecondaryDescriptionEnabled = BooleanUtils.isNotFalse(onlySecondaryDescription);
        // default: false
        boolean forceRewriteEnabled = BooleanUtils.isTrue(forceRewrite);

        Optional<FamilyTree> maybeFamilyTree = graphJsonFamilyTreeService
                .getFamilyTree(
                        personUuid,
                        obfuscateLivingEnabled,
                        onlySecondaryDescriptionEnabled,
                        forceRewriteEnabled);

        log.info("Graph JSON family tree [ personUuid={}, personId={}, obfuscateLiving={}, onlySecondaryDescription={}, forceRewrite={}, httpRequestId={} ]",
                personUuid,
                maybeFamilyTree
                        .map(FamilyTree::person)
                        .map(EnrichedPerson::getId)
                        .orElse(null),
                obfuscateLivingEnabled,
                onlySecondaryDescriptionEnabled,
                forceRewriteEnabled,
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
    @CrossOrigin(originPatterns = { "http://geneaazul.com.ar:[*]", "https://geneaazul.com.ar:[*]", "http://*.geneaazul.com.ar:[*]", "https://*.geneaazul.com.ar:[*]" })
    public SearchConnectionResultDto searchConnection(
            @Valid @RequestBody SearchConnectionDto searchConnectionDto,
            HttpServletRequest request) {

        Optional<String> clientIpAddress = InetAddressUtils.getRemoteAddress(request);

        Optional<Long> searchId = Optional.empty();

        if (properties.isStoreConnectionSearch()) {
            searchId = connectionService.persistConnectionSearch(searchConnectionDto, clientIpAddress.orElse(null));
        }

        if (!clientIpAddress
                .map(familyService::isAllowedSearch)
                .orElse(true)) {
            SearchConnectionResultDto searchConnectionResult =  SearchConnectionResultDto.builder()
                    .errors(List.of("TOO-MANY-REQUESTS"))
                    .build();

            updateSearchResult(searchId, searchConnectionResult);

            return searchConnectionResult;
        }

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
