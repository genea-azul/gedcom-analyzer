package com.geneaazul.gedcomanalyzer.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.Surname;

import org.folg.gedcom.model.EventFact;
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
public class NameUtilsTests {

    @Autowired
    private GedcomAnalyzerProperties properties;

    @Test
    public void testGetNormalizedGivenName() {
        Name name = new Name();
        name.setSurname("Smith");
        EventFact sex = new EventFact();
        sex.setTag("SEX");
        Person person = new Person();
        person.addName(name);
        person.addEventFact(sex);

        name.setGiven(" ? d'  (YíçbjéjizAEU) - Domenico ");
        sex.setValue("M");
        assertThat(PersonUtils.getNormalizedGivenName(person, properties.getNormalizedGivenNamesMap()))
                .get()
                .satisfies(givenName -> {
                    assertThat(givenName.value()).isEqualTo("? d'  (YíçbjéjizAEU) - Domenico");
                    assertThat(givenName.normalized()).isEqualTo("d yicbjejizaeu domingo");
                    assertThat(givenName.wordsCount()).isEqualTo(3);
                    assertThat(givenName.searchPattern().toString()).isEqualTo("^(?=.*\\bd\\b)(?=.*\\byicbjejizaeu\\b)(?=.*\\bdomingo\\b).*$");
                });

        name.setGiven("Francescantonio");
        sex.setValue("M");
        assertThat(PersonUtils.getNormalizedGivenName(person, properties.getNormalizedGivenNamesMap()))
                .get()
                .satisfies(givenName -> {
                    assertThat(givenName.value()).isEqualTo("Francescantonio");
                    assertThat(givenName.normalized()).isEqualTo("francisco antonio");
                    assertThat(givenName.wordsCount()).isEqualTo(2);
                    assertThat(givenName.searchPattern().toString()).isEqualTo("^(?=.*\\bfrancisco\\b)(?=.*\\bantonio\\b).*$");
                });

        name.setGiven("Elizabeth dite Marie");
        sex.setValue("F");
        assertThat(PersonUtils.getNormalizedGivenName(person, properties.getNormalizedGivenNamesMap()))
                .get()
                .satisfies(givenName -> {
                    assertThat(givenName.value()).isEqualTo("Elizabeth dite Marie");
                    assertThat(givenName.normalized()).isEqualTo("elisa isabel maria");
                    assertThat(givenName.wordsCount()).isEqualTo(3);
                    assertThat(givenName.searchPattern().toString()).isEqualTo("^(?=.*\\belisa\\b)(?=.*\\bisabel\\b)(?=.*\\bmaria\\b).*$");
                });

        name.setGiven("Валянціна");
        sex.setValue("F");
        assertThat(PersonUtils.getNormalizedGivenName(person, properties.getNormalizedGivenNamesMap()))
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
                    assertThat(surname.normalizedMainWord()).isEqualTo("sainteclucue");
                    assertThat(surname.shortenedMainWord()).isEqualTo("saintecluc_");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("Saint Cluque", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(matching)).isTrue();
                });

        name.setSurname("Ahetxetcheberry");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("Ahetxetcheberry");
                    assertThat(surname.normalizedMainWord()).isEqualTo("ahetsetcheveri");
                    assertThat(surname.shortenedMainWord()).isEqualTo("ahetsetchever_");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("Ahets Etcheberry", properties.getNormalizedSurnamesMap()).orElse(null);
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

                    Surname notMatching = PersonUtils.getShortenedSurnameMainWord("Gau Rodríguez", properties.getNormalizedSurnamesMap()).orElse(null);
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

        name.setSurname("Pérez de la Cruz Molina");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("Pérez de la Cruz Molina");
                    assertThat(surname.normalizedMainWord()).isEqualTo("pereslacrus");
                    assertThat(surname.shortenedMainWord()).isEqualTo("pereslacrus");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("Pérez", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(matching)).isFalse();
                });

        name.setSurname("Pérez de Villarreal García");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("Pérez de Villarreal García");
                    assertThat(surname.normalizedMainWord()).isEqualTo("peresdevilareal");
                    assertThat(surname.shortenedMainWord()).isEqualTo("peresdevilareal");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("Pérez", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(matching)).isFalse();
                });

        name.setSurname("De la Rosa Fernández");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("De la Rosa Fernández");
                    assertThat(surname.normalizedMainWord()).isEqualTo("delarosa");
                    assertThat(surname.shortenedMainWord()).isEqualTo("delaros_");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("Rosa", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(matching)).isFalse();
                });

        name.setSurname("B. y Miñana González");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("B. y Miñana González");
                    assertThat(surname.normalizedMainWord()).isEqualTo("vi");
                    assertThat(surname.shortenedMainWord()).isEqualTo("v_");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("B", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(matching)).isFalse();
                });

        name.setSurname("De los Heros");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("De los Heros");
                    assertThat(surname.normalizedMainWord()).isEqualTo("delosheros");
                    assertThat(surname.shortenedMainWord()).isEqualTo("delosheros");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("Heros", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(matching)).isFalse();
                });

        name.setSurname("De San Martín");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("De San Martín");
                    assertThat(surname.normalizedMainWord()).isEqualTo("sanmartin");
                    assertThat(surname.shortenedMainWord()).isEqualTo("sanmartin");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("San Martín", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(matching)).isTrue();
                });

        name.setSurname("de Etxegaray");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("de Etxegaray");
                    assertThat(surname.normalizedMainWord()).isEqualTo("etchegar_");
                    assertThat(surname.shortenedMainWord()).isEqualTo("etchegar_");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("Etchegaray", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(matching)).isTrue();
                });

        name.setSurname("Peuvrié de Urreta");
        assertThat(PersonUtils.getShortenedSurnameMainWord(person, properties.getNormalizedSurnamesMap()))
                .get()
                .satisfies(surname -> {
                    assertThat(surname.value()).isEqualTo("Peuvrié de Urreta");
                    assertThat(surname.normalizedMainWord()).isEqualTo("peuvrie");
                    assertThat(surname.shortenedMainWord()).isEqualTo("peuvr_");

                    Surname matching = PersonUtils.getShortenedSurnameMainWord("Peuvrié", properties.getNormalizedSurnamesMap()).orElse(null);
                    assertThat(surname.matches(matching)).isTrue();
                });
    }

}
