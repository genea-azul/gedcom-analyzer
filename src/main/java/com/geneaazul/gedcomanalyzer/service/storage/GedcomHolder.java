package com.geneaazul.gedcomanalyzer.service.storage;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("ResultOfMethodCallIgnored")
public class GedcomHolder {

    private final StorageService storageService;
    private final ExecutorService gedcomHolderExecutorService;

    private final BlockingQueue<EnrichedGedcom> gedcomQueue = new LinkedBlockingQueue<>();

    public EnrichedGedcom getGedcom() {
        try {
            EnrichedGedcom gedcom = gedcomQueue.poll(10, TimeUnit.SECONDS);

            if (gedcom != null) {
                gedcomQueue.offer(gedcom);
                return gedcom;
            }

        } catch (InterruptedException e) {
            // do nothing
        }

        throw new IllegalStateException("Server is starting, please try again.");
    }

    @PostConstruct
    public void postConstruct() {
        gedcomHolderExecutorService.submit(() -> {
            try {
                EnrichedGedcom gedcom = storageService.getGedcom();
                gedcomQueue.offer(gedcom);

                log.info("Gedcom file loaded: {}", storageService.getGedcomName());

            } catch (Throwable e) {
                log.error("Error when loading gedcom file: {}", storageService.getGedcomName(), e);
            }
        });
    }

    @PreDestroy
    public void preDestroy() {
        gedcomQueue.clear();
        gedcomHolderExecutorService.shutdownNow();
    }

}
