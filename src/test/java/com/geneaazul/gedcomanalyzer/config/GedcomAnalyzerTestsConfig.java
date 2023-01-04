package com.geneaazul.gedcomanalyzer.config;

import com.geneaazul.gedcomanalyzer.helper.ExecutorServiceMock;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.service.storage.StorageService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
public class GedcomAnalyzerTestsConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public GedcomHolder gedcomHolderTest(StorageService storageService) {
        return new GedcomHolder(storageService, new ExecutorServiceMock());
    }

}
