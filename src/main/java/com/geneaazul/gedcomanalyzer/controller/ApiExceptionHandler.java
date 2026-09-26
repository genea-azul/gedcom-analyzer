package com.geneaazul.gedcomanalyzer.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.annotation.Nullable;

/**
 * Renders MVC exceptions (validation failures, unreadable bodies, {@code ResponseStatusException}, …)
 * as RFC 9457 problem details with an extra {@code errorCode} property. The website maps
 * {@code errorCode} to a Spanish message, so the codes are part of the public API contract.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String INVALID_REQUEST = "INVALID-REQUEST";
    public static final String TOO_MANY_REQUESTS = "TOO-MANY-REQUESTS";
    public static final String NOT_FOUND = "NOT-FOUND";
    public static final String ERROR = "ERROR";

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        ProblemDetail problemDetail = body instanceof ProblemDetail pd
                ? pd
                : ProblemDetail.forStatus(statusCode);
        problemDetail.setProperty("errorCode", toErrorCode(statusCode));

        return super.handleExceptionInternal(ex, problemDetail, headers, statusCode, request);
    }

    static String toErrorCode(HttpStatusCode statusCode) {
        if (statusCode.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return TOO_MANY_REQUESTS;
        }
        if (statusCode.value() == HttpStatus.NOT_FOUND.value()) {
            return NOT_FOUND;
        }
        if (statusCode.is4xxClientError()) {
            return INVALID_REQUEST;
        }
        return ERROR;
    }

}
