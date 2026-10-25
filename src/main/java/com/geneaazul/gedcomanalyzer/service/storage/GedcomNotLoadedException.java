package com.geneaazul.gedcomanalyzer.service.storage;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * The GEDCOM is still loading (cold start or admin reload): rendered as 503 with Retry-After.
 */
public class GedcomNotLoadedException extends ResponseStatusException {

    static final String RETRY_AFTER_SECONDS = "30";

    public GedcomNotLoadedException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Server is starting, please try again.");
    }

    @Override
    public HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS);
        return headers;
    }

}
