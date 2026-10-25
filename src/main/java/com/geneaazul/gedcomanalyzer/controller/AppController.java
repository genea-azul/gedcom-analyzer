package com.geneaazul.gedcomanalyzer.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.net.URI;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AppController {

    static final String WEBSITE_URL = "https://geneaazul.com.ar";

    @Value("${project.version}")
    private String projectVersion;

    /**
     * The website lives on Cloudflare Pages. Old links to this host (its former home page and the
     * pyvis network tree pages, removed with Python) go there instead.
     */
    @GetMapping({ "/", "/no-redirect", "/family-tree/{personUuid}", "/family-tree/{personUuid}/network" })
    public ResponseEntity<Void> redirectToWebsite() {
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create(WEBSITE_URL))
                .build();
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
