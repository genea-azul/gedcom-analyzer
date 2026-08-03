package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.domain.UserComment;
import com.geneaazul.gedcomanalyzer.model.dto.CommentContextType;
import com.geneaazul.gedcomanalyzer.model.dto.CommentDetailsDto;
import com.geneaazul.gedcomanalyzer.model.dto.CommentDto;
import com.geneaazul.gedcomanalyzer.model.dto.CommentStatus;
import com.geneaazul.gedcomanalyzer.model.dto.CommentSubmitDto;
import com.geneaazul.gedcomanalyzer.repository.UserCommentRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private static final int MAX_PAGE_SIZE = 1000;

    private final UserCommentRepository commentRepository;
    private final GedcomAnalyzerProperties properties;

    @Transactional(readOnly = true)
    public boolean isAllowedSubmit(@Nullable String clientIpAddress) {
        if (clientIpAddress == null) {
            return true;
        }
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime from = now.minusHours(properties.getMaxClientRequestsHoursThreshold());
        long count = commentRepository.countByClientIpAddressAndCreateDateBetween(clientIpAddress, from, now);
        boolean isSpecial = properties.getClientsWithSpecialThreshold().contains(clientIpAddress);
        return count < (isSpecial
                ? properties.getMaxClientRequestsCountSpecialThreshold()
                : properties.getMaxClientRequestsCountThreshold());
    }

    @Transactional
    public Long persistComment(CommentSubmitDto dto, @Nullable String clientIpAddress) {
        UserComment comment = UserComment.builder()
                .contextType(dto.getContextType())
                .contextId(dto.getContextId())
                .commentType(dto.getCommentType())
                .authorName(dto.getAuthorName())
                .authorLocation(dto.getAuthorLocation())
                .authorEmail(dto.getAuthorEmail())
                .authorWantsContact(dto.isAuthorWantsContact())
                .body(dto.getBody())
                .clientIpAddress(clientIpAddress)
                .build();
        Long id = commentRepository.save(comment).getId();
        notifyIfReconnectionCandidate(dto);
        return id;
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getApproved(CommentContextType contextType, String contextId) {
        return commentRepository
                .findByContextTypeAndContextIdAndStatusOrderByCreateDateAsc(contextType, contextId, CommentStatus.APPROVED)
                .stream()
                .map(c -> CommentDto.builder()
                        .id(c.getId())
                        .commentType(c.getCommentType())
                        .authorName(c.getAuthorName())
                        .authorLocation(c.getAuthorLocation())
                        .body(c.getBody())
                        .createDate(c.getCreateDate())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommentDetailsDto> getLatest(int page, int size, @Nullable CommentStatus status, @Nullable String linkBaseUrl) {
        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<UserComment> results = status != null
                ? commentRepository.findByStatus(status, pageable)
                : commentRepository.findAll(pageable);
        return results
                .stream()
                .map(c -> CommentDetailsDto.builder()
                        .id(c.getId())
                        .contextType(c.getContextType())
                        .contextId(c.getContextId())
                        .commentType(c.getCommentType())
                        .authorName(c.getAuthorName())
                        .authorLocation(c.getAuthorLocation())
                        .authorEmail(c.getAuthorEmail())
                        .authorWantsContact(c.getAuthorWantsContact())
                        .body(c.getBody())
                        .status(c.getStatus())
                        .createDate(c.getCreateDate())
                        .clientIpAddress(c.getClientIpAddress())
                        .build())
                .peek(details -> {
                    if (linkBaseUrl != null && details.getStatus() != CommentStatus.APPROVED) {
                        details.setMarkApprovedLink(linkBaseUrl + "/api/admin/comments/" + details.getId() + "/status?status=APPROVED");
                    }
                    if (linkBaseUrl != null && details.getStatus() != CommentStatus.REJECTED) {
                        details.setMarkRejectedLink(linkBaseUrl + "/api/admin/comments/" + details.getId() + "/status?status=REJECTED");
                    }
                })
                .toList();
    }

    @Transactional
    public CommentDetailsDto updateStatus(Long id, CommentStatus status) {
        UserComment comment = commentRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "UserComment not found id=" + id));
        comment.setStatus(status);
        return CommentDetailsDto.builder()
                .id(comment.getId())
                .contextType(comment.getContextType())
                .contextId(comment.getContextId())
                .commentType(comment.getCommentType())
                .authorName(comment.getAuthorName())
                .authorLocation(comment.getAuthorLocation())
                .authorEmail(comment.getAuthorEmail())
                .authorWantsContact(comment.getAuthorWantsContact())
                .body(comment.getBody())
                .status(comment.getStatus())
                .createDate(comment.getCreateDate())
                .clientIpAddress(comment.getClientIpAddress())
                .build();
    }

    private void notifyIfReconnectionCandidate(CommentSubmitDto dto) {
        if (!dto.isAuthorWantsContact() || dto.getAuthorEmail() == null) {
            return;
        }
        List<UserComment> others = commentRepository
                .findByContextTypeAndContextIdAndAuthorWantsContactTrueAndAuthorEmailIsNotNullAndStatus(
                        dto.getContextType(), dto.getContextId(), CommentStatus.APPROVED);
        if (!others.isEmpty()) {
            log.info("Reconnection candidate [ contextType={}, contextId={}, authorName={}, otherCount={} ]",
                    dto.getContextType(), dto.getContextId(), dto.getAuthorName(), others.size());
        }
    }

}
