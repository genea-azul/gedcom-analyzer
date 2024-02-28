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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class StorageConfig {

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
        return Executors.newFixedThreadPool(2);
    }

    @Configuration
    @RequiredArgsConstructor
    public static class SubConfig {

        private final GedcomHolder gedcomHolder;
        private final ExecutorService executorService;

        @Value("${gedcom.reload-delay-in-secs:8}")
        private int gedcomReloadDelayInSecs;

        @PostConstruct
        public void postConstruct() {
            executorService.submit(() -> ThreadUtils.sleepSecondsAndThen(
                    gedcomReloadDelayInSecs,
                    () -> gedcomHolder.reloadFromStorage(false)));
        }
    }

}
