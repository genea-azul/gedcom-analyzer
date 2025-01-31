package com.geneaazul.gedcomanalyzer;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;

@SpringBootTest
@EnableConfigurationProperties
@ActiveProfiles("test")
public class ApplicationTests {

    @Test
    public void contextLoads() {
    }

}
