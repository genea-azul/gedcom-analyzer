package com.geneaazul.gedcomanalyzer.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;

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
public class SearchUtilsTests {

    @Autowired
    private GedcomAnalyzerProperties properties;

    @Test
    public void testGetNormalizedGivenNameForSearch() {
        Name name = new Name();
        name.setGiven(" ? d'  (YíçbjéjizAEU) - Domenico ");
        name.setSurname("Smith");

        EventFact sexEventFact = new EventFact();
        sexEventFact.setTag("SEX");
        sexEventFact.setValue("M");

        Person person = new Person();
        person.addName(name);
        person.addEventFact(sexEventFact);

        assertThat(PersonUtils.getNormalizedGivenNameForSearch(person, properties.getNormalizedNamesMap()))
                .get()
                .satisfies(givenName -> {
                    assertThat(givenName.getName()).isEqualTo("d yicbjejizaeu domingo");
                    assertThat(givenName.getWordsCount()).isEqualTo(3);
                    assertThat(givenName.getSearchPattern().toString()).isEqualTo("^(?=.*\\bd\\b)(?=.*\\byicbjejizaeu\\b)(?=.*\\bdomingo\\b).*$");
                });
    }

    @Test
    public void testGetSurnameForSearch() {
        Name name = new Name();
        name.setGiven("Domenico");
        name.setSurname(" ? d'  (YíçbjéjizAEU) - Smith ");

        Person person = new Person();
        person.addName(name);

        assertThat(PersonUtils.getSurnameForSearch(person))
                .contains("d yicbjejizaeu smith");
    }

    @Test
    public void testGetSurnameMainWordForSearch() {
        Name name = new Name();
        name.setGiven("Domenico");
        name.setSurname("Dí YannijébeZçlli Rago");

        Person person = new Person();
        person.addName(name);

        assertThat(PersonUtils.getSurnameMainWordForSearch(person, properties.getNormalizedSurnamesMap()))
                .contains("dianigevescl_");
    }

}
