package com.geneaazul.gedcomanalyzer.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.Surname;
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
    public void testGetShortenedSurnameMainWord() {
        Name name = new Name();
        name.setGiven("?");
        Person person = new Person();
        person.addName(name);

        name.setSurname("Dí YannijébeZçlli Rago");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("Dí YannijébeZçlli Rago");
                    assertThat(surname.normalizedMainWord()).isEqualTo("dianigevescli");
                    assertThat(surname.shortenedMainWord()).isEqualTo("dianigevescl_");
                });

        name.setSurname("La Camera");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("La Camera");
                    assertThat(surname.normalizedMainWord()).isEqualTo("lacamara");
                    assertThat(surname.shortenedMainWord()).isEqualTo("lacamar_");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("Lacámara", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(matching)).isTrue();
                });

        name.setSurname("Mac Cabe");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("Mac Cabe");
                    assertThat(surname.normalizedMainWord()).isEqualTo("mcave");
                    assertThat(surname.shortenedMainWord()).isEqualTo("mcav_");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("McCabe", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(matching)).isTrue();
                });

        name.setSurname("Sainte-Cluque");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("Sainte-Cluque");
                    assertThat(surname.normalizedMainWord()).isEqualTo("saintecluque");
                    assertThat(surname.shortenedMainWord()).isEqualTo("saintecluq_");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("Saintecluque", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(matching)).isTrue();
                });

        name.setSurname("Bebedé");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("Bebedé");
                    assertThat(surname.normalizedMainWord()).isEqualTo("vegveder");
                    assertThat(surname.shortenedMainWord()).isEqualTo("vegveder");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("Betbeder", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(matching)).isTrue();
                });

        name.setSurname("De Paula");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("De Paula");
                    assertThat(surname.normalizedMainWord()).isEqualTo("depaola");
                    assertThat(surname.shortenedMainWord()).isEqualTo("depaol_");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("Di Paola", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(matching)).isTrue();
                });

        name.setSurname("Ippolito");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("Ippolito");
                    assertThat(surname.normalizedMainWord()).isEqualTo("hipolito");
                    assertThat(surname.shortenedMainWord()).isEqualTo("hipolit_");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("Hippólito", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(matching)).isTrue();
                });

        name.setSurname("Viciconte");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("Viciconte");
                    assertThat(surname.normalizedMainWord()).isEqualTo("viceconte");
                    assertThat(surname.shortenedMainWord()).isEqualTo("vicecont_");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("Viceconte", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(matching)).isTrue();
                });

        name.setSurname("Gioja");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("Gioja");
                    assertThat(surname.normalizedMainWord()).isEqualTo("gioia");
                    assertThat(surname.shortenedMainWord()).isEqualTo("g_");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("Gioia", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(matching)).isTrue();

                    Surname notMatching = PersonUtils.getShortenedSurnameMainWord("Gau", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(notMatching)).isFalse();
                });

        name.setSurname("Montagna");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("Montagna");
                    assertThat(surname.normalizedMainWord()).isEqualTo("montagna");
                    assertThat(surname.shortenedMainWord()).isEqualTo("montagn_");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("Montaño", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(matching)).isTrue();
                });

        name.setSurname("Castaño");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("Castaño");
                    assertThat(surname.normalizedMainWord()).isEqualTo("castagno");
                    assertThat(surname.shortenedMainWord()).isEqualTo("castagn_");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("Castagna", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(matching)).isTrue();
                });
    }

}
