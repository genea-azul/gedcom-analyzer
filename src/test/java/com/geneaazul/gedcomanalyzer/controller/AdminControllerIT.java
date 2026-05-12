package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.domain.SearchConnection;
import com.geneaazul.gedcomanalyzer.domain.SearchFamily;
import com.geneaazul.gedcomanalyzer.domain.TreeBuilderSubmission;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SuppressWarnings("unchecked")
public class AdminControllerIT extends AbstractControllerIT {

    @Autowired
    private MockMvc mvc;

    // ── Gedcom ───────────────────────────────────────────────────────────────

    @Test
    public void testHealthCheck() throws Exception {
        String url = "/api/admin/gedcom-analyzer/health";
        MvcResult result = mvc.perform(get(url).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")))
                .andReturn();

        log.info("{} response:\n{}", url, result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    // ── Search — family ───────────────────────────────────────────────────────

    @Test
    public void testGetLatestFamilySearches() throws Exception {
        doReturn(new PageImpl<>(
                List.of(
                        SearchFamily.builder()
                                .id(1L)
                                .isMatch(true)
                                .isReviewed(null)
                                .contact("@contact")
                                .build()
                )))
                .when(searchFamilyRepository)
                .findAll(any(Specification.class), any(Pageable.class));

        String url = "/api/admin/search/family/latest?isMatch=true&page=0&size=5";
        MvcResult result = mvc.perform(get(url).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].isMatch", is(true)))
                .andReturn();

        log.info("{} response:\n{}", url, result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    public void testGetLatestFamilySearches_noFilters_returnsAll() throws Exception {
        doReturn(new PageImpl<>(List.of(
                SearchFamily.builder().id(1L).build(),
                SearchFamily.builder().id(2L).build()
        )))
                .when(searchFamilyRepository)
                .findAll(any(Specification.class), any(Pageable.class));

        String url = "/api/admin/search/family/latest?page=0&size=10";
        mvc.perform(get(url).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    public void testMarkFamilySearchReviewed() throws Exception {
        SearchFamily family = SearchFamily.builder()
                .id(1L)
                .isMatch(false)
                .isReviewed(null)
                .build();

        doReturn(Optional.of(family)).when(searchFamilyRepository).findById(1L);
        doAnswer(inv -> inv.getArgument(0)).when(searchFamilyRepository).save(any());

        String url = "/api/admin/search/family/1/reviewed";
        MvcResult result = mvc.perform(get(url).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.isReviewed", is(true)))
                .andReturn();

        log.info("{} response:\n{}", url, result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    public void testMarkFamilySearchIgnored() throws Exception {
        SearchFamily family = SearchFamily.builder()
                .id(2L)
                .isMatch(false)
                .isIgnored(null)
                .build();

        doReturn(Optional.of(family)).when(searchFamilyRepository).findById(2L);
        doAnswer(inv -> inv.getArgument(0)).when(searchFamilyRepository).save(any());

        String url = "/api/admin/search/family/2/ignored";
        MvcResult result = mvc.perform(get(url).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(2)))
                .andExpect(jsonPath("$.isIgnored", is(true)))
                .andReturn();

        log.info("{} response:\n{}", url, result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    public void testMarkFamilySearchReviewed_notFound_throwsException() {
        doReturn(Optional.empty()).when(searchFamilyRepository).findById(999L);

        assertThrows(Exception.class, () ->
                mvc.perform(get("/api/admin/search/family/999/reviewed").with(csrf())));
    }

    // ── Search — connection ───────────────────────────────────────────────────

    @Test
    public void testGetLatestConnectionSearches() throws Exception {
        doReturn(new PageImpl<>(
                List.of(
                        SearchConnection.builder()
                                .id(10L)
                                .isMatch(true)
                                .isReviewed(null)
                                .build()
                )))
                .when(searchConnectionRepository)
                .findAll(any(Specification.class), any(Pageable.class));

        String url = "/api/admin/search/connection/latest?page=0&size=5";
        MvcResult result = mvc.perform(get(url).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(10)))
                .andReturn();

        log.info("{} response:\n{}", url, result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    public void testMarkConnectionSearchReviewed() throws Exception {
        SearchConnection connection = SearchConnection.builder()
                .id(10L)
                .isMatch(true)
                .isReviewed(null)
                .build();

        doReturn(Optional.of(connection)).when(searchConnectionRepository).findById(10L);
        doAnswer(inv -> inv.getArgument(0)).when(searchConnectionRepository).save(any());

        String url = "/api/admin/search/connection/10/reviewed";
        MvcResult result = mvc.perform(get(url).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.isReviewed", is(true)))
                .andReturn();

        log.info("{} response:\n{}", url, result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    // ── Tree builder ──────────────────────────────────────────────────────────

    @Test
    public void testGetLatestTreeBuilderSubmissions() throws Exception {
        doReturn(new PageImpl<>(
                List.of(
                        TreeBuilderSubmission.builder()
                                .id(5L)
                                .contact("user@example.com")
                                .clientIpAddress("1.2.3.4")
                                .payload("{\"ego\":{\"givenName\":\"Ana\"}}")
                                .build()
                )))
                .when(treeBuilderSubmissionRepository)
                .findAll(any(Pageable.class));

        String url = "/api/admin/tree-builder/latest?page=0&size=10";
        MvcResult result = mvc.perform(get(url).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(5)))
                .andExpect(jsonPath("$[0].contact", is("user@example.com")))
                .andReturn();

        log.info("{} response:\n{}", url, result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    public void testGetLatestTreeBuilderSubmissions_emptyResult() throws Exception {
        doReturn(new PageImpl<>(List.of()))
                .when(treeBuilderSubmissionRepository)
                .findAll(any(Pageable.class));

        mvc.perform(get("/api/admin/tree-builder/latest").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testGetLatestFamilySearches_withContentType() throws Exception {
        doReturn(new PageImpl<>(List.of())).when(searchFamilyRepository)
                .findAll(any(Specification.class), any(Pageable.class));

        mvc.perform(get("/api/admin/search/family/latest").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

}
