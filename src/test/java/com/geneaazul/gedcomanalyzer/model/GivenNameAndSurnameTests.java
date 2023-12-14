package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnableConfigurationProperties
@ActiveProfiles("test")
public class GivenNameAndSurnameTests {

    @Autowired
    private GedcomAnalyzerProperties properties;

    @Test
    public void testMatchesWithAka() {
        // "Valentina" search will only match the a.k.a. value: Валянціна Pérez (Valyantsina Pérez)
        GivenNameAndSurname givenNameAndSurname = GivenNameAndSurname.of("Valeria", "Pérez", "Валянціна Pérez", SexType.F, properties);
        GivenNameAndSurname other = GivenNameAndSurname.of("Valentina", "Pérez", SexType.F, properties);
        assertThat(givenNameAndSurname.matches(other)).isTrue();
    }

    @Test
    public void testMatchesSurnameWhenMissingAnyGivenName() {
        GivenNameAndSurname givenNameAndSurname = GivenNameAndSurname.of("Valeria", "Pérez", SexType.F, properties);
        GivenNameAndSurname other = GivenNameAndSurname.of(null, "Pérez", SexType.F, properties);
        assertThat(givenNameAndSurname.matchesSurnameWhenMissingAnyGivenName(other)).isTrue();
    }

}
