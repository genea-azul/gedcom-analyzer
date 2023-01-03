package com.geneaazul.gedcomanalyzer.config;

import com.geneaazul.gedcomanalyzer.service.GedcomParsingService;
import com.geneaazul.gedcomanalyzer.service.storage.GoogleCloudStorageService;
import com.geneaazul.gedcomanalyzer.service.storage.LocalStorageService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

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
    public GoogleCloudStorageService googleCloudStorageService(
            GedcomParsingService gedcomParsingService,
            GedcomAnalyzerProperties properties) {
        return new GoogleCloudStorageService(
                gedcomParsingService,
                properties);
    }

}
