package com.geneaazul.gedcomanalyzer.config;

import com.geneaazul.gedcomanalyzer.model.NameAndSex;
import com.geneaazul.gedcomanalyzer.utils.EntryStreamTestUtils;
import com.geneaazul.gedcomanalyzer.utils.SearchUtils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.stream.Stream;

import static com.geneaazul.gedcomanalyzer.utils.EntryStreamTestUtils.assertEqualMappingKey;
import static com.geneaazul.gedcomanalyzer.utils.EntryStreamUtils.*;

@SpringBootTest
@EnableConfigurationProperties
@ActiveProfiles("test")
public class GedcomAnalyzerPropertiesTest {

    @Autowired
    private GedcomAnalyzerProperties properties;

    @Test
    public void testPropertyNameNormalizedM() {
        properties.getNormalizedGivenNamesMap()
                .entrySet()
                .stream()
                .flatMap(entry -> Stream.of(
                        entry.getKey(),
                        new NameAndSex(entry.getValue(), entry.getKey().sex())))
                .distinct()
                .map(mappingEntry(NameAndSex::name))
                .map(entryValueMapper(SearchUtils::simplifyName))
                .map(entryValueMapper((nameAndSex, givenName) -> SearchUtils.normalizeGivenName(givenName, nameAndSex.sex(), Map.of())))
                .forEach(entry -> assertEqualMappingKey(entry, NameAndSex::name));
    }

    @Test
    public void testPropertySurnameNormalizedMap() {
        Stream.concat(
                properties.getNormalizedSurnamesMap()
                        .keySet()
                        .stream(),
                properties.getNormalizedSurnamesMap()
                        .values()
                        .stream())
                .map(unaryEntry())
                .map(entryValueMapper(SearchUtils::simplifyName))
                .map(entryValueMapper(surname -> SearchUtils.normalizeSurnameToMainWord(surname, Map.of())))
                .forEach(EntryStreamTestUtils::assertEqualKeyValue);
    }

}
