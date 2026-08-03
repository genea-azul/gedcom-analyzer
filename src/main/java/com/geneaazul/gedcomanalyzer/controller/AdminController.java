package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.dto.CommentDetailsDto;
import com.geneaazul.gedcomanalyzer.model.dto.CommentStatus;
import com.geneaazul.gedcomanalyzer.model.dto.GedcomAnalysisDto;
import com.geneaazul.gedcomanalyzer.model.dto.GedcomMetadataDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchConnectionDetailsDto;
import com.geneaazul.gedcomanalyzer.model.dto.SearchFamilyDetailsDto;
import com.geneaazul.gedcomanalyzer.model.dto.TreeBuilderSubmissionDetailsDto;
import com.geneaazul.gedcomanalyzer.model.dto.UsageStatsDto;
import com.geneaazul.gedcomanalyzer.service.CommentService;
import com.geneaazul.gedcomanalyzer.service.ConnectionService;
import com.geneaazul.gedcomanalyzer.service.FamilyService;
import com.geneaazul.gedcomanalyzer.service.GedcomAnalyzerService;
import com.geneaazul.gedcomanalyzer.service.GedcomParsingService;
import com.geneaazul.gedcomanalyzer.service.TreeBuilderService;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin-only endpoints. No @CrossOrigin — the public website must never call these.
 * Gated by {@link com.geneaazul.gedcomanalyzer.config.AdminTokenFilter} via ?token=... when ADMIN_TOKEN is set.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final GedcomHolder gedcomHolder;
    private final GedcomParsingService gedcomParsingService;
    private final GedcomAnalyzerService gedcomAnalyzerService;
    private final FamilyService familyService;
    private final ConnectionService connectionService;
    private final TreeBuilderService treeBuilderService;
    private final CommentService commentService;

    // ── Gedcom ──────────────────────────────────────────────────────────────

    @GetMapping("/gedcom-analyzer/health")
    public Map<String, Object> healthCheck(HttpServletRequest request) {
        log.debug("Health check [ httpRequestId={} ]", request.getRequestId());
        return Map.of("status", "ok");
    }

    @GetMapping("/gedcom-analyzer/usageStats")
    public UsageStatsDto getUsageStats() {
        return gedcomAnalyzerService.getUsageStats();
    }

    @GetMapping("/gedcom-analyzer/reload")
    public GedcomMetadataDto reloadAndGetGedcomMetadata() {
        Instant start = Instant.now();
        gedcomHolder.reloadFromStorage(true);
        Duration reloadDuration = Duration.between(start, Instant.now());
        return gedcomAnalyzerService.getGedcomMetadata(gedcomHolder.getGedcom(), reloadDuration);
    }

    @PostMapping("/gedcom-analyzer")
    public GedcomAnalysisDto analyzeGedcom(@RequestPart MultipartFile gedcomFile) throws SAXParseException, IOException {
        EnrichedGedcom gedcom = gedcomParsingService.parse(gedcomFile);
        return gedcomAnalyzerService.analyze(gedcom);
    }

    // ── Search — family ─────────────────────────────────────────────────────

    @GetMapping("/search/family/latest")
    public List<SearchFamilyDetailsDto> getLatestFamilySearches(
            @RequestParam @Nullable Boolean isMatch,
            @RequestParam @Nullable Boolean isReviewed,
            @RequestParam @Nullable Boolean isIgnored,
            @RequestParam @Nullable Boolean hasContact,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        log.info("Search family latest [ isMatch={}, isReviewed={}, isIgnored={}, hasContact={}, page={}, size={} ]",
                isMatch, isReviewed, isIgnored, hasContact, page, size);
        String context = StringUtils.substringBefore(request.getRequestURL().toString(), "/api");
        return familyService.getLatest(isMatch, isReviewed, isIgnored, hasContact, page, size, context);
    }

    @GetMapping("/search/family/{searchId}/reviewed")
    public SearchFamilyDetailsDto markFamilyReviewed(
            @PathVariable Long searchId,
            @RequestParam(defaultValue = BooleanUtils.TRUE) Boolean isReviewed) {
        log.info("Mark family reviewed [ searchId={}, isReviewed={} ]", searchId, isReviewed);
        return familyService.updateFamilySearchIsReviewed(searchId, isReviewed);
    }

    @GetMapping("/search/family/{searchId}/ignored")
    public SearchFamilyDetailsDto markFamilyIgnored(
            @PathVariable Long searchId,
            @RequestParam(defaultValue = BooleanUtils.TRUE) Boolean isIgnored) {
        log.info("Mark family ignored [ searchId={}, isIgnored={} ]", searchId, isIgnored);
        return familyService.updateFamilySearchIsIgnored(searchId, isIgnored);
    }

    // ── Search — connection ─────────────────────────────────────────────────

    @GetMapping("/search/connection/latest")
    public List<SearchConnectionDetailsDto> getLatestConnectionSearches(
            @RequestParam @Nullable Boolean isMatch,
            @RequestParam @Nullable Boolean isReviewed,
            @RequestParam @Nullable Boolean hasContact,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        log.info("Search connection latest [ isMatch={}, isReviewed={}, hasContact={}, page={}, size={} ]",
                isMatch, isReviewed, hasContact, page, size);
        String context = StringUtils.substringBefore(request.getRequestURL().toString(), "/api");
        return connectionService.getLatest(isMatch, isReviewed, hasContact, page, size, context);
    }

    @GetMapping("/search/connection/{searchId}/reviewed")
    public SearchConnectionDetailsDto markConnectionReviewed(
            @PathVariable Long searchId,
            @RequestParam(defaultValue = BooleanUtils.TRUE) Boolean isReviewed) {
        log.info("Mark connection reviewed [ searchId={}, isReviewed={} ]", searchId, isReviewed);
        return connectionService.updateConnectionSearchIsReviewed(searchId, isReviewed);
    }

    // ── Tree builder ────────────────────────────────────────────────────────

    @GetMapping("/tree-builder/latest")
    public List<TreeBuilderSubmissionDetailsDto> getLatestTreeBuilderSubmissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Tree builder submissions latest [ page={}, size={} ]", page, size);
        return treeBuilderService.getLatest(page, size);
    }

    // ── Comments ────────────────────────────────────────────────────────────

    @GetMapping("/comments/latest")
    public List<CommentDetailsDto> getLatestComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam @Nullable CommentStatus status,
            HttpServletRequest request) {
        log.info("Comments latest [ page={}, size={}, status={} ]", page, size, status);
        String context = StringUtils.substringBefore(request.getRequestURL().toString(), "/api");
        return commentService.getLatest(page, size, status, context);
    }

    @GetMapping("/comments/{commentId}/status")
    public CommentDetailsDto updateCommentStatus(
            @PathVariable Long commentId,
            @RequestParam CommentStatus status) {
        log.info("Update comment status [ commentId={}, status={} ]", commentId, status);
        return commentService.updateStatus(commentId, status);
    }

}
