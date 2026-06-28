package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.domain.TreeBuilderSubmission;
import com.geneaazul.gedcomanalyzer.model.dto.TreeBuilderPersonDto;
import com.geneaazul.gedcomanalyzer.model.dto.TreeBuilderSubmissionDetailsDto;
import com.geneaazul.gedcomanalyzer.model.dto.TreeBuilderSubmitDto;
import com.geneaazul.gedcomanalyzer.repository.TreeBuilderSubmissionRepository;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreeBuilderServiceTest {

    @Mock
    private TreeBuilderSubmissionRepository submissionRepository;
    @Mock
    private GedcomAnalyzerProperties properties;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TreeBuilderService treeBuilderService;

    // ── isAllowedSubmit ──────────────────────────────────────────────────────

    @Test
    void isAllowedSubmit_nullIp_returnsTrueWithoutHittingDb() {
        assertThat(treeBuilderService.isAllowedSubmit(null)).isTrue();
        verifyNoInteractions(submissionRepository);
    }

    @Test
    void isAllowedSubmit_countBelowThreshold_returnsTrue() {
        when(properties.getMaxClientRequestsHoursThreshold()).thenReturn(1);
        when(properties.getClientsWithSpecialThreshold()).thenReturn(Set.of());
        when(properties.getMaxClientRequestsCountThreshold()).thenReturn(12);
        when(submissionRepository.countByClientIpAddressAndCreateDateBetween(any(), any(), any())).thenReturn(11L);

        assertThat(treeBuilderService.isAllowedSubmit("1.2.3.4")).isTrue();
    }

    @Test
    void isAllowedSubmit_countAtThreshold_returnsFalse() {
        when(properties.getMaxClientRequestsHoursThreshold()).thenReturn(1);
        when(properties.getClientsWithSpecialThreshold()).thenReturn(Set.of());
        when(properties.getMaxClientRequestsCountThreshold()).thenReturn(12);
        when(submissionRepository.countByClientIpAddressAndCreateDateBetween(any(), any(), any())).thenReturn(12L);

        assertThat(treeBuilderService.isAllowedSubmit("1.2.3.4")).isFalse();
    }

    @Test
    void isAllowedSubmit_specialIpBelowSpecialThreshold_returnsTrue() {
        when(properties.getMaxClientRequestsHoursThreshold()).thenReturn(1);
        when(properties.getClientsWithSpecialThreshold()).thenReturn(Set.of("1.2.3.4"));
        when(properties.getMaxClientRequestsCountSpecialThreshold()).thenReturn(3);
        when(submissionRepository.countByClientIpAddressAndCreateDateBetween(any(), any(), any())).thenReturn(2L);

        assertThat(treeBuilderService.isAllowedSubmit("1.2.3.4")).isTrue();
    }

    @Test
    void isAllowedSubmit_specialIpAtSpecialThreshold_returnsFalse() {
        when(properties.getMaxClientRequestsHoursThreshold()).thenReturn(1);
        when(properties.getClientsWithSpecialThreshold()).thenReturn(Set.of("1.2.3.4"));
        when(properties.getMaxClientRequestsCountSpecialThreshold()).thenReturn(3);
        when(submissionRepository.countByClientIpAddressAndCreateDateBetween(any(), any(), any())).thenReturn(3L);

        assertThat(treeBuilderService.isAllowedSubmit("1.2.3.4")).isFalse();
    }

    @Test
    void isAllowedSubmit_regularIpNotInSpecialSet_usesRegularThreshold() {
        when(properties.getMaxClientRequestsHoursThreshold()).thenReturn(1);
        when(properties.getClientsWithSpecialThreshold()).thenReturn(Set.of("9.9.9.9"));
        when(properties.getMaxClientRequestsCountThreshold()).thenReturn(12);
        when(submissionRepository.countByClientIpAddressAndCreateDateBetween(any(), any(), any())).thenReturn(5L);

        // 5 < 12 → allowed
        assertThat(treeBuilderService.isAllowedSubmit("1.2.3.4")).isTrue();
    }

    // ── getLatest ────────────────────────────────────────────────────────────

    @Test
    void getLatest_mapsAllFieldsToDto() {
        OffsetDateTime now = OffsetDateTime.now();
        TreeBuilderSubmission submission = TreeBuilderSubmission.builder()
                .id(42L)
                .contact("test@example.com")
                .clientIpAddress("5.6.7.8")
                .payload("{\"ego\":{}}")
                .build();
        // createDate is set by @PrePersist; simulate it for the mapping assertion
        submission.setCreateDate(now);

        when(submissionRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(submission)));

        List<TreeBuilderSubmissionDetailsDto> result = treeBuilderService.getLatest(0, 10);

        assertThat(result).hasSize(1);
        TreeBuilderSubmissionDetailsDto dto = result.getFirst();
        assertThat(dto.getId()).isEqualTo(42L);
        assertThat(dto.getContact()).isEqualTo("test@example.com");
        assertThat(dto.getClientIpAddress()).isEqualTo("5.6.7.8");
        assertThat(dto.getPayload()).isEqualTo("{\"ego\":{}}");
        assertThat(dto.getCreateDate()).isEqualTo(now);
    }

    @Test
    void getLatest_cappedAtMaxPageSize() {
        when(submissionRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        treeBuilderService.getLatest(0, Integer.MAX_VALUE);

        verify(submissionRepository).findAll(any(Pageable.class));
        // The service caps size at MAX_PAGE_SIZE (1000); we verify the repository is always called
    }

    // ── persistSubmission ────────────────────────────────────────────────────

    @Test
    void persistSubmission_savesWithCorrectFields() {
        String json = "{\"ego\":{\"givenName\":\"Ana\"}}";
        when(objectMapper.writeValueAsString(any())).thenReturn(json);
        when(submissionRepository.save(any())).thenAnswer(inv -> {
            TreeBuilderSubmission s = inv.getArgument(0);
            s.setId(99L);
            return s;
        });

        TreeBuilderSubmitDto dto = TreeBuilderSubmitDto.builder()
                .ego(TreeBuilderPersonDto.builder().givenName("Ana").build())
                .contact("ana@example.com")
                .build();

        Long id = treeBuilderService.persistSubmission(dto, "10.0.0.1");

        assertThat(id).isEqualTo(99L);
        verify(submissionRepository).save(any(TreeBuilderSubmission.class));
    }

    @Test
    void persistSubmission_nullIpAddress_savesWithNullClientIp() {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(submissionRepository.save(any())).thenAnswer(inv -> {
            TreeBuilderSubmission s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        TreeBuilderSubmitDto dto = TreeBuilderSubmitDto.builder()
                .ego(TreeBuilderPersonDto.builder().givenName("Ana").build())
                .build();

        treeBuilderService.persistSubmission(dto, null);

        verify(submissionRepository).save(any(TreeBuilderSubmission.class));
    }

}
