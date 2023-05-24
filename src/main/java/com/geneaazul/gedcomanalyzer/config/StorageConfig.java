package com.geneaazul.gedcomanalyzer.config;

import com.geneaazul.gedcomanalyzer.service.GedcomParsingService;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.service.storage.GoogleDriveStorageService;
import com.geneaazul.gedcomanalyzer.service.storage.LocalStorageService;
import com.geneaazul.gedcomanalyzer.service.storage.StorageService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.Executors;

@Configuration
public class StorageConfig {

    @Bean
    @Profile("!prod")
    public LocalStorageService localStorageService(
            GedcomParsingService gedcomParsingService,
            GedcomAnalyzerProperties properties) {
        return new LocalStorageService(
                gedcomParsingService,
                properties);
    }

    @Bean
    @Profile("prod")
    public GoogleDriveStorageService googleDriveStorageService(
            GedcomParsingService gedcomParsingService,
            GedcomAnalyzerProperties properties) {
        return new GoogleDriveStorageService(
                new LocalStorageService(
                        gedcomParsingService,
                        properties),
                properties);
    }

    @Bean
    @Profile("!test")
    public GedcomHolder gedcomHolder(StorageService storageService) {
        return new GedcomHolder(storageService, Executors.newSingleThreadExecutor());
    }

}
