package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.Place;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.Surname;
import com.geneaazul.gedcomanalyzer.model.dto.RelationshipDto;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.DateUtils.AstrologicalSign;
import com.geneaazul.gedcomanalyzer.utils.PathUtils;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Month;
import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
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
    private SurnameService surnameService;
    @Autowired
    private RelationshipMapper relationshipMapper;

    private EnrichedGedcom gedcom;

    @BeforeEach
    public void setUp() {
        gedcom = gedcomHolder.getGedcom();
    }

    @Test
    public void getMissingReferences() {
        System.out.println("getMissingReferences:");
        gedcomAnalyzerService
                .getMissingReferences(gedcom.getLegacyGedcom().orElseThrow())
                .forEach(System.out::println);
    }

    @Test
    public void findPersonsWithTagExtensions() {
        System.out.println("findPersonsWithTagExtensions:");
        gedcom.analyzeCustomEventFactsAndTagExtensions();

        searchService
                .findPersonsWithTagExtensions(gedcom.getPeople())
                .forEach(person -> System.out.println(person.getDisplayName() + ": " + person.getTagExtensions()));
    }

    @Test
    public void findPersonsWithNoCountryButParentsWithCountry() {
        System.out.println("findPersonsWithNoCountryButParentsWithCountry:");
        searchService
                .findPersonsWithNoCountryButParentsWithCountry(gedcom.getPeople())
                .forEach(System.out::println);
    }

    @Test
    public void getMostFrequentSurnamesByOrphanTree() {
        System.out.println("getMostFrequentSurnamesByOrphanTree:");
        List<EnrichedPerson> persons = gedcomAnalyzerService
                .getInitialPersonOfOrphanTrees(gedcom);
        Map<EnrichedPerson, Pair<String, Integer>> subTrees = gedcomAnalyzerService
                .getMostFrequentSurnamesByPersonSubTree(persons);
        subTrees
                .forEach((person, pair) -> System.out.println(pair.getLeft() + " tree - (" + pair.getRight() + " persons) - Main person: " + person.getId() + " - " + person.getDisplayName()));
        int personsCount = subTrees
                .values()
                .stream()
                .mapToInt(Pair::getRight)
                .sum();
        System.out.println("Total persons: " + personsCount);
    }

    @Test
    public void findPersonsByNameAndSpouseName() {
        System.out.println("findPersonsByNameAndSpouseName:");
        searchService
                .findPersonsByNameAndSpouseName(null, "Vazzano", null, "Bazzano", false, gedcom.getPeople())
                .forEach(System.out::println);
    }

    @Test
    public void findSurnamesByPattern() {
        System.out.println("findSurnamesByPattern:");
        searchService
                .findSurnamesByPattern(" [dD]el ", gedcom.getPeople())
                .stream()
                .map(Surname::value)
                .forEach(System.out::println);
    }

    @Test
    public void findAlivePersonsTooOldOrWithFamilyMembersTooOld() {
        System.out.println("findAlivePersonsTooOldOrWithFamilyMembersTooOld:");
        searchService
                .findAlivePersonsTooOldOrWithFamilyMembersTooOld(gedcom.getPeople())
                .forEach(System.out::println);
    }

    @Test
    public void findPersonsWithCustomEventFacts() {
        System.out.println("findPersonsWithCustomEventFacts:");
        gedcom.analyzeCustomEventFactsAndTagExtensions();

        searchService
                .findPersonsWithCustomEventFacts(gedcom.getPeople())
                .stream()
                .filter(person -> !person.getCustomEventFacts().isEmpty()) // only display the ones with person custom events
                .forEach(person -> System.out.println(person.getDisplayName() + ": " + person.getCustomEventFacts()
                        .stream()
                        .map(event -> event.getType() + " - " + event.getValue())
                        .toList()));
    }

    @Test
    public void findPersonsWithMisspellingByPlaceOfBirth() {
        System.out.println("findPersonsWithMisspellingByPlaceOfBirth:");
        searchService
                .findPersonsWithMisspellingByPlaceOfBirth("Italia", null, null, gedcom.getPeople())
                .forEach(System.out::println);
    }

    @Test
    public void getPlacesOfBirthCardinality() {
        System.out.println("getPlacesOfBirthCardinality:");
        gedcomAnalyzerService
                .getPlacesOfBirthCardinality(gedcom.getPeople(), true)
                .stream()
                .filter(pair -> pair.getRight() > 10)
                .forEach(pair -> System.out.println(pair.getLeft() + " (" + pair.getRight() + ")"));
    }

    @Test
    public void getCountriesOfBirthCardinality() {
        System.out.println("getCountriesOfBirthCardinality:");
        gedcomAnalyzerService
                .getCountriesOfBirthCardinality(gedcom.getPeople(), true)
                .forEach(pair -> System.out.println(pair.getLeft() + " (" + pair.getRight() + ")"));
    }

    @Test
    public void getAllPlaces() {
        System.out.println("getAllPlaces:");
        gedcomAnalyzerService
                .getAllPlaces(gedcom.getLegacyGedcom().orElseThrow(), true)
                .forEach(System.out::println);
    }

    @Test
    public void getSurnamesCardinalityByPlaceOfAnyEvent() {
        List<GedcomAnalyzerService.SurnamesCardinality> surnamesCardinalities = gedcomAnalyzerService
                .getSurnamesCardinalityByPlaceOfAnyEvent(gedcom.getPeople(), "Azul, Buenos Aires, Argentina", null, false);
        System.out.println("getSurnamesCardinalityByPlaceOfAnyEvent (" + surnamesCardinalities.size() + "):");
        surnamesCardinalities
                .stream()
                .limit(504)
                .forEach(cardinality -> {

                    List<String> variants = surnameService.getSurnameVariants(
                            cardinality.mainSurname(),
                            cardinality.variantsCardinality()
                                    .stream()
                                    .map(Pair::getLeft)
                                    .toList(),
                            gedcom.getProperties().getNormalizedSurnamesMap());

                    System.out.println(
                            cardinality.value()
                            + " - "
                            + cardinality.mainSurname().value()
                            + (!variants.isEmpty()
                                    ? variants
                                            .stream()
                                            .collect(Collectors.joining(", ", " (", ")"))
                                    : ""));
                });

        /*
        System.out.println("getSurnamesCardinalityByPlaceOfAnyEvent (2):");
        gedcomAnalyzerService
                .getSurnamesCardinalityByPlaceOfAnyEvent(gedcom.getPeople(), "Azul, Buenos Aires, Argentina", null)
                .stream()
                .limit(576)
                .forEach(cardinality -> System.out.println(
                        cardinality.mainSurname().normalizedMainWord()
                        + " - " + cardinality.value()
                        + " - " + Stream.concat(
                                        Stream.of(cardinality.mainSurname().value() + " (" + cardinality.mainSurnameCardinality() + ")"),
                                        cardinality.variantsCardinality()
                                                .stream()
                                                .map(pair -> pair.getLeft() + " (" + pair.getRight() + ")"))
                                .collect(Collectors.joining(", "))
                        + (cardinality.relatedNormalized().isEmpty()
                                ? ""
                                : " - Related: " + String.join(", ", cardinality.relatedNormalized()))));
         */
    }

    @Test
    public void getAncestryCountriesCardinalityByPlaceOfBirth() {
        System.out.println("getAncestryCountriesCardinalityByPlaceOfBirth:");
        gedcomAnalyzerService
                .getAncestryCountriesCardinalityByPlaceOfAnyEvent(gedcom.getPeople(), "Azul, Buenos Aires, Argentina", true, false)
                .forEach(cardinality -> System.out.println(
                        StringUtils.rightPad(cardinality.country(), 20)
                                + " - " + String.format("%5d", cardinality.cardinality())
                                + " - " + String.format("%5.2f%%", cardinality.percentage())
                                + " - " + cardinality.surnames()));
    }

    @Test
    public void findPersonsByPlaceOfBirth() {
        System.out.println("findPersonsByPlaceOfBirth:");
        List<EnrichedPerson> people = searchService.findPersonsBySurname("Arbío", null, gedcom);
        searchService
                .findPersonsByPlaceOfBirth("Argentina", null, null, false, people)
                .forEach(System.out::println);
    }

    @Test
    public void findPersonsByMonthAndDayOfBirth() {
        System.out.println("findPersonsByMonthAndDayOfBirth:");
        List<EnrichedPerson> people = searchService.findPersonsByName(
                "Emma",
                null,
                SexType.F,
                gedcom);
        searchService
                .findPersonsByMonthAndDayOfBirth(Month.AUGUST, 22, null, people)
                .forEach(System.out::println);
    }

    @Test
    public void findPersonsByMonthAndDayOfDeath() {
        System.out.println("findPersonsByMonthAndDayOfDeath:");
        searchService
                .findPersonsByMonthAndDayOfDeath(Month.APRIL, 2, null, gedcom.getPeople())
                .forEach(System.out::println);
    }

    @Test
    public void findPersonsByYearOfDeathAndNoParents() {
        System.out.println("findPersonsByYearOfDeathAndNoParents:");
        searchService
                .findPersonsByYearOfDeathAndNoParents(Year.of(2020), null, gedcom.getPeople())
                .forEach(System.out::println);
    }

    @Test
    public void getAstrologicalSignsCardinalityByPlaceOfBirth() {
        System.out.println("getAstrologicalSignsCardinalityByPlaceOfBirth:");
        List<EnrichedPerson> personsByPlaceOfBirth = searchService
                .findPersonsByPlaceOfBirth("Azul, Buenos Aires, Argentina", null, null, false, gedcom.getPeople());
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
        System.out.println("getMonthOfDeathCardinalityByPlaceOfDeath:");
        List<EnrichedPerson> personsByPlaceOfDeath = searchService
                .findPersonsByPlaceOfDeath("Azul, Buenos Aires, Argentina", null, false, gedcom.getPeople());
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
    public void getOlderAndLongestLivingPersons() {
        System.out.println("getOlderAndLongestLivingPersons:");
        List<EnrichedPerson> personsByPlace = searchService
                .findPersonsByPlaceOfAnyEvent("Azul, Buenos Aires, Argentina", null, null, false, gedcom.getPeople());
        List<EnrichedPerson> alivePersonsByPlace = searchService
                .findPersonsByPlaceOfAnyEvent("Azul, Buenos Aires, Argentina", true, null, false, gedcom.getPeople());
        List<EnrichedPerson> deadPersonsByPlace = searchService
                .findPersonsByPlaceOfAnyEvent("Azul, Buenos Aires, Argentina", false, null, false, gedcom.getPeople());
        List<EnrichedPerson> aliveMenByPlace = searchService
                .findPersonsByPlaceOfAnyEvent("Azul, Buenos Aires, Argentina", true, SexType.M, false, gedcom.getPeople())
                .stream()
                .sorted(PersonUtils.DATES_COMPARATOR)
                .toList();
        List<EnrichedPerson> aliveWomenByPlace = searchService
                .findPersonsByPlaceOfAnyEvent("Azul, Buenos Aires, Argentina", true, SexType.F, false, gedcom.getPeople())
                .stream()
                .sorted(PersonUtils.DATES_COMPARATOR)
                .toList();
        List<EnrichedPerson> deadMenByPlace = searchService
                .findPersonsByPlaceOfAnyEvent("Azul, Buenos Aires, Argentina", false, SexType.M, false, gedcom.getPeople())
                .stream()
                .filter(p -> p.getAge().isPresent())
                .sorted(PersonUtils.AGES_COMPARATOR.reversed())
                .toList();
        List<EnrichedPerson> deadWomenByPlace = searchService
                .findPersonsByPlaceOfAnyEvent("Azul, Buenos Aires, Argentina", false, SexType.F, false, gedcom.getPeople())
                .stream()
                .filter(p -> p.getAge().isPresent())
                .sorted(PersonUtils.AGES_COMPARATOR.reversed())
                .toList();
        System.out.println("\nTotal persons: " + personsByPlace.size());
        System.out.println("Alive persons: " + alivePersonsByPlace.size());
        System.out.println("Alive persons with day of birth: " + alivePersonsByPlace
                .stream()
                .filter(person -> person.getDateOfBirth().isPresent())
                .count());
        System.out.println("Dead persons: " + deadPersonsByPlace.size());
        System.out.println("Dead persons with day of birth and death: " + deadPersonsByPlace
                .stream()
                .filter(person -> person.getDateOfBirth().isPresent())
                .filter(person -> person.getDateOfDeath().isPresent())
                .count());
        System.out.println("\nOlder men: " + aliveMenByPlace.size());
        aliveMenByPlace
                .stream()
                .limit(8)
                .forEach(System.out::println);
        System.out.println("\nOlder women: " + aliveWomenByPlace.size());
        aliveWomenByPlace
                .stream()
                .limit(8)
                .forEach(System.out::println);
        System.out.println("\nLongest living men: " + deadMenByPlace.size());
        deadMenByPlace
                .stream()
                .limit(8)
                .forEach(System.out::println);
        System.out.println("\nLongest living women: " + deadWomenByPlace.size());
        deadWomenByPlace
                .stream()
                .limit(8)
                .forEach(System.out::println);
    }

    @Test
    public void findPersonsWithManyChildrenByPlaceOfBirth() {
        System.out.println("findPersonsWithManyChildrenByPlaceOfBirth:");
        Stream.concat(
                searchService
                        .findPersonsByPlaceOfBirth("Azul, Buenos Aires, Argentina", true, null, false, gedcom.getPeople())
                        .stream(),
                searchService
                        .findPersonsByPlaceOfBirth("Azul, Buenos Aires, Argentina", true, null, false, gedcom.getPeople())
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
        System.out.println("findDuplicatedPersons:");
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
    public void findDistinguishedPersons() {
        System.out.println("findDistinguishedPersons:");
        gedcom
                .getPeople()
                .stream()
                .filter(EnrichedPerson::isDistinguishedPerson)
                .forEach(System.out::println);
    }

    @Test
    public void findPeopleNonDistinguishedPersons() {
        System.out.println("findPeopleNonDistinguishedPersons:");
        gedcom
                .getPeople()
                .stream()
                .filter(person -> StringUtils.containsAnyIgnoreCase(person.getDisplayName(), "Mun.", "Pte.", "Diác.", "Padre ", "Sor ", "Mons.", "Cacique", "Gdor.", "Gdora.", "Bto.", "Bta.", "Cde.", "Cdesa.", "Pnt."))
                .filter(Predicate.not(EnrichedPerson::isDistinguishedPerson))
                .forEach(System.out::println);
    }

    @Test
    public void findDisappearedPersons() {
        System.out.println("findDisappearedPersons:");
        gedcom
                .getPeople()
                .stream()
                .filter(EnrichedPerson::isDisappearedPerson)
                .forEach(System.out::println);
    }

    @Test
    public void getPeopleInTree() {
        EnrichedPerson person = Objects.requireNonNull(gedcom.getPersonById(4));
        personService.setTransientProperties(person, true);

        System.out.println("setTransientProperties (excludeRootPerson):");
        System.out.println("personsCountInTree: " + person.getPersonsCountInTree());
        System.out.println("surnamesCountInTree: " + person.getSurnamesCountInTree());
        System.out.println("ancestryCountries: " + person.getAncestryCountries());
        System.out.println("ancestryGenerations: " + person.getAncestryGenerations());
        System.out.println("maxDistantRelationship: " + person.getMaxDistantRelationship().orElse(null));

        System.out.println("\ngetPeopleInTree:");
        personService
                .getPeopleInTree(person, false, false)
                .stream()
                // Order internal elements of each relationship group: first not-in-law, then in-law
                .map(relationships -> {
                    if (relationships.size() == 2 && relationships.findFirst().isInLaw()) {
                        return List.of(relationships.findLast(), relationships.findFirst());
                    }
                    return List.copyOf(relationships.getOrderedRelationships());
                })
                .sorted(Comparator.comparing(List::getFirst))
                .limit(50)
                .forEach(relationships -> System.out.println(
                        relationships
                                .stream()
                                .map(r -> relationshipMapper.toRelationshipDto(r, false))
                                .map(r -> relationshipMapper.formatInSpanish(r, false))
                                .map(FormattedRelationship::toString)
                                .collect(Collectors.joining(", "))
                        + "  --  "
                        + relationships.getFirst().person()));
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    public void getShortestPathsToPersons() {
        EnrichedPerson person = gedcom.getPersonById(4);
        Pair<Map<Integer, Integer>, Map<Integer, List<Integer>>> distancesAndPaths = PathUtils.calculateShortestPathFromSource(gedcom, person, true);

        System.out.println("getShortestPathsToPersons:");
        System.out.println("distance from I4 to I1 (" + gedcom.getPersonById(1).getDisplayName() + "): " + distancesAndPaths.getLeft().get(1));
        System.out.println("distance from I4 to I2 (" + gedcom.getPersonById(2).getDisplayName() + "): " + distancesAndPaths.getLeft().get(2));
        System.out.println("distance from I4 to I3 (" + gedcom.getPersonById(3).getDisplayName() + "): " + distancesAndPaths.getLeft().get(3));
        System.out.println("distance from I4 to I4 (" + gedcom.getPersonById(4).getDisplayName() + "): " + distancesAndPaths.getLeft().get(4));
        System.out.println("distance from I4 to I5 (" + gedcom.getPersonById(5).getDisplayName() + "): " + distancesAndPaths.getLeft().get(5));
        System.out.println("distance from I4 to I6 (" + gedcom.getPersonById(6).getDisplayName() + "): " + distancesAndPaths.getLeft().get(6));

        System.out.println();
        gedcom
                .getPeople()
                .stream()
                .filter(EnrichedPerson::isDistinguishedPerson)
                .sorted(Comparator
                        .<EnrichedPerson, Integer>comparing(d -> distancesAndPaths.getLeft().get(d.getId()), Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(d -> d.getSurname().map(Surname::simplified).orElse(null), Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(d -> d.getGivenName().map(GivenName::simplified).orElse(null), Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(distinguished -> System.out.println(StringUtils.rightPad(distinguished.getId().toString(), 8) + distinguished.getDisplayName() + ": " + distancesAndPaths.getLeft().get(distinguished.getId())));

        System.out.println();
        List<Integer> shortestPath = distancesAndPaths.getRight().getOrDefault(525113, List.of());
        for (int i = 0; i < shortestPath.size() - 1; i++) {
            EnrichedPerson personA = gedcom.getPersonById(shortestPath.get(i));
            EnrichedPerson personB = gedcom.getPersonById(shortestPath.get(i + 1));
            Relationship relationship = personService.getRelationshipBetween(personB, personA);
            RelationshipDto relationshipDto = relationshipMapper.toRelationshipDto(relationship, false);
            FormattedRelationship formattedRelationship = relationshipMapper.formatInSpanish(relationshipDto, false);
            if (i == 0) {
                System.out.println(displayPersonInfo(personA));
            }
            System.out.println("  " + formattedRelationship.relationshipDesc() + " de");
            System.out.println(displayPersonInfo(personB));
        }
    }

    private String displayPersonInfo(EnrichedPerson person) {
        boolean displayDate = !person.isAlive()
                && person
                        .getDateOfBirth()
                        .filter(date -> date.getOperator() == null
                                || date.getOperator() != Date.Operator.AFT && date.getOperator() != Date.Operator.BEF)
                        .isPresent();
        boolean displayPlace = person.getPlaceOfBirth().isPresent();
        String displayName = person.isAlive()
                ? "<nombre privado> " + person.getSurname().map(Surname::value).orElse("?")
                : person.getDisplayName();
        if (!displayDate && !displayPlace) {
            return displayName;
        }
        if (!displayDate) {
            return displayName + "  (" + person.getPlaceOfBirth().map(Place::country).orElseThrow() + ")";
        }
        String dateStr = person
                .getDateOfBirth()
                .map(date -> ((date.getOperator() == Date.Operator.EST || date.getOperator() == Date.Operator.ABT)
                        ? "aprox. " + date.getYear()
                        : String.valueOf(date.getYear())))
                .orElse("");
        if (!displayPlace) {
            return displayName + "  (" + dateStr + ")";
        }
        return displayName + "  (" + dateStr + ", " + person.getPlaceOfBirth().map(Place::country).orElseThrow() + ")";
    }

}
