package com.geneaazul.gedcomanalyzer.service.storage;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class GedcomHolder {

    private final StorageService storageService;
    private final ExecutorService gedcomHolderExecutorService;

    private EnrichedGedcom gedcom;

    public Optional<EnrichedGedcom> getGedcom() {
        return Optional.ofNullable(gedcom);
    }

    @PostConstruct
    public void postConstruct() {
        gedcomHolderExecutorService.submit(() -> {
            try {
                gedcom = storageService.getGedcom();
                log.info("Gedcom file loaded: {}", storageService.getGedcomName());
            } catch (Throwable e) {
                log.error("Error when loading gedcom file: {}", storageService.getGedcomName(), e);
            }
        });
    }

    @PreDestroy
    public void preDestroy() {
        gedcomHolderExecutorService.shutdownNow();
    }

}
