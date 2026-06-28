package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.domain.TreeBuilderSubmission;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.model.dto.TreeBuilderPersonDto;
import com.geneaazul.gedcomanalyzer.model.dto.TreeBuilderSubmitDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public class TreeBuilderControllerIT extends AbstractControllerIT {

    private static final String URL = "/api/tree-builder/submit";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testSubmitSuccess() throws Exception {
        TreeBuilderSubmitDto submitDto = TreeBuilderSubmitDto.builder()
                .ego(TreeBuilderPersonDto.builder()
                        .givenName("María")
                        .surname("González")
                        .sex(SexType.F)
                        .birthYear(1985)
                        .birthPlace("Buenos Aires")
                        .isDeceased(false)
                        .build())
                .partner(TreeBuilderPersonDto.builder()
                        .givenName("Juan")
                        .surname("Pérez")
                        .sex(SexType.M)
                        .birthYear(1982)
                        .build())
                .contact("maria@example.com")
                .build();

        doReturn(TreeBuilderSubmission.builder()
                .id(42L)
                .build())
                .when(treeBuilderSubmissionRepository)
                .save(any());

        MvcResult result = mvc.perform(post(URL)
                        .content(objectMapper.writeValueAsBytes(submitDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionId", is(42)))
                .andReturn();

        log.info("{} response:\n{}", URL, result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    public void testSubmitMissingEgo() throws Exception {
        TreeBuilderSubmitDto submitDto = TreeBuilderSubmitDto.builder()
                .contact("maria@example.com")
                .build();

        mvc.perform(post(URL)
                        .content(objectMapper.writeValueAsBytes(submitDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSubmitEgoFieldTooLong() throws Exception {
        TreeBuilderSubmitDto submitDto = TreeBuilderSubmitDto.builder()
                .ego(TreeBuilderPersonDto.builder()
                        .givenName("A".repeat(61))
                        .surname("González")
                        .build())
                .build();

        mvc.perform(post(URL)
                        .content(objectMapper.writeValueAsBytes(submitDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSubmitTooManyChildren() throws Exception {
        TreeBuilderPersonDto child = TreeBuilderPersonDto.builder()
                .givenName("Hijo")
                .surname("González")
                .birthYear(2010)
                .build();

        TreeBuilderSubmitDto submitDto = TreeBuilderSubmitDto.builder()
                .ego(TreeBuilderPersonDto.builder()
                        .givenName("María")
                        .surname("González")
                        .build())
                .children(Collections.nCopies(21, child))
                .build();

        mvc.perform(post(URL)
                        .content(objectMapper.writeValueAsBytes(submitDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetLatestSubmissions() throws Exception {
        doReturn(new PageImpl<>(
                List.of(
                        TreeBuilderSubmission.builder()
                                .id(42L)
                                .contact("maria@example.com")
                                .build()
                )))
                .when(treeBuilderSubmissionRepository)
                .findAll(any(Pageable.class));

        String url = "/api/admin/tree-builder/latest?page=0&size=5";
        MvcResult result = mvc.perform(get(url)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(42)))
                .andReturn();

        log.info("{} response:\n{}", url, result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    public void testSubmitMinimalEgo() throws Exception {
        TreeBuilderSubmitDto submitDto = TreeBuilderSubmitDto.builder()
                .ego(TreeBuilderPersonDto.builder()
                        .givenName("Ana")
                        .build())
                .build();

        doReturn(TreeBuilderSubmission.builder()
                .id(1L)
                .build())
                .when(treeBuilderSubmissionRepository)
                .save(any());

        mvc.perform(post(URL)
                        .content(objectMapper.writeValueAsBytes(submitDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionId", is(1)));
    }

    @Test
    public void testSubmitContactTooLong() throws Exception {
        TreeBuilderSubmitDto submitDto = TreeBuilderSubmitDto.builder()
                .ego(TreeBuilderPersonDto.builder()
                        .givenName("Ana")
                        .build())
                .contact("a".repeat(121))
                .build();

        mvc.perform(post(URL)
                        .content(objectMapper.writeValueAsBytes(submitDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSubmitInvalidBirthYear() throws Exception {
        TreeBuilderSubmitDto submitDto = TreeBuilderSubmitDto.builder()
                .ego(TreeBuilderPersonDto.builder()
                        .givenName("Ana")
                        .birthYear(2101)
                        .build())
                .build();

        mvc.perform(post(URL)
                        .content(objectMapper.writeValueAsBytes(submitDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSubmitChildrenAtMaxLimit() throws Exception {
        TreeBuilderPersonDto child = TreeBuilderPersonDto.builder()
                .givenName("Hijo")
                .build();

        TreeBuilderSubmitDto submitDto = TreeBuilderSubmitDto.builder()
                .ego(TreeBuilderPersonDto.builder()
                        .givenName("María")
                        .surname("González")
                        .build())
                .children(Collections.nCopies(20, child))
                .build();

        doReturn(TreeBuilderSubmission.builder()
                .id(1L)
                .build())
                .when(treeBuilderSubmissionRepository)
                .save(any());

        mvc.perform(post(URL)
                        .content(objectMapper.writeValueAsBytes(submitDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    public void testSubmitRateLimited() throws Exception {
        TreeBuilderSubmitDto submitDto = TreeBuilderSubmitDto.builder()
                .ego(TreeBuilderPersonDto.builder()
                        .givenName("María")
                        .surname("González")
                        .build())
                .contact("maria@example.com")
                .build();

        // Simulate IP address present and over threshold
        doReturn(100L)
                .when(treeBuilderSubmissionRepository)
                .countByClientIpAddressAndCreateDateBetween(anyString(), any(OffsetDateTime.class), any(OffsetDateTime.class));

        mvc.perform(post(URL)
                        .content(objectMapper.writeValueAsBytes(submitDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "1.2.3.4")
                        .with(csrf()))
                .andExpect(status().isTooManyRequests());
    }

}
