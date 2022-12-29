package com.geneaazul.gedcomanalyzer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@EnableConfigurationProperties
@ActiveProfiles("test")
class GedcomAnalyzerApplicationTests {

    @Test
    public void contextLoads() {
    }

}
