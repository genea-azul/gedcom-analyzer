package com.geneaazul.gedcomanalyzer.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Person;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@EnableConfigurationProperties
@ActiveProfiles("test")
public class SearchUtilsTests {

    @Autowired
    private GedcomAnalyzerProperties properties;

    @Test
    public void testGetNormalizedGivenName() {
        Name name = new Name();
        name.setSurname("Smith");
        Person person = new Person();
        person.addName(name);

        name.setGiven(" ? d'  (YíçbjéjizAEU) - Domenico ");
        assertThat(PersonUtils.getNormalizedGivenName(person, SexType.M, properties.getNormalizedGivenNamesMap()))
                .get()
                .satisfies(givenName -> {
                    assertThat(givenName.value()).isEqualTo("? d'  (YíçbjéjizAEU) - Domenico");
                    assertThat(givenName.normalized()).isEqualTo("d yicbjejizaeu domingo");
                    assertThat(givenName.wordsCount()).isEqualTo(3);
                    assertThat(givenName.searchPattern().toString()).isEqualTo("^(?=.*\\bd\\b)(?=.*\\byicbjejizaeu\\b)(?=.*\\bdomingo\\b).*$");
                });

        name.setGiven("Francescantonio");
        assertThat(PersonUtils.getNormalizedGivenName(person, SexType.M, properties.getNormalizedGivenNamesMap()))
                .get()
                .satisfies(givenName -> {
                    assertThat(givenName.value()).isEqualTo("Francescantonio");
                    assertThat(givenName.normalized()).isEqualTo("francisco antonio");
                    assertThat(givenName.wordsCount()).isEqualTo(2);
                    assertThat(givenName.searchPattern().toString()).isEqualTo("^(?=.*\\bfrancisco\\b)(?=.*\\bantonio\\b).*$");
                });

        name.setGiven("Elizabeth");
        assertThat(PersonUtils.getNormalizedGivenName(person, SexType.F, properties.getNormalizedGivenNamesMap()))
                .get()
                .satisfies(givenName -> {
                    assertThat(givenName.value()).isEqualTo("Elizabeth");
                    assertThat(givenName.normalized()).isEqualTo("elisa isabel");
                    assertThat(givenName.wordsCount()).isEqualTo(2);
                    assertThat(givenName.searchPattern().toString()).isEqualTo("^(?=.*\\belisa\\b)(?=.*\\bisabel\\b).*$");
                });

        name.setGiven("Валянціна");
        assertThat(PersonUtils.getNormalizedGivenName(person, SexType.F, properties.getNormalizedGivenNamesMap()))
                .get()
                .satisfies(givenName -> {
                    assertThat(givenName.value()).isEqualTo("Валянціна");
                    assertThat(givenName.normalized()).isEqualTo("valentina");
                    assertThat(givenName.wordsCount()).isEqualTo(1);
                    assertThat(givenName.searchPattern().toString()).isEqualTo("\\bvalentina\\b");
                });
    }

    @Test
    public void testGetNormalizedSurnameMainWord() {
        Name name = new Name();
        name.setGiven("?");
        Person person = new Person();
        person.addName(name);

        name.setSurname("Dí YannijébeZçlli Rago");
        assertThat(PersonUtils.getNormalizedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("Dí YannijébeZçlli Rago");
                    assertThat(surname.simplifiedMainWord()).isEqualTo("dianigevescli");
                    assertThat(surname.normalizedMainWord()).isEqualTo("dianigevescl_");
                });

        name.setSurname("La Camera");
        assertThat(PersonUtils.getNormalizedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("La Camera");
                    assertThat(surname.simplifiedMainWord()).isEqualTo("lacamera");
                    assertThat(surname.normalizedMainWord()).isEqualTo("lacamar_");
                });

        name.setSurname("Mac Cabe");
        assertThat(PersonUtils.getNormalizedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("Mac Cabe");
                    assertThat(surname.simplifiedMainWord()).isEqualTo("macave");
                    assertThat(surname.normalizedMainWord()).isEqualTo("mcav_");
                });

        name.setSurname("Sainte-Cluque");
        assertThat(PersonUtils.getNormalizedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("Sainte-Cluque");
                    assertThat(surname.simplifiedMainWord()).isEqualTo("saintecluque");
                    assertThat(surname.normalizedMainWord()).isEqualTo("saintecluq_");
                });

        name.setSurname("Bebedé");
        assertThat(PersonUtils.getNormalizedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("Bebedé");
                    assertThat(surname.simplifiedMainWord()).isEqualTo("vevede");
                    assertThat(surname.normalizedMainWord()).isEqualTo("vegveder");
                });

        name.setSurname("De Paula");
        assertThat(PersonUtils.getNormalizedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("De Paula");
                    assertThat(surname.simplifiedMainWord()).isEqualTo("depaula");
                    assertThat(surname.normalizedMainWord()).isEqualTo("depaol_");
                });

        name.setSurname("Ippolito");
        assertThat(PersonUtils.getNormalizedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("Ippolito");
                    assertThat(surname.simplifiedMainWord()).isEqualTo("ipolito");
                    assertThat(surname.normalizedMainWord()).isEqualTo("hipolit_");
                });

        name.setSurname("Viciconte");
        assertThat(PersonUtils.getNormalizedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("Viciconte");
                    assertThat(surname.simplifiedMainWord()).isEqualTo("viciconte");
                    assertThat(surname.normalizedMainWord()).isEqualTo("vicecont_");
                });
    }

}
