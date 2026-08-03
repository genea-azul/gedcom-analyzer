package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.model.dto.CommentContextType;
import com.geneaazul.gedcomanalyzer.model.dto.CommentDto;
import com.geneaazul.gedcomanalyzer.model.dto.CommentSubmitDto;
import com.geneaazul.gedcomanalyzer.model.dto.CommentSubmitResultDto;
import com.geneaazul.gedcomanalyzer.service.CommentService;
import com.geneaazul.gedcomanalyzer.utils.InetAddressUtils;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @CrossOrigin(originPatterns = { "http://geneaazul.com.ar:[*]", "https://geneaazul.com.ar:[*]", "http://*.geneaazul.com.ar:[*]", "https://*.geneaazul.com.ar:[*]" })
    public CommentSubmitResultDto submit(
            @Valid @RequestBody CommentSubmitDto submitDto,
            HttpServletRequest request) {

        Optional<String> clientIpAddress = InetAddressUtils.getRemoteAddress(request);

        if (!commentService.isAllowedSubmit(clientIpAddress.orElse(null))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        }

        Long id = commentService.persistComment(submitDto, clientIpAddress.orElse(null));

        log.info("Comment submitted [ id={}, contextType={}, contextId={}, httpRequestId={} ]",
                id,
                submitDto.getContextType(),
                submitDto.getContextId(),
                request.getRequestId());

        return CommentSubmitResultDto.builder()
                .commentId(id)
                .build();
    }

    @GetMapping
    @CrossOrigin(originPatterns = { "http://geneaazul.com.ar:[*]", "https://geneaazul.com.ar:[*]", "http://*.geneaazul.com.ar:[*]", "https://*.geneaazul.com.ar:[*]" })
    public List<CommentDto> getApproved(
            @RequestParam CommentContextType contextType,
            @RequestParam String contextId) {
        return commentService.getApproved(contextType, contextId);
    }

}
