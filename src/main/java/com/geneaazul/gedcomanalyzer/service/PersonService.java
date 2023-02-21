package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.model.AncestryGenerations;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.dto.ReferenceType;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PersonService {

    public List<String> getAncestryCountries(EnrichedPerson person) {
        if (person.getAncestryCountries() == null) {
            Set<String> visitedPersons = new HashSet<>();
            List<String> ancestryCountries = person
                    .getParents()
                    .stream()
                    .map(parent -> getAncestryCountries(parent, visitedPersons, 0))
                    .flatMap(Set::stream)
                    .distinct()
                    .sorted()
                    .toList();
            person.setAncestryCountries(ancestryCountries);
        }

        return person.getAncestryCountries();
    }

    private static Set<String> getAncestryCountries(
            EnrichedPerson person,
            Set<String> visitedPersons,
            int level) {

        // Corner case: parents are cousins -> skip visiting a person twice
        if (visitedPersons.contains(person.getId())) {
            return Set.of();
        }

        visitedPersons.add(person.getId());

        if (level == 20) {
            // If max level or recursion is reached, stop the search
            return person.getCountryOfBirthForSearch()
                    .map(Set::of)
                    .orElseGet(Set::of);
        }

        Set<String> ancestryCountries = person
                .getParents()
                .stream()
                .map(parent -> getAncestryCountries(
                        parent,
                        visitedPersons,
                        level + 1))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        // Add person's country to the set of ancestry countries
        person.getCountryOfBirthForSearch()
                .ifPresent(ancestryCountries::add);

        return ancestryCountries;
    }

    public AncestryGenerations getAncestryGenerations(EnrichedPerson person) {
        if (person.getAncestryGenerations() == null) {
            int ascendingGenerations = getAncestryGenerations(person, new HashSet<>(), EnrichedPerson::getParents, 0, 0);
            int descendingGenerations = getAncestryGenerations(person, new HashSet<>(), EnrichedPerson::getChildren, 0, 0);
            person.setAncestryGenerations(AncestryGenerations.of(ascendingGenerations, descendingGenerations));
        }

        return person.getAncestryGenerations();
    }

    private static int getAncestryGenerations(
            EnrichedPerson person,
            Set<String> visitedPersons,
            Function<EnrichedPerson, List<EnrichedPerson>> relativesResolver,
            int level,
            int maxLevel) {

        // Corner case: parents are cousins -> skip visiting
        // Corner case: parent is cousin of spouse's parent -> visit higher distance
        if (visitedPersons.contains(person.getId()) && level <= maxLevel) {
            return level;
        }

        visitedPersons.add(person.getId());

        if (level == 20) {
            // If max level or recursion is reached, stop the search
            return level;
        }

        MutableInt maxLevelHolder = new MutableInt(maxLevel);

        return relativesResolver
                .apply(person)
                .stream()
                .map(parent -> {
                    int generations = getAncestryGenerations(
                            parent,
                            visitedPersons,
                            relativesResolver,
                            level + 1,
                            maxLevelHolder.getValue());

                    int newMaxLevel = Math.max(maxLevelHolder.getValue(), generations);
                    maxLevelHolder.setValue(newMaxLevel);

                    return generations;
                })
                .reduce(Integer::max)
                .orElse(level);
    }

    public int getNumberOfPeopleInTree(EnrichedPerson person) {
        if (person.getNumberOfPeopleInTree() == null) {
            Set<String> visitedPersons = new HashSet<>();
            traversePeopleInTree(person, visitedPersons, null, Sort.Direction.ASC, 0);
            person.setNumberOfPeopleInTree(visitedPersons.size());
        }

        return person.getNumberOfPeopleInTree();
    }

    public List<EnrichedPerson> getPeopleInTree(EnrichedPerson person) {
        Set<String> visitedPersons = new LinkedHashSet<>();
        traversePeopleInTree(person, visitedPersons, null, Sort.Direction.ASC, 0);
        EnrichedGedcom gedcom = person.getGedcom();
        return visitedPersons
                .stream()
                .map(gedcom::getPersonById)
                .toList();
    }

    private static void traversePeopleInTree(
            EnrichedPerson person,
            Set<String> visitedPersons,
            ReferenceType referenceType,
            Sort.Direction direction,
            int level) {

        boolean visited = visitedPersons.contains(person.getId());
        if (visited && !(referenceType == ReferenceType.CHILD && direction == Sort.Direction.ASC)) {
            return;
        }

        visitedPersons.add(person.getId());

        if (level == 32) {
            // If max level or recursion is reached, stop the search
            return;
        }

        resolveRelatives(person, direction)
                .forEach(relativeAndDirection -> traversePeopleInTree(
                        relativeAndDirection.getLeft(),
                        visitedPersons,
                        relativeAndDirection.getMiddle(),
                        relativeAndDirection.getRight(),
                        level + 1));
    }

    private static Stream<Triple<EnrichedPerson, ReferenceType, Sort.Direction>> resolveRelatives(
            EnrichedPerson person,
            @Nullable Sort.Direction direction) {

        if (direction == null) {
            return Stream.of();
        }

        Stream<Triple<List<EnrichedPerson>, ReferenceType, Sort.Direction>> relativesAndDirections = Sort.Direction.ASC == direction
                ? Stream.of(
                        Triple.of(person.getParents(), ReferenceType.CHILD, Sort.Direction.ASC),
                        Triple.of(person.getSpouses(), ReferenceType.SPOUSE, null),
                        Triple.of(person.getChildren(), ReferenceType.PARENT, Sort.Direction.DESC))
                : Stream.of(
                        Triple.of(person.getSpouses(), ReferenceType.SPOUSE, null),
                        Triple.of(person.getChildren(), ReferenceType.PARENT, Sort.Direction.DESC));

        return relativesAndDirections
                .flatMap(triple -> triple.getLeft()
                        .stream()
                        .map(relative -> Triple.of(relative, triple.getMiddle(), triple.getRight())));
    }

}
