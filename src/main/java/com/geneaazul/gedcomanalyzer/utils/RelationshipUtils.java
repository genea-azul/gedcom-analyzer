package com.geneaazul.gedcomanalyzer.utils;

import com.geneaazul.gedcomanalyzer.model.AncestryGenerations;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.Surname;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RelationshipUtils {

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
                        RelationshipUtils::mergeMaps)
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

    private static Map<String, Set<Surname>> mergeMaps(Map<String, Set<Surname>> map1, Map<String, Set<Surname>> map2) {
        Map<String, Set<Surname>> result = new HashMap<>(map1);
        map2
                .forEach((shortenedMainWord, surnamesSet) -> result.merge(shortenedMainWord, surnamesSet, RelationshipUtils::mergeSurnamesSets));
        return result;
    }

    public static List<String> getAncestryCountries(List<Relationship> relationships) {
        return relationships
                .stream()
                .reduce(Set.<String>of(),
                        (s, r) -> (r.isDirect() && r.getGeneration() >= 0 && !r.isInLaw())
                                ? r
                                        .person()
                                        .getCountryOfBirth()
                                        .map(country -> SetUtils.add(s, country))
                                        .orElse(s)
                                : s,
                        SetUtils::merge)
                .stream()
                .sorted()
                .toList();
    }

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
