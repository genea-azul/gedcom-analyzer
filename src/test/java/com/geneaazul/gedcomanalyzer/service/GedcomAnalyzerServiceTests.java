package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.Surname;
import com.geneaazul.gedcomanalyzer.model.dto.RelationshipDto;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.DateUtils.AstrologicalSign;
import com.geneaazul.gedcomanalyzer.utils.PathUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folg.gedcom.model.EventFact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Month;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private EnrichedGedcom gedcom;

    @BeforeEach
    public void setUp() {
        gedcom = gedcomHolder.getGedcom();
    }

    @Test
    public void getMissingReferences() {
        System.out.println("\ngetMissingReferences:");
        gedcomAnalyzerService
                .getMissingReferences(gedcom.getLegacyGedcom().orElseThrow())
                .forEach(System.out::println);
    }

    @Test
    public void findPersonsWithTagExtensions() {
        System.out.println("\nfindPersonsWithTagExtensions:");
        gedcom.analyzeCustomEventFactsAndTagExtensions();

        searchService
                .findPersonsWithTagExtensions(gedcom.getPeople())
                .forEach(person -> System.out.println(person.getDisplayName() + ": " + person.getTagExtensions()));
    }

    @Test
    public void findPersonsWithNoCountryButParentsWithCountry() {
        System.out.println("\nfindPersonsWithNoCountryButParentsWithCountry:");
        searchService
                .findPersonsWithNoCountryButParentsWithCountry(gedcom.getPeople())
                .forEach(System.out::println);
    }

    @Test
    public void getMostFrequentSurnamesByOrphanTree() {
        System.out.println("\ngetMostFrequentSurnamesByOrphanTree:");
        List<EnrichedPerson> persons1 = gedcomAnalyzerService
                .getInitialPersonOfOrphanTrees(gedcom);
        gedcomAnalyzerService
                .getMostFrequentSurnamesByPersonSubTree(persons1)
                .forEach((person, pair) -> System.out.println(pair.getLeft() + " tree - (" + pair.getRight() + " persons) - Main person: " + person.getId() + " - " + person.getDisplayName()));
    }

    @Test
    public void findPersonsByNameAndSpouseName() {
        System.out.println("\nfindPersonsByNameAndSpouseName:");
        searchService
                .findPersonsByNameAndSpouseName(null, "Vazzano", null, "Bazzano", false, gedcom.getPeople())
                .forEach(System.out::println);
    }

    @Test
    public void findSurnamesByPattern() {
        System.out.println("\nfindSurnamesByPattern:");
        searchService
                .findSurnamesByPattern("^[dD]el ", gedcom.getPeople())
                .stream()
                .map(Surname::value)
                .forEach(System.out::println);
    }

    @Test
    public void findAlivePersonsTooOldOrWithFamilyMembersTooOld() {
        System.out.println("\nfindAlivePersonsTooOldOrWithFamilyMembersTooOld:");
        searchService
                .findAlivePersonsTooOldOrWithFamilyMembersTooOld(gedcom.getPeople())
                .forEach(System.out::println);
    }

    @Test
    public void findPersonsWithCustomEventFacts() {
        System.out.println("\nfindPersonsWithCustomEventFacts:");
        gedcom.analyzeCustomEventFactsAndTagExtensions();

        searchService
                .findPersonsWithCustomEventFacts(gedcom.getPeople())
                .stream()
                .filter(person -> !person.getCustomEventFacts().isEmpty()) // only display the ones with person custom events
                .forEach(person -> System.out.println(person.getDisplayName() + ": " + person.getCustomEventFacts()
                        .stream()
                        .map(EventFact::getType)
                        .toList()));
    }

    @Test
    public void findPersonsWithMisspellingByPlaceOfBirth() {
        System.out.println("\nfindPersonsWithMisspellingByPlaceOfBirth:");
        searchService
                .findPersonsWithMisspellingByPlaceOfBirth("Italia", null, null, gedcom.getPeople())
                .forEach(System.out::println);
    }

    @Test
    public void getPlacesOfBirthCardinality() {
        System.out.println("\ngetPlacesOfBirthCardinality:");
        gedcomAnalyzerService
                .getPlacesOfBirthCardinality(gedcom.getPeople(), true)
                .stream()
                .filter(pair -> pair.getRight() > 10)
                .forEach(pair -> System.out.println(pair.getLeft() + " (" + pair.getRight() + ")"));
    }

    @Test
    public void getCountriesOfBirthCardinality() {
        System.out.println("\ngetCountriesOfBirthCardinality:");
        gedcomAnalyzerService
                .getCountriesOfBirthCardinality(gedcom.getPeople(), true)
                .forEach(pair -> System.out.println(pair.getLeft() + " (" + pair.getRight() + ")"));
    }

    @Test
    public void getAllPlaces() {
        System.out.println("\ngetAllPlaces:");
        gedcomAnalyzerService
                .getAllPlaces(gedcom.getLegacyGedcom().orElseThrow(), true)
                .forEach(System.out::println);
    }

    @Test
    public void getSurnamesCardinalityByPlaceOfBirth() {
        System.out.println("\ngetSurnamesCardinalityByPlaceOfBirth:");
        gedcomAnalyzerService
                .getSurnamesCardinalityByPlaceOfBirth(gedcom.getPeople(), "Azul, Buenos Aires, Argentina", true)
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
    }

    @Test
    @Disabled
    public void getAncestryCountriesCardinalityByPlaceOfBirth() {
        System.out.println("\ngetAncestryCountriesCardinalityByPlaceOfBirth:");
        gedcomAnalyzerService
                .getAncestryCountriesCardinalityByPlaceOfBirth(gedcom.getPeople(), "Azul, Buenos Aires, Argentina")
                .stream()
                .limit(150)
                .forEach(cardinality -> System.out.println(
                        cardinality.country()
                                + " - " + cardinality.cardinality()
                                + " - " + cardinality.surnames()));
    }

    @Test
    public void findPersonsByPlaceOfBirth() {
        System.out.println("\nfindPersonsByPlaceOfBirth:");
        searchService
                .findPersonsByPlaceOfBirth("Latina, Lazio, Italia", null, null, gedcom.getPeople())
                .forEach(System.out::println);
    }

    @Test
    public void findPersonsByMonthAndDayOfDeath() {
        System.out.println("\nfindPersonsByMonthAndDayOfDeath:");
        searchService
                .findPersonsByMonthAndDayOfDeath(Month.APRIL, 2, null, gedcom.getPeople())
                .forEach(System.out::println);
    }

    @Test
    public void getAstrologicalSignsCardinalityByPlaceOfBirth() {
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
    }

    @Test
    public void getMonthOfDeathCardinalityByPlaceOfDeath() {
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
    }

    @Test
    public void findPersonsWithManyChildrenByPlaceOfBirth() {
        System.out.println("\nfindPersonsWithManyChildrenByPlaceOfBirth:");
        Stream.concat(
                searchService
                        .findPersonsByPlaceOfBirth("Azul, Buenos Aires, Argentina", true, null, gedcom.getPeople())
                        .stream(),
                searchService
                        .findPersonsByPlaceOfBirth("Azul, Buenos Aires, Argentina", true, null, gedcom.getPeople())
                        .stream()
                        .map(EnrichedPerson::getParents)
                        .flatMap(List::stream))
                .distinct()
                .filter(person -> person.getChildren().size() >= 9)
                .sorted(Comparator
                        .<EnrichedPerson, Integer>comparing(person -> person.getChildren().size())
                        .reversed())
                .forEach(System.out::println);
    }

    @Test
    public void findDuplicatedPersons() {
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
    }

    @Test
    public void getPeopleInTree() {
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

    @Disabled
    @Test
    @SuppressWarnings("DataFlowIssue")
    public void getShortestPathsToPersons() {
        EnrichedPerson person = gedcom.getPersonById("I4");
        Pair<Map<String, Integer>, Map<String, List<String>>> distancesAndPaths = PathUtils.calculateShortestPathFromSource(gedcom, person);

        System.out.println("\ngetShortestPathsToPersons:");
        System.out.println("distance from I4 to I1 (" + gedcom.getPersonById("I1").getDisplayName() + "): " + distancesAndPaths.getLeft().get("I1"));
        System.out.println("distance from I4 to I2 (" + gedcom.getPersonById("I2").getDisplayName() + "): " + distancesAndPaths.getLeft().get("I2"));
        System.out.println("distance from I4 to I3 (" + gedcom.getPersonById("I3").getDisplayName() + "): " + distancesAndPaths.getLeft().get("I3"));
        System.out.println("distance from I4 to I4 (" + gedcom.getPersonById("I4").getDisplayName() + "): " + distancesAndPaths.getLeft().get("I4"));
        System.out.println("distance from I4 to I5 (" + gedcom.getPersonById("I5").getDisplayName() + "): " + distancesAndPaths.getLeft().get("I5"));
        System.out.println("distance from I4 to I6 (" + gedcom.getPersonById("I6").getDisplayName() + "): " + distancesAndPaths.getLeft().get("I6"));
        System.out.println("distance from I4 to Papa Franc.: " + distancesAndPaths.getLeft().get("I525113"));
        System.out.println("distance from I4 to JM de Rosas: " + distancesAndPaths.getLeft().get("I542961"));

        List<String> shortestPath = distancesAndPaths.getRight().getOrDefault("I525113", List.of());
        for (int i = 0; i < shortestPath.size() - 1; i++) {
            EnrichedPerson personA = gedcom.getPersonById(shortestPath.get(i));
            EnrichedPerson personB = gedcom.getPersonById(shortestPath.get(i + 1));
            Relationship relationship = personService.getRelationshipBetween(personB, personA);
            RelationshipDto relationshipDto = relationshipMapper.toRelationshipDto(relationship, false);
            FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, 0, false);
            System.out.println(personA.getDisplayName() + " " + formattedRelationship.relationshipDesc() + " de " + personB.getDisplayName());
        }
    }

}
