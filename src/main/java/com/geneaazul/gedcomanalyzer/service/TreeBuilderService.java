package com.geneaazul.gedcomanalyzer.service;

import tools.jackson.databind.ObjectMapper;
import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.domain.TreeBuilderSubmission;
import com.geneaazul.gedcomanalyzer.model.dto.TreeBuilderSubmissionDetailsDto;
import com.geneaazul.gedcomanalyzer.model.dto.TreeBuilderSubmitDto;
import com.geneaazul.gedcomanalyzer.repository.TreeBuilderSubmissionRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.annotation.Nullable;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TreeBuilderService {

    private static final int MAX_PAGE_SIZE = 1000;

    private final TreeBuilderSubmissionRepository submissionRepository;
    private final GedcomAnalyzerProperties properties;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public boolean isAllowedSubmit(@Nullable String clientIpAddress) {
        if (clientIpAddress == null) {
            return true;
        }
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime from = now.minusHours(properties.getMaxClientRequestsHoursThreshold());
        long count = submissionRepository.countByClientIpAddressAndCreateDateBetween(clientIpAddress, from, now);
        // Tree-builder submissions are counted independently from family/connection searches —
        // they are a distinct, heavier operation so they carry their own quota.
        boolean isSpecial = properties.getClientsWithSpecialThreshold().contains(clientIpAddress);
        return count < (isSpecial
                ? properties.getMaxClientRequestsCountSpecialThreshold()
                : properties.getMaxClientRequestsCountThreshold());
    }

    @Transactional(readOnly = true)
    public List<TreeBuilderSubmissionDetailsDto> getLatest(int page, int size) {
        size = Math.min(size, MAX_PAGE_SIZE);
        return submissionRepository
                .findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")))
                .stream()
                .map(s -> TreeBuilderSubmissionDetailsDto.builder()
                        .id(s.getId())
                        .contact(s.getContact())
                        .createDate(s.getCreateDate())
                        .clientIpAddress(s.getClientIpAddress())
                        .payload(s.getPayload())
                        .build())
                .toList();
    }

    @Transactional
    public Long persistSubmission(TreeBuilderSubmitDto submitDto, @Nullable String clientIpAddress) {
        String payload = objectMapper.writeValueAsString(submitDto);
        TreeBuilderSubmission submission = TreeBuilderSubmission.builder()
                .payload(payload)
                .contact(submitDto.getContact())
                .clientIpAddress(clientIpAddress)
                .build();
        return submissionRepository.save(submission).getId();
    }

}
