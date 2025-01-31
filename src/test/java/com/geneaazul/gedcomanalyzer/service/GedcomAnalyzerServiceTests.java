package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.Surname;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.DateUtils.AstrologicalSign;

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
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
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
    private GedcomAnalyzerProperties properties;

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
                .findPersonsByNameAndSpouseName(null, "Zarate", null, "Sosa", false, gedcom.getPeople())
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
                .filter(pair -> pair.getRight() > 10 || pair.getLeft().endsWith("Azul, Buenos Aires, Argentina"))
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
    public void findLastModified() {
        System.out.println("findLastModified:");
        ZonedDateTime minUpdateDate = ZonedDateTime.now(properties.getZoneId()).minusDays(5);
        gedcom
                .getPeople()
                .stream()
                .filter(person -> person.getUpdateDate().map(updateDate -> updateDate.isAfter(minUpdateDate)).orElse(false))
                .sorted(Comparator.comparing(person -> person.getUpdateDate().orElse(null)))
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
    public void findNativePeople() {
        System.out.println("findNativePeople:");
        gedcom
                .getPeople()
                .stream()
                .filter(EnrichedPerson::isNativePerson)
                .forEach(System.out::println);
    }

    @Test
    public void findDistinguishedPersons() {
        List<EnrichedPerson> distinguishedPeople = gedcom
                .getPeople()
                .stream()
                .filter(EnrichedPerson::isDistinguishedPerson)
                .toList();
        System.out.println("findDistinguishedPersons (" + distinguishedPeople.size() + "):");

        /* Default printing */
        distinguishedPeople
                .stream()
                .sorted(Comparator.comparing(
                        p -> (
                                p.isAlive()
                                        ? p.getDateOfBirth()
                                        : p.getDateOfDeath())
                                .filter(Date::isFullDate)
                                .map(date -> Pair.of(date.getMonth(), date.getDay()))
                                .orElse(null),
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(System.out::println);

        /* Generate HTMl <li> list */
        /*
        distinguishedPeople
                .stream()
                .sorted(Comparator
                        .<EnrichedPerson, String>comparing(
                                p -> p.getSurname().map(Surname::simplified).orElse(null),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(
                                p -> p.getGivenName().map(GivenName::simplified).orElse(null),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(p -> {
                    String nameWithFormat = PersonUtils.getDistinguishedPersonNameForSite(
                            p.getLegacyPerson().orElseThrow(),
                            properties.getNamePrefixesMap());
                    System.out.println("<li>" + nameWithFormat + "</li>");
                });
         */
    }

    @Test
    public void findPeopleNonDistinguishedPersons() {
        System.out.println("findPeopleNonDistinguishedPersons:");
        gedcom
                .getPeople()
                .stream()
                .filter(person -> StringUtils.containsAnyIgnoreCase(person.getDisplayName(), "Mun.", "Pte.", "Diác.", "Pbro.", "Padre ", "Rev.", "Sor ", "Mons.", "Cacique", "Gdor.", "Gdora.", "Bto.", "Bta.", "Cde.", "Cdesa.", "Pnt.", "Gral.", "Cnel.", "Ctan.", "Alfz.", "Tte. Cnel."))
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

}
