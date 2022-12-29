package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.domain.EnrichedGedcom;

import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GedcomHolder {

    private final GedcomParsingService gedcomParsingService;
    private final GedcomAnalyzerProperties properties;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Getter
    private EnrichedGedcom gedcom;

    @PostConstruct
    public void postConstruct() {
        executorService.submit(() -> {
            try {
                gedcom = gedcomParsingService.parse(properties.getMainGedcomPath());
                log.info("Gedcom file loaded: {}", properties.getMainGedcomPath());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @PreDestroy
    public void preDestroy() {
        executorService.shutdownNow();
    }

}
