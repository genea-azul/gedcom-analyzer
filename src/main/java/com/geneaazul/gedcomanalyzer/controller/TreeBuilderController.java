package com.geneaazul.gedcomanalyzer.controller;

import com.geneaazul.gedcomanalyzer.model.dto.TreeBuilderSubmitDto;
import com.geneaazul.gedcomanalyzer.model.dto.TreeBuilderSubmitResultDto;
import com.geneaazul.gedcomanalyzer.service.TreeBuilderService;
import com.geneaazul.gedcomanalyzer.utils.InetAddressUtils;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/tree-builder")
@RequiredArgsConstructor
public class TreeBuilderController {

    private final TreeBuilderService treeBuilderService;

    @PostMapping("/submit")
    @CrossOrigin(originPatterns = { "http://geneaazul.com.ar:[*]", "https://geneaazul.com.ar:[*]", "http://*.geneaazul.com.ar:[*]", "https://*.geneaazul.com.ar:[*]" })
    public TreeBuilderSubmitResultDto submit(
            @Valid @RequestBody TreeBuilderSubmitDto submitDto,
            HttpServletRequest request) {

        Optional<String> clientIpAddress = InetAddressUtils.getRemoteAddress(request);

        if (!treeBuilderService.isAllowedSubmit(clientIpAddress.orElse(null))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        }

        Long submissionId = treeBuilderService.persistSubmission(submitDto, clientIpAddress.orElse(null));

        log.info("Tree builder submission [ id={}, contact={}, httpRequestId={} ]",
                submissionId,
                submitDto.getContact(),
                request.getRequestId());

        return TreeBuilderSubmitResultDto.builder()
                .submissionId(submissionId)
                .build();
    }

}
