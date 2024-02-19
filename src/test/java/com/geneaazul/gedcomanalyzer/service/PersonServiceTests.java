package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.model.Relationships;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnableConfigurationProperties
@ActiveProfiles("test")
public class PersonServiceTests {

    @Autowired
    private PersonService personService;
    @Autowired
    private GedcomHolder gedcomHolder;
    @Autowired
    private RelationshipMapper relationshipMapper;

    @Test
    public void testGetPeopleInTreeWhenOneParentIsBiologicalAndAdoptive() {

        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson person = gedcom.getPersonById(9);
        List<Relationships> relationshipsList = personService.getPeopleInTree(person, false, false);

        List<FormattedRelationship> formatted = relationshipsList
                .stream()
                .map(Relationships::getOrderedRelationships)
                .map(TreeSet::first)
                .sorted()
                .map(relationship -> relationshipMapper.toRelationshipDto(relationship, false))
                .map(relationshipDto -> relationshipMapper.formatInSpanish(relationshipDto, false))
                .toList();

        formatted.forEach(System.out::println);

        assertThat(formatted.size()).isEqualTo(17);

        assertThat(formatted.stream().filter(f -> f.personName().equals("Son B&A")).findFirst())
                .hasValueSatisfying(f -> assertThat(f.relationshipDesc()).isEqualTo("persona principal"))
                .hasValueSatisfying(f -> assertThat(f.adoption()).isNull());

        assertThat(formatted.stream().filter(f -> f.personName().equals("Father B")).findFirst())
                .hasValueSatisfying(f -> assertThat(f.relationshipDesc()).isEqualTo("padre"))
                .hasValueSatisfying(f -> assertThat(f.adoption()).isNull());

        assertThat(formatted.stream().filter(f -> f.personName().equals("Mother B")).findFirst())
                .hasValueSatisfying(f -> assertThat(f.relationshipDesc()).isEqualTo("madre"))
                .hasValueSatisfying(f -> assertThat(f.adoption()).isNull());

        assertThat(formatted.stream().filter(f -> f.personName().equals("Father A")).findFirst())
                .hasValueSatisfying(f -> assertThat(f.relationshipDesc()).isEqualTo("padre adoptivo"))
                .hasValueSatisfying(f -> assertThat(f.adoption()).isEqualTo("ADOPTIVE"));

        assertThat(formatted.stream().filter(f -> f.personName().equals("Father's Couple B")).findFirst())
                .hasValueSatisfying(f -> assertThat(f.relationshipDesc()).isEqualTo("pareja de padre"))
                .hasValueSatisfying(f -> assertThat(f.adoption()).isNull());

        assertThat(formatted.stream().filter(f -> f.personName().equals("Mother's Couple B")).findFirst())
                .hasValueSatisfying(f -> assertThat(f.relationshipDesc()).isEqualTo("pareja de madre"))
                .hasValueSatisfying(f -> assertThat(f.adoption()).isNull());

        assertThat(formatted.stream().filter(f -> f.personName().equals("Father's Couple A")).findFirst())
                .hasValueSatisfying(f -> assertThat(f.relationshipDesc()).isEqualTo("pareja de padre adoptivo"))
                .hasValueSatisfying(f -> assertThat(f.adoption()).isEqualTo("ADOPTIVE"));

        // Siblings from both biological parents, one is adopted

        assertThat(formatted.stream().filter(f -> f.personName().equals("Son B1")).findFirst())
                .hasValueSatisfying(f -> assertThat(f.relationshipDesc()).isEqualTo("hermano"))
                .hasValueSatisfying(f -> assertThat(f.adoption()).isNull());

        assertThat(formatted.stream().filter(f -> f.personName().equals("Son A1")).findFirst())
                .hasValueSatisfying(f -> assertThat(f.relationshipDesc()).isEqualTo("hermano"))
                .hasValueSatisfying(f -> assertThat(f.adoption()).isEqualTo("ADOPTIVE"));

        // Siblings from biological parent and adoptive parent, one is adopted

        assertThat(formatted.stream().filter(f -> f.personName().equals("Son B2")).findFirst())
                .hasValueSatisfying(f -> assertThat(f.relationshipDesc()).isEqualTo("medio-hermano"))
                .hasValueSatisfying(f -> assertThat(f.adoption()).isNull());

        assertThat(formatted.stream().filter(f -> f.personName().equals("Son A2")).findFirst())
                .hasValueSatisfying(f -> assertThat(f.relationshipDesc()).isEqualTo("hermano"))
                .hasValueSatisfying(f -> assertThat(f.adoption()).isEqualTo("ADOPTIVE"));

        // Siblings from biological mother and new couple, one is adopted

        assertThat(formatted.stream().filter(f -> f.personName().equals("Son B3 Mother's Couple B")).findFirst())
                .hasValueSatisfying(f -> assertThat(f.relationshipDesc()).isEqualTo("medio-hermano"))
                .hasValueSatisfying(f -> assertThat(f.adoption()).isNull());

        assertThat(formatted.stream().filter(f -> f.personName().equals("Son A3 Mother's Couple B")).findFirst())
                .hasValueSatisfying(f -> assertThat(f.relationshipDesc()).isEqualTo("medio-hermano"))
                .hasValueSatisfying(f -> assertThat(f.adoption()).isEqualTo("ADOPTIVE"));

        // Siblings from biological father and new couple, one is adopted

        assertThat(formatted.stream().filter(f -> f.personName().equals("Son B4 Father's Couple B")).findFirst())
                .hasValueSatisfying(f -> assertThat(f.relationshipDesc()).isEqualTo("medio-hermano"))
                .hasValueSatisfying(f -> assertThat(f.adoption()).isNull());

        assertThat(formatted.stream().filter(f -> f.personName().equals("Son A4 Father's Couple B")).findFirst())
                .hasValueSatisfying(f -> assertThat(f.relationshipDesc()).isEqualTo("medio-hermano"))
                .hasValueSatisfying(f -> assertThat(f.adoption()).isEqualTo("ADOPTIVE"));

        // Siblings from adoptive father and new couple, one is adopted

        assertThat(formatted.stream().filter(f -> f.personName().equals("Son B5 Father's Couple A")).findFirst())
                .hasValueSatisfying(f -> assertThat(f.relationshipDesc()).isEqualTo("medio-hermano"))
                .hasValueSatisfying(f -> assertThat(f.adoption()).isEqualTo("ADOPTIVE"));

        assertThat(formatted.stream().filter(f -> f.personName().equals("Son A5 Father's Couple A")).findFirst())
                .hasValueSatisfying(f -> assertThat(f.relationshipDesc()).isEqualTo("medio-hermano"))
                .hasValueSatisfying(f -> assertThat(f.adoption()).isEqualTo("ADOPTIVE"));
    }

}
