package com.geneaazul.gedcomanalyzer.config;

import com.geneaazul.gedcomanalyzer.model.FamilyTreeType;
import com.geneaazul.gedcomanalyzer.service.familytree.FamilyTreeService;
import com.geneaazul.gedcomanalyzer.service.familytree.NetworkFamilyTreeService;
import com.geneaazul.gedcomanalyzer.service.familytree.PlainFamilyTreeService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class FamilyTreeConfig {

    @Bean
    public Map<FamilyTreeType, FamilyTreeService> familyTreeServiceByType(
            PlainFamilyTreeService plainFamilyTreeService,
            NetworkFamilyTreeService networkFamilyTreeService) {
        return Map.of(
                FamilyTreeType.PLAIN, plainFamilyTreeService,
                FamilyTreeType.NETWORK, networkFamilyTreeService);
    }

    @Bean
    @Profile("!test")
    public ExecutorService familyTreeExecutorService() {
        return Executors.newSingleThreadExecutor();
    }

}
