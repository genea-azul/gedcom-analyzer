package com.geneaazul.gedcomanalyzer.service.storage;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ReloadInProgressException extends ResponseStatusException {

    public ReloadInProgressException() {
        super(HttpStatus.CONFLICT, "A gedcom reload is already in progress.");
    }

}
