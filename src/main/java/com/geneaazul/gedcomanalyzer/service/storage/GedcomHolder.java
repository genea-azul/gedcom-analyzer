package com.geneaazul.gedcomanalyzer.service.storage;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GedcomHolder {

    private final StorageService storageService;

    private final LinkedBlockingQueue<EnrichedGedcom> gedcomQueue = new LinkedBlockingQueue<>();

    public EnrichedGedcom getGedcom() {
        try {
            EnrichedGedcom gedcom = gedcomQueue.poll(30, TimeUnit.SECONDS);

            if (gedcom != null) {
                gedcomQueue.offer(gedcom);
                return gedcom;
            }

        } catch (InterruptedException e) {
            // do nothing
        }

        throw new IllegalStateException("Server is starting, please try again.");
    }

    public void reloadFromStorage(boolean refreshCachedGedcom) {
        try {
            Instant start = Instant.now();
            gedcomQueue.clear();
            EnrichedGedcom gedcom = storageService.getGedcom(refreshCachedGedcom);
            gedcomQueue.offer(gedcom);

            log.info("Gedcom file loaded: {} - total time: {}", storageService.getGedcomName(), Duration.between(start, Instant.now()));

        } catch (Throwable e) {
            log.error("Error when loading gedcom file: {}", storageService.getGedcomName(), e);
        }
    }

    @PreDestroy
    public void preDestroy() {
        gedcomQueue.clear();
    }

}
