package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.DateUtils.AstrologicalSign;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folg.gedcom.model.EventFact;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@SpringBootTest
@EnableConfigurationProperties
@ActiveProfiles("test")
public class GedcomAnalyzerServiceTests {

    @Autowired
    private GedcomHolder gedcomHolder;
    @Autowired
    private GedcomAnalyzerService gedcomAnalyzerService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private PersonService personService;
    @Autowired
    private RelationshipMapper relationshipMapper;

    @Test
    public void smokeTest() {

        EnrichedGedcom gedcom = gedcomHolder.getGedcom();

        gedcom.analyzeCustomEventFactsAndTagExtensions();

        System.out.println("\ngetMissingReferences:");
        gedcomAnalyzerService
                .getMissingReferences(gedcom.getGedcom())
                .forEach(System.out::println);

        System.out.println("\nfindPersonsWithTagExtensions:");
        searchService
                .findPersonsWithTagExtensions(gedcom.getPeople())
                .forEach(person -> System.out.println(person.getDisplayName() + ": " + person.getTagExtensions()));

        System.out.println("\nfindPersonsWithNoCountryButParentsWithCountry:");
        searchService
                .findPersonsWithNoCountryButParentsWithCountry(gedcom.getPeople())
                .forEach(System.out::println);

        System.out.println("\ngetMostFrequentSurnamesByOrphanTree:");
        List<EnrichedPerson> persons1 = gedcomAnalyzerService
                .getInitialPersonOfOrphanTrees(gedcom);
        gedcomAnalyzerService
                .getMostFrequentSurnamesByPersonSubTree(persons1)
                .forEach((person, surname) -> System.out.println(surname + " tree - Main person: " + person.getId() + " - " + person.getDisplayName()));

        System.out.println("\nfindPersonsBySurnameAndSpouseSurname:");
        searchService
                .findPersonsBySurnameAndSpouseSurname("Diéguez", "Pérez", false, gedcom.getPeople())
                .forEach(System.out::println);

        System.out.println("\nfindPersonsBySurnameAndSpouseGivenName:");
        searchService
                .findPersonsBySurnameAndSpouseGivenName("Zaffora", "Maria", false, gedcom.getPeople())
                .forEach(System.out::println);

        System.out.println("\nfindAlivePersonsTooOldOrWithFamilyMembersTooOld:");
        searchService
                .findAlivePersonsTooOldOrWithFamilyMembersTooOld(gedcom.getPeople())
                .forEach(System.out::println);

        System.out.println("\nfindPersonsWithCustomEventFacts:");
        searchService
                .findPersonsWithCustomEventFacts(gedcom.getPeople())
                .stream()
                .filter(person -> !person.getCustomEventFacts().isEmpty()) // only display the ones with person custom events
                .forEach(person -> System.out.println(person.getDisplayName() + ": " + person.getCustomEventFacts()
                        .stream()
                        .map(EventFact::getType)
                        .toList()));

        System.out.println("\ngetPlacesOfBirthCardinality:");
        gedcomAnalyzerService
                .getPlacesOfBirthCardinality(gedcom.getPeople(), true)
                .stream()
                .filter(pair -> pair.getRight() > 1)
                .forEach(pair -> System.out.println(pair.getLeft() + " (" + pair.getRight() + ")"));

        System.out.println("\ngetCountriesOfBirthCardinality:");
        gedcomAnalyzerService
                .getCountriesOfBirthCardinality(gedcom.getPeople(), true)
                .forEach(pair -> System.out.println(pair.getLeft() + " (" + pair.getRight() + ")"));

        System.out.println("\ngetAllPlaces:");
        gedcomAnalyzerService
                .getAllPlaces(gedcom.getGedcom(), true)
                .forEach(System.out::println);

        System.out.println("\ngetSurnamesCardinalityByPlaceOfBirth:");
        gedcomAnalyzerService
                .getSurnamesCardinalityByPlaceOfBirth(gedcom.getPeople(), "Azul, Buenos Aires, Argentina")
                .stream()
                .limit(500)
                .forEach(cardinality -> System.out.println(
                        cardinality.normalizedMainWord()
                                + " - " + cardinality.cardinality()
                                + " - " + cardinality.surnamesCardinality()
                                        .stream()
                                        .map(pair -> pair.getLeft() + " (" + pair.getRight() + ")")
                                        .collect(Collectors.joining(", "))
                                + " - Related: " + String.join(", ", cardinality.relatedNormalized())));

        System.out.println("\nfindPersonsByPlaceOfBirth:");
        searchService
                .findPersonsByPlaceOfBirth("Latina, Lazio, Italia", null, null, gedcom.getPeople())
                .forEach(System.out::println);

        System.out.println("\nfindPersonsByMonthAndDayOfDeath:");
        searchService
                .findPersonsByMonthAndDayOfDeath(Month.APRIL, 2, null, gedcom.getPeople())
                .forEach(System.out::println);

        System.out.println("\ngetAstrologicalSignsCardinalityByPlaceOfBirth:");
        List<EnrichedPerson> personsByPlaceOfBirth = searchService
                .findPersonsByPlaceOfBirth("Azul, Buenos Aires, Argentina", null, null, gedcom.getPeople());
        Pair<Optional<EnrichedPerson>, Optional<EnrichedPerson>> minMaxDateOfBirth = gedcomAnalyzerService.getMinMaxFullDateOfBirth(personsByPlaceOfBirth);
        Map<AstrologicalSign, Integer> astrologicalSignsCardinality = gedcomAnalyzerService.getAstrologicalSignsCardinality(personsByPlaceOfBirth, true);
        int astrologicalSignsCount = astrologicalSignsCardinality.values().stream().reduce(0, Integer::sum);
        System.out.println(
                minMaxDateOfBirth
                        .getLeft()
                        .map(person -> person.getDateOfBirth().map(Date::format).orElse("") + " (" + person.getDisplayName() + ")")
                        .orElse("")
                + " to "
                + minMaxDateOfBirth
                        .getRight()
                        .map(person -> person.getDateOfBirth().map(Date::format).orElse("") + " (" + person.getDisplayName() + ")")
                        .orElse("")
                + ": " + astrologicalSignsCount + " births");
        astrologicalSignsCardinality
                .forEach(((astrologicalSign, count) -> System.out.printf("%s\t%.2f%%\t%d%n", astrologicalSign, (double) count / astrologicalSignsCount * 100, count)));

        System.out.println("\ngetMonthOfDeathCardinalityByPlaceOfDeath:");
        List<EnrichedPerson> personsByPlaceOfDeath = searchService
                .findPersonsByPlaceOfDeath("Azul, Buenos Aires, Argentina", null, gedcom.getPeople());
        Pair<Optional<EnrichedPerson>, Optional<EnrichedPerson>> minMaxDateOfDeath = gedcomAnalyzerService.getMinMaxFullDateOfDeath(personsByPlaceOfDeath);
        Map<Month, Integer> monthsOfDeathCardinality = gedcomAnalyzerService.getMonthsOfDeathCardinality(personsByPlaceOfDeath);
        int monthsCount = monthsOfDeathCardinality.values().stream().reduce(0, Integer::sum);
        System.out.println(
                minMaxDateOfDeath
                        .getLeft()
                        .map(person -> person.getDateOfDeath().map(Date::format).orElse("") + " (" + person.getDisplayName() + ")")
                        .orElse("")
                + " to "
                + minMaxDateOfDeath
                        .getRight()
                        .map(person -> person.getDateOfDeath().map(Date::format).orElse("") + " (" + person.getDisplayName() + ")")
                        .orElse("")
                + ": " + monthsCount + " deaths");
        monthsOfDeathCardinality
                .forEach(((month, count) -> System.out.printf("%s\t%.2f%%\t%d%n", month, (double) count / monthsCount * 100, count)));

        System.out.println("\nfindDuplicatedPersons:");
        searchService
                .findDuplicatedPersons(gedcom)
                .forEach(personResults -> {
                    EnrichedPerson person = personResults.getPerson();
                    System.out.println(">    " + person);
                    personResults
                            .getResults()
                            .forEach(comparisonResult -> {
                                Integer score = comparisonResult.getScore();
                                EnrichedPerson compare = comparisonResult.getCompare();
                                System.out.println(StringUtils.leftPad(String.valueOf(score), 2) + " - " + compare);
                            });
                });

        System.out.println("\ngetNumberOfPeopleInTree and getMaxDistantRelationship:");
        System.out.println(personService.getNumberOfPeopleInTree(gedcom.getPersonById("I4")));
        System.out.println(personService.getAncestryCountries(gedcom.getPersonById("I4")));
        System.out.println(personService.getAncestryGenerations(gedcom.getPersonById("I4")));
        System.out.println(personService.getMaxDistantRelationship(gedcom.getPersonById("I4")));

        System.out.println("\ngetPeopleInTree:");
        personService
                .getPeopleInTree(gedcom.getPersonById("I4"), false)
                .stream()
                .sorted()
                .limit(500)
                .forEach(relationship -> System.out.println(
                        relationshipMapper
                                .toRelationshipDto(relationship, false)
                                .toString()
                        + "  --  "
                        + relationship.person()));
    }

}
