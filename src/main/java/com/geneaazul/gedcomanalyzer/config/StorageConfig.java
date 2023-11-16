package com.geneaazul.gedcomanalyzer.config;

import com.geneaazul.gedcomanalyzer.service.GedcomParsingService;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.service.storage.GoogleDriveStorageService;
import com.geneaazul.gedcomanalyzer.service.storage.LocalStorageService;
import com.geneaazul.gedcomanalyzer.service.storage.StorageService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    public GedcomHolder gedcomHolder(
            StorageService storageService,
            ExecutorService singleThreadExecutorService) {
        return new GedcomHolder(storageService, singleThreadExecutorService);
    }

    @Bean
    @Profile("!test")
    public ExecutorService singleThreadExecutorService() {
        return Executors.newSingleThreadExecutor();
    }

}
