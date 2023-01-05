package com.geneaazul.gedcomanalyzer.config;

import com.geneaazul.gedcomanalyzer.helper.ExecutorServiceMock;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.service.storage.StorageService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GedcomAnalyzerTestsConfig {

    @Bean
    public GedcomHolder gedcomHolderTest(StorageService storageService) {
        return new GedcomHolder(storageService, new ExecutorServiceMock());
    }

}
