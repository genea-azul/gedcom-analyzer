package com.geneaazul.gedcomanalyzer.utils;

import com.geneaazul.gedcomanalyzer.model.AncestryGenerations;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.Place;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.Surname;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RelationshipUtils {

    public static Set<String> getCountriesOfBirth(List<Relationship> relationships) {
        return getCountriesOfBirth(relationships, _ -> true, s -> s);
    }

    public static <T extends Collection<String>> T getCountriesOfBirth(
            List<Relationship> relationships,
            Predicate<Relationship> filter,
            Function<Set<String>, T> finisher) {
        return relationships
                .stream()
                .filter(filter)
                .map(Relationship::person)
                .map(EnrichedPerson::getPlaceOfBirth)
                .flatMap(Optional::stream)
                .map(Place::country)
                .collect(Collectors.collectingAndThen(Collectors.toSet(), finisher));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static List<Pair<EnrichedPerson, String>> getImmigrantsCitiesOfBirth(
            List<Relationship> relationships,
            Predicate<EnrichedPerson> isImmigrantWithCityCondition) {
        return relationships
                .stream()
                .map(Relationship::person)
                .filter(isImmigrantWithCityCondition)
                .filter(StreamUtils.distinctByKey(EnrichedPerson::getId))
                .map(person -> Pair.of(person, StringUtils.substringBeforeLast(person.getPlaceOfBirth().get().name(), ",").trim()))
                .toList();
    }

    public static Integer getSurnamesCount(List<Relationship> relationships) {
        return relationships
                .stream()
                .map(Relationship::person)
                .map(EnrichedPerson::getSurname)
                .flatMap(Optional::stream)
                .reduce(
                        new HashMap<>(),
                        (map, surname) -> {
                            map.merge(
                                    surname.shortenedMainWord(),
                                    Set.of(surname),
                                    RelationshipUtils::mergeSurnamesSets);
                            return map;
                        },
                        RelationshipUtils::mergeSurnameMaps)
                .values()
                .stream()
                .mapToInt(Set::size)
                .sum();
    }

    private static Set<Surname> mergeSurnamesSets(Set<Surname> set1, Set<Surname> set2) {
        Set<Surname> result = new HashSet<>(set1);
        set2
                .forEach(surname -> {
                    if (set1.stream().noneMatch(surname::matches)) {
                        result.add(surname);
                    }
                });
        return result;
    }

    private static Map<String, Set<Surname>> mergeSurnameMaps(Map<String, Set<Surname>> map1, Map<String, Set<Surname>> map2) {
        Map<String, Set<Surname>> result = new HashMap<>(map1);
        map2
                .forEach((shortenedMainWord, surnamesSet) -> result.merge(shortenedMainWord, surnamesSet, RelationshipUtils::mergeSurnamesSets));
        return result;
    }

    /*public static List<String> getAncestryCountries(List<Relationship> relationships) {
        return relationships
                .stream()
                .filter()
                .map(Relationship::person)
                .map(EnrichedPerson::getPlaceOfBirth)
                .flatMap(Optional::stream)
                .map(Place::country)
                .collect(Collectors.collectingAndThen(Collectors.toSet(), set -> set.stream().sorted().toList()));
    }*/

    public static AncestryGenerations getAncestryGenerations(List<Relationship> relationships) {
        return relationships
                .stream()
                .reduce(AncestryGenerations.empty(), AncestryGenerations::mergeRelationship, AncestryGenerations::merge);
    }

    public static Optional<Relationship> getMaxDistantRelationship(List<Relationship> relationships) {
        return relationships
                .stream()
                .reduce((r1, r2) -> r1.compareToWithInvertedPriority(r2) >= 0 ? r1 : r2);
    }

}
