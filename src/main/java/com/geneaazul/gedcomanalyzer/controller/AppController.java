package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.FamilyTree;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDto;
import com.geneaazul.gedcomanalyzer.service.ClientRequestRateLimiter;
import com.geneaazul.gedcomanalyzer.service.PersonService;
import com.geneaazul.gedcomanalyzer.service.familytree.NetworkFamilyTreeService;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.InetAddressUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AppController {

    private final GedcomHolder gedcomHolder;
    private final PersonService personService;
    private final NetworkFamilyTreeService networkFamilyTreeService;
    private final GedcomAnalyzerProperties properties;
    private final ClientRequestRateLimiter clientRequestRateLimiter;

    @Value("${project.version}")
    private String projectVersion;

    @GetMapping("/")
    public ModelAndView index() {
        return new ModelAndView("index");
    }

    @GetMapping("/no-redirect")
    public ModelAndView noRedirect() {
        return new ModelAndView("site-v2/index");
    }

    @GetMapping("/family-tree/{personUuid}")
    public ModelAndView familyTreeView(@PathVariable UUID personUuid) {
        boolean obfuscateLiving = properties.isObfuscateLiving();

        String personDisplayName = personService
                .getPersonDto(gedcomHolder.getGedcom(), personUuid)
                .map(PersonDto::getName)
                .orElse("?");

        Map<String, ?> params = Map.of(
                "projectVersion", projectVersion,
                "personUuid", personUuid,
                "personDisplayName", personDisplayName,
                "obfuscateLiving", obfuscateLiving);
        return new ModelAndView("pyvis-network/nodes", params);
    }

    @GetMapping("/family-tree/{personUuid}/network")
    public ResponseEntity<String> networkFamilyTreeView(
            @PathVariable UUID personUuid,
            HttpServletRequest request) throws IOException {
        // Same budget as the PDF / graph JSON exports: each one runs a traversal and a Python process
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

        log.info("Network family tree [ personUuid={}, obfuscateLiving={}, httpRequestId={} ]",
                personUuid,
                obfuscateLiving,
                request.getRequestId());

        Optional<FamilyTree> maybeFamilyTree = networkFamilyTreeService
                .getFamilyTree(personUuid, obfuscateLiving, true, false);

        if (maybeFamilyTree.isEmpty()) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .cacheControl(CacheControl.noCache())
                    .body("<h4>Identificador de persona inv&aacute;lido.</h4>"
                            + "<p>Por favor realiz&aacute; una nueva b&uacute;squeda.</p>");
        }

        FamilyTree familyTree = maybeFamilyTree.get();

        return ResponseEntity.ok()
                .contentLength(Files.size(familyTree.path()))
                .contentType(familyTree.mediaType())
                // UUIDs change on every GEDCOM import; a short TTL also lets removed data disappear
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)))
                .body(Files.readString(familyTree.path(), StandardCharsets.UTF_8));
    }

    @GetMapping("/search-family/latest")
    public ModelAndView searchFamily() {
        Map<String, ?> params = Map.of(
                "projectVersion", projectVersion);
        return new ModelAndView("search-family/latest", params);
    }

    @GetMapping("/search-family/latestToReview")
    public ModelAndView searchFamilyToReview() {
        Map<String, ?> params = Map.of(
                "projectVersion", projectVersion,
                "toReview", true);
        return new ModelAndView("search-family/latest", params);
    }

    @GetMapping("/search-family/latestToReviewNotIgnored")
    public ModelAndView searchFamilyToReviewNotIgnored() {
        Map<String, ?> params = Map.of(
                "projectVersion", projectVersion,
                "toReview", true,
                "notIgnored", true);
        return new ModelAndView("search-family/latest", params);
    }

    @GetMapping("/search-connection/latest")
    public ModelAndView searchConnection() {
        Map<String, ?> params = Map.of(
                "projectVersion", projectVersion);
        return new ModelAndView("search-connection/latest", params);
    }

    @GetMapping("/search-connection/latestToReview")
    public ModelAndView searchConnectionToReview() {
        Map<String, ?> params = Map.of(
                "projectVersion", projectVersion,
                "toReview", true);
        return new ModelAndView("search-connection/latest", params);
    }

    @GetMapping("/tree-builder/latest")
    public ModelAndView treeBuilderLatest() {
        Map<String, ?> params = Map.of(
                "projectVersion", projectVersion);
        return new ModelAndView("tree-builder/latest", params);
    }

    @GetMapping("/comments/latest")
    public ModelAndView commentsLatest() {
        Map<String, ?> params = Map.of(
                "projectVersion", projectVersion);
        return new ModelAndView("comments/latest", params);
    }

    @GetMapping("/comments/latestToReview")
    public ModelAndView commentsLatestToReview() {
        Map<String, ?> params = Map.of(
                "projectVersion", projectVersion,
                "toReview", true);
        return new ModelAndView("comments/latest", params);
    }

}
