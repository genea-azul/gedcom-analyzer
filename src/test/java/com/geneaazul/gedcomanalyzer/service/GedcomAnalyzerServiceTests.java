package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.model.Surname;
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
import java.util.Objects;
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
                .forEach((person, pair) -> System.out.println(pair.getLeft() + " tree - (" + pair.getRight() + " persons) - Main person: " + person.getId() + " - " + person.getDisplayName()));

        System.out.println("\nfindPersonsByNameAndSpouseName:");
        searchService
                .findPersonsByNameAndSpouseName(null, "Diéguez", null, "Pérez", false, gedcom.getPeople())
                .forEach(System.out::println);

        System.out.println("\nfindSurnamesByPattern:");
        searchService
                .findSurnamesByPattern("^[dD]el ", gedcom.getPeople())
                .stream()
                .map(Surname::value)
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

        System.out.println("\nfindPersonsWithMisspellingByPlaceOfBirth:");
        searchService
                .findPersonsWithMisspellingByPlaceOfBirth("Italia", null, null, gedcom.getPeople())
                .forEach(System.out::println);

        System.out.println("\ngetPlacesOfBirthCardinality:");
        gedcomAnalyzerService
                .getPlacesOfBirthCardinality(gedcom.getPeople(), true)
                .stream()
                .filter(pair -> pair.getRight() > 10)
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
                .limit(150)
                .forEach(cardinality -> System.out.println(
                        cardinality.normalizedMainWord()
                                + " - " + cardinality.cardinality()
                                + " - " + cardinality.surnamesCardinality()
                                        .stream()
                                        .map(pair -> pair.getLeft() + " (" + pair.getRight() + ")")
                                        .collect(Collectors.joining(", "))
                                + " - Related: " + String.join(", ", cardinality.relatedNormalized())));

        System.out.println("\ngetAncestryCountriesCardinalityByPlaceOfBirth:");
        gedcomAnalyzerService
                .getAncestryCountriesCardinalityByPlaceOfBirth(gedcom.getPeople(), "Azul, Buenos Aires, Argentina")
                .stream()
                .limit(150)
                .forEach(cardinality -> System.out.println(
                        cardinality.country()
                                + " - " + cardinality.cardinality()
                                + " - " + cardinality.surnames()));

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
                    EnrichedPerson person = personResults.person();
                    System.out.println(">    " + person);
                    personResults
                            .results()
                            .forEach(comparisonResult -> {
                                Integer score = comparisonResult.getScore();
                                EnrichedPerson compare = comparisonResult.getCompare();
                                System.out.println(StringUtils.leftPad(String.valueOf(score), 2) + " - " + compare);
                            });
                });

        EnrichedPerson person = Objects.requireNonNull(gedcom.getPersonById("I4"));
        personService.setTransientProperties(person, true);

        System.out.println("\nsetTransientProperties (excludeRootPerson):");
        System.out.println("personsCountInTree: " + person.getPersonsCountInTree());
        System.out.println("surnamesCountInTree: " + person.getSurnamesCountInTree());
        System.out.println("ancestryCountries: " + person.getAncestryCountries());
        System.out.println("ancestryGenerations: " + person.getAncestryGenerations());
        System.out.println("maxDistantRelationship: " + person.getMaxDistantRelationship());

        System.out.println("\ngetPeopleInTree:");
        personService
                .getPeopleInTree(person, false, false)
                .stream()
                .sorted()
                .limit(50)
                .forEach(relationships -> System.out.println(
                        relationships
                                .getOrderedRelationships()
                                .stream()
                                .map(r -> relationshipMapper.toRelationshipDto(r, false))
                                .map(r -> relationshipMapper.formatInSpanish(r, 0, false))
                                .map(FormattedRelationship::toString)
                                .collect(Collectors.joining(", "))
                        + "  --  "
                        + relationships.findFirst().person()));
    }

}
