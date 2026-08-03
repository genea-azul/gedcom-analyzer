package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.domain.UserComment;
import com.geneaazul.gedcomanalyzer.model.dto.CommentContextType;
import com.geneaazul.gedcomanalyzer.model.dto.CommentStatus;
import com.geneaazul.gedcomanalyzer.model.dto.CommentSubmitDto;
import com.geneaazul.gedcomanalyzer.model.dto.CommentType;

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
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public class CommentControllerIT extends AbstractControllerIT {

    private static final String URL = "/api/comments";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testSubmitSuccess() throws Exception {
        CommentSubmitDto submitDto = CommentSubmitDto.builder()
                .contextType(CommentContextType.FAMILY)
                .contextId("gennuso")
                .commentType(CommentType.MEMORY)
                .authorName("Ana")
                .authorLocation("Azul")
                .body("Un recuerdo lindo de la familia.")
                .build();

        doReturn(UserComment.builder()
                .id(42L)
                .build())
                .when(userCommentRepository)
                .save(any());

        MvcResult result = mvc.perform(post(URL)
                        .content(objectMapper.writeValueAsBytes(submitDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId", is(42)))
                .andReturn();

        log.info("{} response:\n{}", URL, result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    public void testSubmitMissingContextType() throws Exception {
        CommentSubmitDto submitDto = CommentSubmitDto.builder()
                .contextId("gennuso")
                .commentType(CommentType.MEMORY)
                .authorName("Ana")
                .body("Un recuerdo.")
                .build();

        mvc.perform(post(URL)
                        .content(objectMapper.writeValueAsBytes(submitDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSubmitInvalidContextType() throws Exception {
        String invalidJson = "{\"contextType\":\"NOT_A_TYPE\",\"contextId\":\"gennuso\","
                + "\"commentType\":\"MEMORY\",\"authorName\":\"Ana\",\"body\":\"Un recuerdo.\"}";

        mvc.perform(post(URL)
                        .content(invalidJson)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSubmitMissingAuthorName() throws Exception {
        CommentSubmitDto submitDto = CommentSubmitDto.builder()
                .contextType(CommentContextType.STORY)
                .contextId("mi-historia")
                .commentType(CommentType.QUESTION)
                .body("¿Alguien sabe más de este dato?")
                .build();

        mvc.perform(post(URL)
                        .content(objectMapper.writeValueAsBytes(submitDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSubmitBlankBody() throws Exception {
        CommentSubmitDto submitDto = CommentSubmitDto.builder()
                .contextType(CommentContextType.STORY)
                .contextId("mi-historia")
                .commentType(CommentType.QUESTION)
                .authorName("Juan")
                .body("   ")
                .build();

        mvc.perform(post(URL)
                        .content(objectMapper.writeValueAsBytes(submitDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSubmitAuthorNameTooLong() throws Exception {
        CommentSubmitDto submitDto = CommentSubmitDto.builder()
                .contextType(CommentContextType.FAMILY)
                .contextId("gennuso")
                .commentType(CommentType.MEMORY)
                .authorName("A".repeat(101))
                .body("Un recuerdo.")
                .build();

        mvc.perform(post(URL)
                        .content(objectMapper.writeValueAsBytes(submitDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSubmitBodyTooLong() throws Exception {
        CommentSubmitDto submitDto = CommentSubmitDto.builder()
                .contextType(CommentContextType.FAMILY)
                .contextId("gennuso")
                .commentType(CommentType.MEMORY)
                .authorName("Ana")
                .body("a".repeat(2001))
                .build();

        mvc.perform(post(URL)
                        .content(objectMapper.writeValueAsBytes(submitDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSubmitInvalidEmail() throws Exception {
        CommentSubmitDto submitDto = CommentSubmitDto.builder()
                .contextType(CommentContextType.FAMILY)
                .contextId("gennuso")
                .commentType(CommentType.MEMORY)
                .authorName("Ana")
                .authorEmail("not-an-email")
                .body("Un recuerdo.")
                .build();

        mvc.perform(post(URL)
                        .content(objectMapper.writeValueAsBytes(submitDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSubmitWantsContactWithoutEmail() throws Exception {
        CommentSubmitDto submitDto = CommentSubmitDto.builder()
                .contextType(CommentContextType.FAMILY)
                .contextId("gennuso")
                .commentType(CommentType.MEMORY)
                .authorName("Ana")
                .authorWantsContact(true)
                .body("Un recuerdo.")
                .build();

        mvc.perform(post(URL)
                        .content(objectMapper.writeValueAsBytes(submitDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSubmitWantsContactWithBlankEmail() throws Exception {
        String json = "{\"contextType\":\"FAMILY\",\"contextId\":\"gennuso\",\"commentType\":\"MEMORY\","
                + "\"authorName\":\"Ana\",\"authorWantsContact\":true,\"authorEmail\":\"\",\"body\":\"Un recuerdo.\"}";

        mvc.perform(post(URL)
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSubmitWantsContactWithEmailSuccess() throws Exception {
        CommentSubmitDto submitDto = CommentSubmitDto.builder()
                .contextType(CommentContextType.FAMILY)
                .contextId("gennuso")
                .commentType(CommentType.MEMORY)
                .authorName("Ana")
                .authorWantsContact(true)
                .authorEmail("ana@example.com")
                .body("Un recuerdo.")
                .build();

        doReturn(UserComment.builder()
                .id(43L)
                .build())
                .when(userCommentRepository)
                .save(any());

        mvc.perform(post(URL)
                        .content(objectMapper.writeValueAsBytes(submitDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId", is(43)));
    }

    @Test
    public void testSubmitRateLimited() throws Exception {
        CommentSubmitDto submitDto = CommentSubmitDto.builder()
                .contextType(CommentContextType.FAMILY)
                .contextId("gennuso")
                .commentType(CommentType.MEMORY)
                .authorName("Ana")
                .body("Un recuerdo.")
                .build();

        // Simulate IP address present and over threshold
        doReturn(100L)
                .when(userCommentRepository)
                .countByClientIpAddressAndCreateDateBetween(anyString(), any(OffsetDateTime.class), any(OffsetDateTime.class));

        mvc.perform(post(URL)
                        .content(objectMapper.writeValueAsBytes(submitDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "1.2.3.4")
                        .with(csrf()))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    public void testGetApprovedSuccess() throws Exception {
        UserComment comment = UserComment.builder()
                .id(1L)
                .contextType(CommentContextType.FAMILY)
                .contextId("gennuso")
                .commentType(CommentType.MEMORY)
                .authorName("Ana")
                .authorLocation("Azul")
                .body("Un recuerdo lindo.")
                .status(CommentStatus.APPROVED)
                .build();

        doReturn(List.of(comment))
                .when(userCommentRepository)
                .findByContextTypeAndContextIdAndStatusOrderByCreateDateAsc(
                        eq(CommentContextType.FAMILY), eq("gennuso"), eq(CommentStatus.APPROVED));

        String url = URL + "?contextType=FAMILY&contextId=gennuso";
        MvcResult result = mvc.perform(get(url)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].authorName", is("Ana")))
                .andExpect(jsonPath("$[0].authorEmail").doesNotExist())
                .andReturn();

        log.info("{} response:\n{}", url, result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    public void testGetApprovedMissingContextType() throws Exception {
        mvc.perform(get(URL + "?contextId=gennuso")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetApprovedInvalidContextType() throws Exception {
        mvc.perform(get(URL + "?contextType=NOT_A_TYPE&contextId=gennuso")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetApprovedNoResults() throws Exception {
        doReturn(List.of())
                .when(userCommentRepository)
                .findByContextTypeAndContextIdAndStatusOrderByCreateDateAsc(
                        eq(CommentContextType.STORY), eq("unknown-slug"), eq(CommentStatus.APPROVED));

        mvc.perform(get(URL + "?contextType=STORY&contextId=unknown-slug")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ── Admin moderation endpoints (AdminController) ──────────────────────────

    @Test
    public void testAdminGetLatestComments() throws Exception {
        UserComment comment = UserComment.builder()
                .id(7L)
                .contextType(CommentContextType.STORY)
                .contextId("mi-historia")
                .commentType(CommentType.QUESTION)
                .authorName("Juan")
                .authorEmail("juan@example.com")
                .body("¿De qué año es este dato?")
                .status(CommentStatus.PENDING)
                .clientIpAddress("1.2.3.4")
                .build();

        doReturn(new PageImpl<>(List.of(comment)))
                .when(userCommentRepository)
                .findAll(any(Pageable.class));

        String url = "/api/admin/comments/latest?page=0&size=5";
        MvcResult result = mvc.perform(get(url)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(7)))
                .andExpect(jsonPath("$[0].status", is("PENDING")))
                .andReturn();

        log.info("{} response:\n{}", url, result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    public void testAdminGetLatestCommentsFilteredByStatus() throws Exception {
        UserComment comment = UserComment.builder()
                .id(8L)
                .contextType(CommentContextType.FAMILY)
                .contextId("gennuso")
                .commentType(CommentType.MEMORY)
                .authorName("Silvia")
                .body("Necesito saber si mi abuelo...")
                .status(CommentStatus.PENDING)
                .build();

        doReturn(new PageImpl<>(List.of(comment)))
                .when(userCommentRepository)
                .findByStatus(eq(CommentStatus.PENDING), any(Pageable.class));

        String url = "/api/admin/comments/latest?page=0&size=30&status=PENDING";
        mvc.perform(get(url)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(8)))
                .andExpect(jsonPath("$[0].status", is("PENDING")));
    }

    @Test
    public void testAdminUpdateCommentStatusApproved() throws Exception {
        UserComment comment = UserComment.builder()
                .id(7L)
                .contextType(CommentContextType.STORY)
                .contextId("mi-historia")
                .commentType(CommentType.QUESTION)
                .authorName("Juan")
                .status(CommentStatus.PENDING)
                .build();

        doReturn(Optional.of(comment))
                .when(userCommentRepository)
                .findById(7L);

        mvc.perform(get("/api/admin/comments/7/status?status=APPROVED")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));
    }

    @Test
    public void testAdminUpdateCommentStatusNotFound() throws Exception {
        doReturn(Optional.empty())
                .when(userCommentRepository)
                .findById(999L);

        mvc.perform(get("/api/admin/comments/999/status?status=APPROVED")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

}
