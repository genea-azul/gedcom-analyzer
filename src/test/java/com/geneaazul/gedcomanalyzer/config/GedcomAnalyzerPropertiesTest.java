package com.geneaazul.gedcomanalyzer.config;

import com.geneaazul.gedcomanalyzer.model.NameAndSex;
import com.geneaazul.gedcomanalyzer.utils.EntryStreamTestUtils;
import com.geneaazul.gedcomanalyzer.utils.NameUtils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.stream.Stream;

import static com.geneaazul.gedcomanalyzer.utils.EntryStreamTestUtils.assertEqualMappingKey;
import static com.geneaazul.gedcomanalyzer.utils.EntryStreamTestUtils.assertNotEqualMappingKey;
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
                .peek(entry -> assertNotEqualMappingKey(entry, NameAndSex::name))
                .flatMap(entry -> Stream.of(
                        entry.getKey(),
                        new NameAndSex(entry.getValue(), entry.getKey().sex())))
                .distinct()
                .map(mappingEntry(NameAndSex::name))
                .map(entryValueMapper(NameUtils::simplifyName))
                .map(entryValueMapper((nameAndSex, givenName) -> NameUtils.normalizeGivenName(givenName, nameAndSex.sex(), Map.of())))
                .forEach(entry -> assertEqualMappingKey(entry, NameAndSex::name));
    }

    @Test
    public void testPropertySurnameNormalizedMap() {

        properties.getNormalizedSurnamesMap()
                .entrySet()
                .forEach(EntryStreamTestUtils::assertNotEqualKeyValue);

        Stream.concat(
                properties.getNormalizedSurnamesMap()
                        .keySet()
                        .stream(),
                properties.getNormalizedSurnamesMap()
                        .values()
                        .stream())
                .map(unaryEntry())
                .map(entryValueMapper(NameUtils::simplifyName))
                .map(entryValueMapper(surname -> NameUtils.normalizeSurnameToMainWord(surname, Map.of())))
                .forEach(EntryStreamTestUtils::assertEqualKeyValue);
    }

}
