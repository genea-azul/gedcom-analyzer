package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.domain.UserComment;
import com.geneaazul.gedcomanalyzer.model.dto.CommentContextType;
import com.geneaazul.gedcomanalyzer.model.dto.CommentDetailsDto;
import com.geneaazul.gedcomanalyzer.model.dto.CommentDto;
import com.geneaazul.gedcomanalyzer.model.dto.CommentStatus;
import com.geneaazul.gedcomanalyzer.model.dto.CommentSubmitDto;
import com.geneaazul.gedcomanalyzer.model.dto.CommentType;
import com.geneaazul.gedcomanalyzer.repository.UserCommentRepository;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private UserCommentRepository commentRepository;
    @Mock
    private GedcomAnalyzerProperties properties;

    @InjectMocks
    private CommentService commentService;

    // ── isAllowedSubmit ──────────────────────────────────────────────────────

    @Test
    void isAllowedSubmit_nullIp_returnsTrueWithoutHittingDb() {
        assertThat(commentService.isAllowedSubmit(null)).isTrue();
        verifyNoInteractions(commentRepository);
    }

    @Test
    void isAllowedSubmit_countBelowThreshold_returnsTrue() {
        when(properties.getMaxClientRequestsHoursThreshold()).thenReturn(1);
        when(properties.getClientsWithSpecialThreshold()).thenReturn(Set.of());
        when(properties.getMaxClientRequestsCountThreshold()).thenReturn(12);
        when(commentRepository.countByClientIpAddressAndCreateDateBetween(any(), any(), any())).thenReturn(11L);

        assertThat(commentService.isAllowedSubmit("1.2.3.4")).isTrue();
    }

    @Test
    void isAllowedSubmit_countAtThreshold_returnsFalse() {
        when(properties.getMaxClientRequestsHoursThreshold()).thenReturn(1);
        when(properties.getClientsWithSpecialThreshold()).thenReturn(Set.of());
        when(properties.getMaxClientRequestsCountThreshold()).thenReturn(12);
        when(commentRepository.countByClientIpAddressAndCreateDateBetween(any(), any(), any())).thenReturn(12L);

        assertThat(commentService.isAllowedSubmit("1.2.3.4")).isFalse();
    }

    @Test
    void isAllowedSubmit_specialIpBelowSpecialThreshold_returnsTrue() {
        when(properties.getMaxClientRequestsHoursThreshold()).thenReturn(1);
        when(properties.getClientsWithSpecialThreshold()).thenReturn(Set.of("1.2.3.4"));
        when(properties.getMaxClientRequestsCountSpecialThreshold()).thenReturn(3);
        when(commentRepository.countByClientIpAddressAndCreateDateBetween(any(), any(), any())).thenReturn(2L);

        assertThat(commentService.isAllowedSubmit("1.2.3.4")).isTrue();
    }

    @Test
    void isAllowedSubmit_specialIpAtSpecialThreshold_returnsFalse() {
        when(properties.getMaxClientRequestsHoursThreshold()).thenReturn(1);
        when(properties.getClientsWithSpecialThreshold()).thenReturn(Set.of("1.2.3.4"));
        when(properties.getMaxClientRequestsCountSpecialThreshold()).thenReturn(3);
        when(commentRepository.countByClientIpAddressAndCreateDateBetween(any(), any(), any())).thenReturn(3L);

        assertThat(commentService.isAllowedSubmit("1.2.3.4")).isFalse();
    }

    @Test
    void isAllowedSubmit_regularIpNotInSpecialSet_usesRegularThreshold() {
        when(properties.getMaxClientRequestsHoursThreshold()).thenReturn(1);
        when(properties.getClientsWithSpecialThreshold()).thenReturn(Set.of("9.9.9.9"));
        when(properties.getMaxClientRequestsCountThreshold()).thenReturn(12);
        when(commentRepository.countByClientIpAddressAndCreateDateBetween(any(), any(), any())).thenReturn(5L);

        assertThat(commentService.isAllowedSubmit("1.2.3.4")).isTrue();
    }

    // ── persistComment ───────────────────────────────────────────────────────

    @Test
    void persistComment_savesWithCorrectFields() {
        when(commentRepository.save(any())).thenAnswer(inv -> {
            UserComment c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });

        CommentSubmitDto dto = CommentSubmitDto.builder()
                .contextType(CommentContextType.FAMILY)
                .contextId("gennuso")
                .commentType(CommentType.MEMORY)
                .authorName("Ana")
                .authorLocation("Azul")
                .authorEmail("ana@example.com")
                .authorWantsContact(true)
                .body("Un recuerdo lindo.")
                .build();

        Long id = commentService.persistComment(dto, "10.0.0.1");

        assertThat(id).isEqualTo(99L);
        verify(commentRepository).save(argThat((UserComment c) ->
                c.getContextType() == dto.getContextType()
                        && c.getContextId().equals(dto.getContextId())
                        && c.getCommentType() == dto.getCommentType()
                        && c.getAuthorName().equals(dto.getAuthorName())
                        && Objects.equals(c.getAuthorLocation(), dto.getAuthorLocation())
                        && Objects.equals(c.getAuthorEmail(), dto.getAuthorEmail())
                        && c.getAuthorWantsContact() == dto.isAuthorWantsContact()
                        && c.getBody().equals(dto.getBody())
                        && "10.0.0.1".equals(c.getClientIpAddress())));
    }

    @Test
    void persistComment_nullIpAddress_savesWithNullClientIp() {
        when(commentRepository.save(any())).thenAnswer(inv -> {
            UserComment c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CommentSubmitDto dto = CommentSubmitDto.builder()
                .contextType(CommentContextType.STORY)
                .contextId("mi-historia")
                .commentType(CommentType.QUESTION)
                .authorName("Juan")
                .body("¿De qué año es este dato?")
                .build();

        Long id = commentService.persistComment(dto, null);

        assertThat(id).isEqualTo(1L);
        verify(commentRepository).save(argThat((UserComment c) -> c.getClientIpAddress() == null));
    }

    @Test
    void persistComment_defaultsToPendingStatus() {
        when(commentRepository.save(any())).thenAnswer(inv -> {
            UserComment c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });

        CommentSubmitDto dto = CommentSubmitDto.builder()
                .contextType(CommentContextType.TESTIMONIAL)
                .contextId("global")
                .commentType(CommentType.TESTIMONIAL)
                .authorName("María")
                .body("Gracias por este proyecto.")
                .build();

        commentService.persistComment(dto, "1.2.3.4");

        verify(commentRepository).save(argThat((UserComment c) -> c.getStatus() == CommentStatus.PENDING));
    }

    @Test
    void persistComment_wantsContactWithoutEmail_doesNotNotify() {
        when(commentRepository.save(any())).thenAnswer(inv -> {
            UserComment c = inv.getArgument(0);
            c.setId(3L);
            return c;
        });

        CommentSubmitDto dto = CommentSubmitDto.builder()
                .contextType(CommentContextType.FAMILY)
                .contextId("gennuso")
                .commentType(CommentType.MEMORY)
                .authorName("Ana")
                .authorWantsContact(true)
                .body("Recuerdo")
                .build();

        commentService.persistComment(dto, "1.2.3.4");

        verify(commentRepository, never())
                .findByContextTypeAndContextIdAndAuthorWantsContactTrueAndAuthorEmailIsNotNullAndStatus(any(), any(), any());
    }

    @Test
    void persistComment_wantsContactWithNoEmail_doesNotNotify() {
        when(commentRepository.save(any())).thenAnswer(inv -> {
            UserComment c = inv.getArgument(0);
            c.setId(4L);
            return c;
        });

        CommentSubmitDto dto = CommentSubmitDto.builder()
                .contextType(CommentContextType.FAMILY)
                .contextId("gennuso")
                .commentType(CommentType.MEMORY)
                .authorName("Ana")
                .authorWantsContact(false)
                .authorEmail("ana@example.com")
                .body("Recuerdo")
                .build();

        commentService.persistComment(dto, "1.2.3.4");

        verify(commentRepository, never())
                .findByContextTypeAndContextIdAndAuthorWantsContactTrueAndAuthorEmailIsNotNullAndStatus(any(), any(), any());
    }

    @Test
    void persistComment_wantsContactWithEmailAndExistingCandidates_queriesForOthers() {
        when(commentRepository.save(any())).thenAnswer(inv -> {
            UserComment c = inv.getArgument(0);
            c.setId(5L);
            return c;
        });
        when(commentRepository.findByContextTypeAndContextIdAndAuthorWantsContactTrueAndAuthorEmailIsNotNullAndStatus(
                CommentContextType.FAMILY, "gennuso", CommentStatus.APPROVED))
                .thenReturn(List.of(UserComment.builder()
                        .id(1L)
                        .contextType(CommentContextType.FAMILY)
                        .contextId("gennuso")
                        .commentType(CommentType.MEMORY)
                        .authorName("Otro")
                        .authorEmail("otro@example.com")
                        .authorWantsContact(true)
                        .body("otro recuerdo")
                        .build()));

        CommentSubmitDto dto = CommentSubmitDto.builder()
                .contextType(CommentContextType.FAMILY)
                .contextId("gennuso")
                .commentType(CommentType.MEMORY)
                .authorName("Ana")
                .authorEmail("ana@example.com")
                .authorWantsContact(true)
                .body("Recuerdo")
                .build();

        commentService.persistComment(dto, "1.2.3.4");

        verify(commentRepository)
                .findByContextTypeAndContextIdAndAuthorWantsContactTrueAndAuthorEmailIsNotNullAndStatus(
                        CommentContextType.FAMILY, "gennuso", CommentStatus.APPROVED);
    }

    // ── getApproved ──────────────────────────────────────────────────────────

    @Test
    void getApproved_mapsFieldsAndExcludesPrivateData() {
        OffsetDateTime now = OffsetDateTime.now();
        UserComment comment = UserComment.builder()
                .id(5L)
                .contextType(CommentContextType.FAMILY)
                .contextId("gennuso")
                .commentType(CommentType.MEMORY)
                .authorName("Ana")
                .authorLocation("Azul")
                .authorEmail("ana@example.com")
                .authorWantsContact(true)
                .body("Un recuerdo")
                .clientIpAddress("1.2.3.4")
                .status(CommentStatus.APPROVED)
                .build();
        comment.setCreateDate(now);

        when(commentRepository.findByContextTypeAndContextIdAndStatusOrderByCreateDateAsc(
                CommentContextType.FAMILY, "gennuso", CommentStatus.APPROVED))
                .thenReturn(List.of(comment));

        List<CommentDto> result = commentService.getApproved(CommentContextType.FAMILY, "gennuso");

        assertThat(result).hasSize(1);
        CommentDto dto = result.getFirst();
        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getCommentType()).isEqualTo(CommentType.MEMORY);
        assertThat(dto.getAuthorName()).isEqualTo("Ana");
        assertThat(dto.getAuthorLocation()).isEqualTo("Azul");
        assertThat(dto.getBody()).isEqualTo("Un recuerdo");
        assertThat(dto.getCreateDate()).isEqualTo(now);
        // CommentDto has no authorEmail/clientIpAddress/status fields — the public
        // endpoint must never leak them, enforced structurally by CommentDto's shape.
    }

    @Test
    void getApproved_noResults_returnsEmptyList() {
        when(commentRepository.findByContextTypeAndContextIdAndStatusOrderByCreateDateAsc(
                CommentContextType.STORY, "unknown-slug", CommentStatus.APPROVED))
                .thenReturn(List.of());

        List<CommentDto> result = commentService.getApproved(CommentContextType.STORY, "unknown-slug");

        assertThat(result).isEmpty();
    }

    @Test
    void getApproved_onlyQueriesApprovedStatus() {
        when(commentRepository.findByContextTypeAndContextIdAndStatusOrderByCreateDateAsc(
                CommentContextType.FAMILY, "gennuso", CommentStatus.APPROVED))
                .thenReturn(List.of());

        commentService.getApproved(CommentContextType.FAMILY, "gennuso");

        verify(commentRepository)
                .findByContextTypeAndContextIdAndStatusOrderByCreateDateAsc(CommentContextType.FAMILY, "gennuso", CommentStatus.APPROVED);
    }

    // ── getLatest ────────────────────────────────────────────────────────────

    @Test
    void getLatest_mapsAllFieldsToDto() {
        OffsetDateTime now = OffsetDateTime.now();
        UserComment comment = UserComment.builder()
                .id(42L)
                .contextType(CommentContextType.FAMILY)
                .contextId("gennuso")
                .commentType(CommentType.CORRECTION)
                .authorName("Ana")
                .authorLocation("Azul")
                .authorEmail("ana@example.com")
                .authorWantsContact(false)
                .body("Corrección de dato")
                .clientIpAddress("5.6.7.8")
                .status(CommentStatus.PENDING)
                .build();
        comment.setCreateDate(now);

        when(commentRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(comment)));

        List<CommentDetailsDto> result = commentService.getLatest(0, 10);

        assertThat(result).hasSize(1);
        CommentDetailsDto dto = result.getFirst();
        assertThat(dto.getId()).isEqualTo(42L);
        assertThat(dto.getContextType()).isEqualTo(CommentContextType.FAMILY);
        assertThat(dto.getContextId()).isEqualTo("gennuso");
        assertThat(dto.getAuthorEmail()).isEqualTo("ana@example.com");
        assertThat(dto.getClientIpAddress()).isEqualTo("5.6.7.8");
        assertThat(dto.getStatus()).isEqualTo(CommentStatus.PENDING);
        assertThat(dto.getCreateDate()).isEqualTo(now);
    }

    @Test
    void getLatest_cappedAtMaxPageSize() {
        when(commentRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        commentService.getLatest(0, Integer.MAX_VALUE);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(commentRepository).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1000);
    }

    @Test
    void getLatest_belowMaxPageSize_usesRequestedSize() {
        when(commentRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        commentService.getLatest(0, 25);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(commentRepository).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(25);
    }

    // ── updateStatus ─────────────────────────────────────────────────────────

    @Test
    void updateStatus_existingComment_updatesAndReturnsDto() {
        UserComment comment = UserComment.builder()
                .id(7L)
                .contextType(CommentContextType.STORY)
                .contextId("mi-historia")
                .commentType(CommentType.QUESTION)
                .authorName("Juan")
                .status(CommentStatus.PENDING)
                .build();

        when(commentRepository.findById(7L)).thenReturn(Optional.of(comment));

        CommentDetailsDto result = commentService.updateStatus(7L, CommentStatus.APPROVED);

        assertThat(result.getStatus()).isEqualTo(CommentStatus.APPROVED);
        assertThat(comment.getStatus()).isEqualTo(CommentStatus.APPROVED);
    }

    @Test
    void updateStatus_unknownId_throwsNotFound() {
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> commentService.updateStatus(999L, CommentStatus.REJECTED));

        assertThat(ex.getStatusCode().value()).isEqualTo(404);
    }

}
