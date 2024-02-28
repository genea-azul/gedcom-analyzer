package com.geneaazul.gedcomanalyzer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.ExecutorServiceAdapter;

import java.util.concurrent.ExecutorService;

@Configuration
public class GedcomAnalyzerTestsConfig {

    @Bean
    public ExecutorService executorService() {
        return new ExecutorServiceAdapter(Runnable::run);
    }

}
