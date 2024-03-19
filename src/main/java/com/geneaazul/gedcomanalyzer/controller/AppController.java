package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.FamilyTree;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDto;
import com.geneaazul.gedcomanalyzer.service.DockerService;
import com.geneaazul.gedcomanalyzer.service.PersonService;
import com.geneaazul.gedcomanalyzer.service.familytree.NetworkFamilyTreeService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AppController {

    private final DockerService dockerService;
    private final PersonService personService;
    private final NetworkFamilyTreeService networkFamilyTreeService;
    private final GedcomAnalyzerProperties properties;

    @Value("${project.version}")
    private String projectVersion;

    @GetMapping("/")
    public ModelAndView index(@RequestParam @Nullable String f) {
        dockerService.startDbContainer();
        boolean obfuscateLiving = !properties.isDisableObfuscateLiving() && !"0".equals(f);
        Map<String, ?> params = Map.of(
                "projectVersion", projectVersion,
                "obfuscateLiving", obfuscateLiving);
        return new ModelAndView("index", params);
    }

    @GetMapping("/family-tree/{personUuid}")
    public ModelAndView familyTreeView(
            @PathVariable UUID personUuid,
            @RequestParam @Nullable String f) {
        boolean obfuscateLiving = !properties.isDisableObfuscateLiving() && !"0".equals(f);

        String personDisplayName = personService
                .getPersonDto(personUuid)
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
            @RequestParam @Nullable String f,
            HttpServletRequest request) throws IOException {
        boolean obfuscateLiving = !properties.isDisableObfuscateLiving() && !"0".equals(f);

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
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)))
                .body(Files.readString(familyTree.path(), StandardCharsets.UTF_8));
    }

    @GetMapping("/search-family/latest")
    public ModelAndView searchFamily() {
        dockerService.startDbContainer();
        Map<String, ?> params = Map.of(
                "projectVersion", projectVersion);
        return new ModelAndView("search-family/latest", params);
    }

    @GetMapping("/search-family/latestToReview")
    public ModelAndView searchFamilyToReview() {
        dockerService.startDbContainer();
        Map<String, ?> params = Map.of(
                "projectVersion", projectVersion,
                "toReview", true);
        return new ModelAndView("search-family/latest", params);
    }

}
