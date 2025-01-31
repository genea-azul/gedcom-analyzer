package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.model.AncestryGenerations;
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
import com.geneaazul.gedcomanalyzer.service.familytree.FamilyTreeHelper;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.PathUtils;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;
import com.geneaazul.gedcomanalyzer.utils.PlaceUtils;
import com.geneaazul.gedcomanalyzer.utils.SetUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;

@SpringBootTest
@EnableConfigurationProperties
@ActiveProfiles("test")
public class HighLoadCalculationsTests {

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
    private FamilyTreeHelper familyTreeHelper;
    @Autowired
    private RelationshipMapper relationshipMapper;
    @Autowired
    private GedcomAnalyzerProperties properties;
    @Autowired
    private GedcomParsingService gedcomParsingService;

    private EnrichedGedcom gedcom;

    @BeforeEach
    public void setUp() {
        gedcom = gedcomHolder.getGedcom();
    }

    @Test
    public void getSurnamesCardinalityByPlaceOfAnyEvent() {
        List<GedcomAnalyzerService.SurnamesCardinality> surnamesCardinalities = gedcomAnalyzerService
                .getSurnamesCardinalityByPlaceOfAnyEvent(gedcom.getPeople(), "Azul, Buenos Aires, Argentina", null, true, false);
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

                    /* Default printing */
                    System.out.println(
                            cardinality.value()
                            + " - "
                            + cardinality.mainSurname().value()
                            + (!variants.isEmpty()
                                    ? variants
                                            .stream()
                                            .collect(Collectors.joining(", ", " (", ")"))
                                    : ""));

                    /* Generate HTMl <li> list */
                    /*
                    System.out.println(
                            "<li>"
                            + StringEscapeUtils.escapeHtml4(cardinality.mainSurname().value())
                            + (!variants.isEmpty()
                                    ? " <span class=\"text-secondary\">"
                                            + variants
                                                    .stream()
                                                    .map(StringEscapeUtils::escapeHtml4)
                                                    .collect(Collectors.joining(", ", "(", ")"))
                                            + "</span>"
                                    : "")
                            + "</li>");
                    */
                });
    }

    @Test
    public void getAncestryCountriesCardinalityByPlaceOfAnyEvent() {
        Boolean isAlive = null;
        List<GedcomAnalyzerService.SurnamesByCountryCardinality> places = gedcomAnalyzerService
                .getAncestryCountriesCardinalityByPlaceOfAnyEvent(gedcom.getPeople(), "Azul, Buenos Aires, Argentina", isAlive, true, false);
        int totalSurnames = places
                .stream()
                .mapToInt(GedcomAnalyzerService.SurnamesByCountryCardinality::cardinality)
                .sum();
        System.out.println("getAncestryCountriesCardinalityByPlaceOfAnyEvent: " + places.size() + " places and " + totalSurnames + " surnames");
        List<EnrichedPerson> people = searchService
                .findPersonsByPlaceOfAnyEvent("Azul, Buenos Aires, Argentina", isAlive, null, true, false, false, gedcom.getPeople());
        places
                .forEach(cardinality -> System.out.println(
                        StringUtils.rightPad(cardinality.country(), 20)
                                + " - " + String.format("%5d", cardinality.cardinality())
                                + " - " + String.format("%7.4f%%", cardinality.percentage())
                                + " - " + String.format("%7.4f%%", (float) cardinality.cardinality() / people.size() * 100)
                                + " - " + cardinality.surnames()));
    }

    @Test
    @Disabled
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void getImmigrantsCitiesCardinalityByPlaceOfAnyEvent() throws IOException {
        List<GedcomAnalyzerService.SurnamesByCityCardinality> places = gedcomAnalyzerService
                .getImmigrantsCitiesCardinalityByPlaceOfAnyEvent(
                        gedcom.getPeople(),
                        "Azul, Buenos Aires, Argentina",
                        null,
                        new String[]{ "Uruguay", "Brasil", "Chile", "Perú", "Paraguay", "Bolívia", "Océano Atlántico" },
                        null,
                        // includeSpousePlaces: relates to placeOfAnyEvent, set true for wider range of immigrants
                        true,
                        // includeAllChildrenPlaces: relates to placeOfAnyEvent, set true for wider range of immigrants
                        false,
                        // isExactPlace: relates to placeOfAnyEvent, set true to match exactly instead of "ends with" matching
                        false,
                        false,
                        false);
        int totalImmigrants = places
                .stream()
                .mapToInt(GedcomAnalyzerService.SurnamesByCityCardinality::cardinality)
                .sum();
        System.out.println("getImmigrantsCitiesCardinalityByPlaceOfAnyEvent: " + places.size() + " places and " + totalImmigrants + " immigrants");
        places
                .forEach(cardinality -> System.out.println(
                        StringUtils.leftPad(cardinality.country(), 80)
                                + " - " + String.format("%5d", cardinality.cardinality())
                                + " - " + String.format("%7.4f%%", cardinality.percentage())
                                + " - " + cardinality.surnames()
                                        .stream()
                                        .map(triple -> triple.getLeft() + " (" + triple.getMiddle() + ")")
                                        .toList()));

        if (!places.isEmpty()) {
            System.out.println("people by city: " + places.getFirst().country() + " (" + places.getFirst().persons().size() + ")");
            places
                    .getFirst()
                    .persons()
                    .stream()
                    .limit(200)
                    .forEach(System.out::println);

            /*
             * TODO ajustes a output.txt "^[^\s,]+ [^\s•] y "^([^\s,]+ )+\("
             *   -> Revisar: <b>.*[^\s,]+ \S
             *   -> Revisar: <b>.*([^\s,]+ )+\(
             *   -> De La Torre y Della Torre (Esp / Ita)
             *   -> Vassallo y Basalo (Ita / Esp)
             *   -> Martines (Por)
             *   -> Rodrigues (Por)
             *   -> Almeyda (Por)
             *   -> Limpiar provincias: Bordachar, Fittipaldi, Fortassin, Indo, Kollmann, Mocciaro, Saks, Sarasúa, Scavuzzo, Valicenti, Vitale
             *   -> Armentano (San Severino Lucano duplicado)
             */

            final int OUTPUT_FIXED_WIDTH_CHARS = 58;
            final Pattern COMPOSITE_SURNAME_PATTERN = Pattern.compile("^(.+) (y|dit|dite|dita|detto) .+$");

            Map<String, ImmigrantsResult> immigrantSurnames = places
                    .stream()
                    .flatMap(place -> {
                        String[] reversedPlaces = PlaceUtils.reversePlaceWords(place.country());
                        return Stream.concat(
                                place.surnames()
                                        .stream()
                                        .flatMap(surnameWithFrequency -> surnameWithFrequency.getRight()
                                                .stream()
                                                .map(immigrationDate -> new ImmigrantsOutput(
                                                        RegExUtils.replaceAll(StringUtils.remove(surnameWithFrequency.getLeft(), "?"), COMPOSITE_SURNAME_PATTERN, "$1"),
                                                        PersonUtils.getShortenedSurnameMainWord(surnameWithFrequency.getLeft(), properties.getNormalizedSurnamesMap()).get().normalizedMainWord(),
                                                        surnameWithFrequency.getMiddle(),
                                                        place.country(),
                                                        reversedPlaces,
                                                        immigrationDate))),
                                place.surnamesVariations()
                                        .stream()
                                        .map(surname -> new ImmigrantsOutput(
                                                RegExUtils.replaceAll(StringUtils.remove(surname, "?"), COMPOSITE_SURNAME_PATTERN, "$1"),
                                                PersonUtils.getShortenedSurnameMainWord(surname, properties.getNormalizedSurnamesMap()).get().normalizedMainWord(),
                                                0,
                                                place.country(),
                                                reversedPlaces,
                                                null)));
                    })
                    .collect(Collectors.groupingBy(
                            output -> output.normalizedSurname,
                            Collectors.collectingAndThen(
                                    Collectors.reducing(
                                            new ImmigrantsReduce(List.of(), Set.of(), null),
                                            output -> new ImmigrantsReduce(
                                                    List.of(
                                                            new ImmigrantsSurnameReduce(
                                                                    output.surname,
                                                                    getSurnameForSorting(output.surname),
                                                                    output.frequency)),
                                                    Set.of(
                                                            new ImmigrantsPlaces(
                                                                    output.place,
                                                                    output.reversedPlace)),
                                                    output.minImmigrationDate),
                                            (r1, r2) -> new ImmigrantsReduce(
                                                    mergeSurnamesLists(r1.surnames, r2.surnames),
                                                    SetUtils.merge(r1.places, r2.places),
                                                    ObjectUtils.compare(r1.minImmigrationDate, r2.minImmigrationDate, true) < 0
                                                            ? r1.minImmigrationDate
                                                            : r2.minImmigrationDate)),
                                    reduce -> new ImmigrantsResult(
                                            reduce.surnames
                                                    .stream()
                                                    .map(ImmigrantsSurnameReduce::surname)
                                                    .collect(Collectors.joining(", ")),
                                            reduce.surnames.getFirst().surnameForSorting,
                                            (reduce.places.size() == 1)
                                                    ? reduce.places
                                                            .stream()
                                                            .map(ImmigrantsPlaces::place)
                                                            .toList()
                                                    : reduce.places
                                                            .stream()
                                                            .filter(immiPlaces
                                                                    -> immiPlaces.reversedPlace.length > 2
                                                                    || reduce.places
                                                                            .stream()
                                                                            .filter(_p -> !_p.place.equals(immiPlaces.place))
                                                                            .noneMatch(_p -> _p.place.endsWith(immiPlaces.place)))
                                                            .sorted(Comparator.comparing(ImmigrantsPlaces::reversedPlace, PlaceUtils.REVERSED_PLACE_ARRAY_COMPARATOR))
                                                            .map(ImmigrantsPlaces::place)
                                                            .toList(),
                                            Optional.ofNullable(reduce.minImmigrationDate)
                                                    .map(date -> date.isOnlyYearDate()
                                                            && (date.getOperator() == Date.Operator.ABT || date.getOperator() == Date.Operator.EST)
                                                            ? "~" + date.getYear()
                                                            : date.getYear().toString())
                                                    .orElse(null)))));

            List<String> lines = immigrantSurnames.entrySet()
                    .stream()
                    .sorted(Comparator.comparing(entry -> entry.getValue().surnameForSorting))
                    .map(Map.Entry::getValue)
                    .flatMap(result -> Stream
                            .of(
                                    Stream.of(Optional.ofNullable(result.minImmigrationYear)
                                            .map(year -> "<span style=\"font-size:9.5pt;\"><b>" + result.surname + "</b></span>  •  " + year + "<br>")
                                            .orElseGet(() -> "<span style=\"font-size:9.5pt;\"><b>" + result.surname + "</b></span><br>")),
                                    result.places
                                            .stream()
                                            .map(place -> {
                                                if (place.length() > OUTPUT_FIXED_WIDTH_CHARS) {
                                                    if (place.contains(", Comunidad Valenciana")) {
                                                        place = place.replace(", Comunidad Valenciana", ", Com. Val.");
                                                    } else if (place.contains(", Castilla-La Mancha")) {
                                                        place = place.replace(", Castilla-La Mancha", ", Cast.-La Man.");
                                                    } else if (place.contains(", Galicia")) {
                                                        place = place.replace(", Galicia", ", Gal.");
                                                    } else if (place.contains(", Distrito Nacional")) {
                                                        place = place.replace(", Distrito Nacional", ", Dist. Nac.");
                                                    } else if (place.contains(", Aquitania")) {
                                                        place = place.replace(", Aquitania", ", Aquit.");
                                                    } else if (place.contains(", Mediodía-Pirineos")) {
                                                        place = place.replace(", Mediodía-Pirineos", ", Med.-Pir.");
                                                    } else if (place.contains(", Auvernia-Ródano-Alpes")) {
                                                        place = place.replace(", Auvernia-Ródano-Alpes", ", Auv.-Ród.-Alpes");
                                                    } else if (place.startsWith("Contrada Mezzana")) {
                                                        place = place.replace("Contrada Mezzana, ", "");
                                                    } else if (place.contains(", Mecklenburg-Vorpommern")) {
                                                        place = place.replace(", Mecklenburg-Vorpommern", ", Meck.-Vorp.");
                                                    } else if (place.contains("(")) {
                                                        place = StringUtils.substringBefore(place, " (") + StringUtils.substringAfter(place, ")");
                                                    }
                                                    if (place.length() > OUTPUT_FIXED_WIDTH_CHARS) {
                                                        if (place.contains(", Pirineos Atlánticos")) {
                                                            place = place.replace(", Pirineos Atlánticos", ", Pir. Atl.");
                                                        } else if (place.contains(", Auv.-Ród.-Alpes")) {
                                                            place = place.replace(", Auv.-Ród.-Alpes", ", Auv.-Ród.");
                                                        } else if (place.contains("(")) {
                                                            place = StringUtils.substringBefore(place, " (") + StringUtils.substringAfter(place, ")");
                                                        }
                                                    }
                                                    if (place.length() > OUTPUT_FIXED_WIDTH_CHARS) {
                                                        if (place.endsWith(", Francia")) {
                                                            place = place.replace(", Francia", ", Fran.");
                                                        }
                                                    }
                                                }
                                                return place;
                                            })
                                            .peek(place -> {
                                                if (place.length() > OUTPUT_FIXED_WIDTH_CHARS) {
                                                    System.err.println(place);
                                                } else if (place.length() == OUTPUT_FIXED_WIDTH_CHARS) {
                                                    System.out.println(place);
                                                }
                                            })
                                            .map(place -> {
                                                int padRepeat = Math.max(OUTPUT_FIXED_WIDTH_CHARS - place.length(), 0);
                                                String padding = StringUtils.repeat("&nbsp;", padRepeat);
                                                return "<span style=\"font-size:9.5pt;\">" + padding + place + "</span><br>";
                                            }),
                                    Stream.of("<span style=\"font-size:5pt;\">&nbsp;</span><br>"))
                            .flatMap(Function.identity()))
                    .collect(Collectors.toList());

            System.out.println();
            System.out.println("Surnames in ./target/output.md: " + immigrantSurnames.size());
            Files.write(Path.of("./target/output.md"), lines);
        }
    }

    private static String getSurnameForSorting(String surname) {
        return StringUtils.stripAccents(StringUtils.replace(StringUtils.lowerCase(surname), "ñ", "o "));
    }

    private static List<ImmigrantsSurnameReduce> mergeSurnamesLists(List<ImmigrantsSurnameReduce> l1, List<ImmigrantsSurnameReduce> l2) {
        if (l1.isEmpty()) {
            return l2;
        }
        if (l2.isEmpty()) {
            return l1;
        }

        List<ImmigrantsSurnameReduce> keepFromL1 = l1
                .stream()
                .map(valueL1 -> getSameMainWordWithOtherGrammarOrNull(valueL1, l2))
                .map(valueL1 -> getDePrefixedWithOtherGrammarOrNull(valueL1, l2))
                .filter(Objects::nonNull)
                .filter(valueL1 -> l2
                        .stream()
                        .noneMatch(valueL2 -> valueL1.surname.equals(valueL2.surname) && valueL1.frequency < valueL2.frequency))
                .toList();

        List<ImmigrantsSurnameReduce> keepFromL2 = l2
                .stream()
                .filter(valueL2 -> keepFromL1
                        .stream()
                        .map(ImmigrantsSurnameReduce::surname)
                        .noneMatch(valueL2.surname::equals))
                .map(valueL2 -> getSameMainWordWithOtherGrammarOrNull(valueL2, keepFromL1))
                .map(valueL2 -> getDePrefixedWithOtherGrammarOrNull(valueL2, keepFromL1))
                .filter(Objects::nonNull)
                .toList();

        // Always sort lists, even if one of them is empty
        return Stream.of(keepFromL1, keepFromL2)
                .flatMap(List::stream)
                .sorted(Comparator
                        .comparing(ImmigrantsSurnameReduce::frequency, Comparator.reverseOrder())
                        .thenComparing(ImmigrantsSurnameReduce::surnameForSorting))
                .toList();
    }

    private static ImmigrantsSurnameReduce getSameMainWordWithOtherGrammarOrNull(@Nullable ImmigrantsSurnameReduce test, List<ImmigrantsSurnameReduce> values) {
        if (test == null || values.isEmpty() || !test.surname.contains(" ")) {
            return test;
        }

        Optional<ImmigrantsSurnameReduce> prefixedSurname = values
                .stream()
                .filter(value -> test.surnameForSorting.startsWith(value.surnameForSorting + " "))
                .findFirst();

        if (prefixedSurname.isEmpty()) {
            return test;
        }

        int spacesOfMatchingPrefix = StringUtils.countMatches(prefixedSurname.get().surname, " ");
        int subStrEndPos = StringUtils.ordinalIndexOf(test.surname, " ", spacesOfMatchingPrefix + 1);

        String newValue = StringUtils.substring(test.surname, 0, subStrEndPos);
        String newValueForSorting = StringUtils.substring(test.surnameForSorting, 0, subStrEndPos);

        return newValue.equals(prefixedSurname.get().surname)
                ? null
                : new ImmigrantsSurnameReduce(newValue, newValueForSorting, test.frequency);
    }

    private static ImmigrantsSurnameReduce getDePrefixedWithOtherGrammarOrNull(@Nullable ImmigrantsSurnameReduce test, List<ImmigrantsSurnameReduce> values) {
        if (test == null || values.isEmpty() || !test.surname.startsWith("de ")) {
            return test;
        }

        Optional<ImmigrantsSurnameReduce> prefixedSurname = values
                .stream()
                .filter(value -> test.surnameForSorting.equals("de " + value.surnameForSorting))
                .findFirst();

        if (prefixedSurname.isEmpty()) {
            return test;
        }

        String newValue = StringUtils.substring(test.surname, 3);
        String newValueForSorting = StringUtils.substring(test.surnameForSorting, 3);

        return newValue.equals(prefixedSurname.get().surname)
                ? null
                : new ImmigrantsSurnameReduce(newValue, newValueForSorting, test.frequency);
    }

    private record ImmigrantsOutput(String surname, String normalizedSurname, int frequency, String place, String[] reversedPlace, @Nullable Date minImmigrationDate) { }
    private record ImmigrantsReduce(List<ImmigrantsSurnameReduce> surnames, Set<ImmigrantsPlaces> places, @Nullable Date minImmigrationDate) { }
    private record ImmigrantsSurnameReduce(String surname, String surnameForSorting, int frequency) { }
    private record ImmigrantsResult(String surname, String surnameForSorting, List<String> places, @Nullable String minImmigrationYear) { }
    private record ImmigrantsPlaces(String place, String[] reversedPlace) implements Comparable<ImmigrantsPlaces> {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ImmigrantsPlaces that)) return false;
            return Objects.equals(place, that.place);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(place);
        }

        @Override
        public int compareTo(ImmigrantsPlaces other) {
            return this.place.compareTo(other.place);
        }
    }

    @Test
    @Disabled
    public void getOlderAndLongestLivingPersons() {
        System.out.println("getOlderAndLongestLivingPersons:");
        List<EnrichedPerson> personsByPlace = searchService
                .findPersonsByPlaceOfAnyEvent("Azul, Buenos Aires, Argentina", null, null, true, false, false, gedcom.getPeople());
        List<EnrichedPerson> alivePersonsByPlace = searchService
                .findPersonsByPlaceOfAnyEvent("Azul, Buenos Aires, Argentina", true, null, true, false, false, gedcom.getPeople());
        List<EnrichedPerson> deadPersonsByPlace = searchService
                .findPersonsByPlaceOfAnyEvent("Azul, Buenos Aires, Argentina", false, null, true, false, false, gedcom.getPeople());
        List<EnrichedPerson> aliveMenByPlace = searchService
                .findPersonsByPlaceOfAnyEvent("Azul, Buenos Aires, Argentina", true, SexType.M, true, false, false, gedcom.getPeople())
                .stream()
                .sorted(PersonUtils.DATES_COMPARATOR)
                .toList();
        List<EnrichedPerson> aliveWomenByPlace = searchService
                .findPersonsByPlaceOfAnyEvent("Azul, Buenos Aires, Argentina", true, SexType.F, true, false, false, gedcom.getPeople())
                .stream()
                .sorted(PersonUtils.DATES_COMPARATOR)
                .toList();
        List<EnrichedPerson> deadMenByPlace = searchService
                .findPersonsByPlaceOfAnyEvent("Azul, Buenos Aires, Argentina", false, SexType.M, true, false, false, gedcom.getPeople())
                .stream()
                .filter(p -> p.getAge().isPresent())
                .sorted(PersonUtils.AGES_COMPARATOR.reversed())
                .toList();
        List<EnrichedPerson> deadWomenByPlace = searchService
                .findPersonsByPlaceOfAnyEvent("Azul, Buenos Aires, Argentina", false, SexType.F, true, false, false, gedcom.getPeople())
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
        System.out.println("First event: " + personsByPlace
                .stream()
                .min((p1, p2) -> {
                    Optional<Date> date1 = p1.getDatesOfAnyEvent()
                            .stream()
                            .min(Date::compareTo);
                    Optional<Date> date2 = p2.getDatesOfAnyEvent()
                            .stream()
                            .min(Date::compareTo);
                    return date2
                            .map(value -> date1
                                    .map(date -> date.compareTo(value))
                                    .orElse(1))
                            .orElseGet(() -> date1.isEmpty() ? 0 : -1);
                })
                .orElse(null));
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
    public void getPeopleInTree() {
        EnrichedPerson person = Objects.requireNonNull(gedcom.getPersonById(4));
        List<List<Relationship>> relationshipsList = familyTreeHelper.getRelationshipsWithNotInLawPriority(person);

        System.out.println("setTransientProperties (excludeRootPerson = false):");
        System.out.println("personsCountInTree: " + person.getPersonsCountInTree());
        System.out.println("surnamesCountInTree: " + person.getSurnamesCountInTree());
        System.out.println("ancestryCountries: " + person.getAncestryCountries());
        System.out.println("ancestryGenerations: " + person.getAncestryGenerations());
        System.out.println("maxDistantRelationship: " + person.getMaxDistantRelationship().orElse(null));

        System.out.println("\ngetPeopleInTree:");
        relationshipsList
                .stream()
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
        Pair<Map<Integer, Integer>, Map<Integer, List<Integer>>> distancesAndPaths = PathUtils.calculateShortestPathFromSource(gedcom, person, true, true);

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

    @Test
    public void generateSubGedcom() throws IOException {
        // I9: Son B&A
        EnrichedPerson person = Objects.requireNonNull(gedcom.getPersonById(9));
        List<List<Relationship>> relationshipsList = familyTreeHelper.getRelationshipsWithNotInLawPriority(person);
        System.out.println("generateSubGedcom: " + person.getDisplayName() + " - People count: " + relationshipsList.size());
        gedcomParsingService.format(
                gedcom.getLegacyGedcom().get(),
                relationshipsList,
                properties.getTempDir().resolve("sub-gedcom-test.ged"),
                250,
                4,
                3);
    }

    @Test
    @Disabled
    public void getPeopleStatsByPlaceOfAnyEvent() {
        System.out.println("getPeopleStatsByPlaceOfAnyEvent:");
        List<EnrichedPerson> people = searchService
                .findPersonsByPlaceOfAnyEvent("Azul, Buenos Aires, Argentina", true, null, true, false, false, gedcom.getPeople());

        people
                .forEach(person -> personService.setTransientProperties(person, false));

        int maxPersonCountInTree = people
                .stream()
                .mapToInt(EnrichedPerson::getPersonsCountInTree)
                .max()
                .orElseThrow();
        List<EnrichedPerson> maxPersonCountInTreePeople = people
                .stream()
                .filter(person -> person.getPersonsCountInTree() == maxPersonCountInTree)
                .toList();

        int maxAncestryCountries = people
                .stream()
                .map(EnrichedPerson::getAncestryCountries)
                .mapToInt(List::size)
                .max()
                .orElseThrow();
        List<EnrichedPerson> maxAncestryCountriesPeople = people
                .stream()
                .filter(person -> person.getAncestryCountries().size() >= maxAncestryCountries - 1)
                .toList();

        int maxAncestryGenerationsAscending = people
                .stream()
                .map(EnrichedPerson::getAncestryGenerations)
                .mapToInt(AncestryGenerations::ascending)
                .max()
                .orElseThrow();
        List<EnrichedPerson> maxAncestryGenerationsAscendingPeople = people
                .stream()
                .filter(person -> person.getAncestryGenerations().ascending() == maxAncestryGenerationsAscending)
                .toList();

        int maxDistantRelationshipDistance = people
                .stream()
                .mapToInt(p -> p.getMaxDistantRelationship().map(Relationship::getDistance).orElse(0))
                .max()
                .orElseThrow();
        List<EnrichedPerson> maxMaxDistantRelationshipDistancePeople = people
                .stream()
                .filter(person -> person.getMaxDistantRelationship().map(Relationship::getDistance).orElse(0) == maxDistantRelationshipDistance)
                .toList();

        int maxDistinguishedPersonsInTree = people
                .stream()
                .map(EnrichedPerson::getDistinguishedPersonsInTree)
                .mapToInt(List::size)
                .max()
                .orElseThrow();
        List<EnrichedPerson> maxDistinguishedPersonsInTreePeople = people
                .stream()
                .filter(person -> person.getDistinguishedPersonsInTree().size() == maxDistinguishedPersonsInTree)
                .toList();

        System.out.println();
    }

}
