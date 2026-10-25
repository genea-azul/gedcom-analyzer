package com.geneaazul.gedcomanalyzer.config;

import com.geneaazul.gedcomanalyzer.service.GedcomParsingService;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.service.storage.GoogleDriveStorageService;
import com.geneaazul.gedcomanalyzer.service.storage.LocalStorageService;
import com.geneaazul.gedcomanalyzer.utils.ThreadUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class StorageConfig {

    private static final int EXECUTOR_QUEUE_CAPACITY = 200;

    @Bean
    @ConditionalOnProperty(name = "gedcom-storage-google-drive.enabled", havingValue = "true")
    public GoogleDriveStorageService googleDriveStorageService(
            GedcomParsingService gedcomParsingService,
            GedcomAnalyzerProperties properties) {

        log.info("Using storage service: Google Drive");

        return new GoogleDriveStorageService(
                new LocalStorageService(
                        gedcomParsingService,
                        properties),
                properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = { "googleDriveStorageService" })
    public LocalStorageService localStorageService(
            GedcomParsingService gedcomParsingService,
            GedcomAnalyzerProperties properties) {

        log.info("Using storage service: local");

        return new LocalStorageService(
                gedcomParsingService,
                properties);
    }

    @Bean
    @Profile("!test")
    public ExecutorService executorService() {
        // Bounded: every search queues exports, and they can always be generated on demand later
        return new ThreadPoolExecutor(
                2,
                2,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(EXECUTOR_QUEUE_CAPACITY),
                (task, executor) -> log.warn("Background task queue is full, dropping a task"));
    }

    @Configuration
    @RequiredArgsConstructor
    public static class SubConfig {

        private final GedcomHolder gedcomHolder;
        private final ExecutorService executorService;

        // Loading right away overlaps the GEDCOM parse with the rest of startup (a fixed 8 s delay used to add to every cold start)
        @Value("${gedcom.reload-delay-in-secs:0}")
        private int gedcomReloadDelayInSecs;

        @PostConstruct
        public void postConstruct() {
            executorService.submit(() -> ThreadUtils.sleepSecondsAndThen(
                    gedcomReloadDelayInSecs,
                    () -> gedcomHolder.reloadFromStorage(false)));
        }
    }

}
