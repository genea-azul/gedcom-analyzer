package com.geneaazul.gedcomanalyzer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class FamilyTreeConfig {

    @Bean
    public ExecutorService familyTreeExecutorService() {
        return Executors.newSingleThreadExecutor();
    }

}
