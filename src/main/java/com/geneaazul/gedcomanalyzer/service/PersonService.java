package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.model.AncestryGenerations;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.Relationship;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
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

    public Integer getNumberOfPeopleInTree(EnrichedPerson person) {
        if (person.getNumberOfPeopleInTree() == null) {
            setNumberOfPeopleInTreeAndMaxDistantRelationship(person);
        }

        return person.getNumberOfPeopleInTree();
    }

    public Pair<String, Relationship> getMaxDistantRelationship(EnrichedPerson person) {
        if (person.getMaxDistantRelationship() == null) {
            setNumberOfPeopleInTreeAndMaxDistantRelationship(person);
        }

        return person
                .getMaxDistantRelationship()
                .orElse(null);
    }

    public void setNumberOfPeopleInTreeAndMaxDistantRelationship(EnrichedPerson person) {
        Map<String, Set<Relationship>> visitedPersons = new LinkedHashMap<>();
        traversePeopleInTree(person, null, visitedPersons, Sort.Direction.ASC, Relationship.empty());

        Optional<Pair<String, Relationship>> maxDistantRelationship = visitedPersons
                .entrySet()
                .stream()
                .filter(entry -> !entry.getKey().equals(person.getId()))
                .map(entry -> Pair.of(entry.getKey(), entry.getValue().iterator().next()))
                .filter(pair -> !pair.getRight().isSpouse())
                .reduce((p1, p2) -> p1.getRight().compareTo(p2.getRight()) >= 0 ? p1 : p2);

        person.setNumberOfPeopleInTree(visitedPersons.size());
        person.setMaxDistantRelationship(maxDistantRelationship);
    }

    public List<Pair<EnrichedPerson, Set<Relationship>>> getPeopleInTree(EnrichedPerson person) {
        Map<String, Set<Relationship>> visitedPersons = new LinkedHashMap<>();
        traversePeopleInTree(person, null, visitedPersons, Sort.Direction.ASC, Relationship.empty());
        EnrichedGedcom gedcom = person.getGedcom();
        return visitedPersons
                .entrySet()
                .stream()
                .map(entry -> Pair.of(gedcom.getPersonById(entry.getKey()), entry.getValue()))
                .toList();
    }

    private static void traversePeopleInTree(
            EnrichedPerson person,
            @Nullable String previousPersonId,
            Map<String, Set<Relationship>> visitedPersons,
            @Nullable Sort.Direction direction,
            Relationship relationship) {

        boolean visited = visitedPersons.containsKey(person.getId());
        if (visited) {
            if (containsRelationship(visitedPersons.get(person.getId()), relationship)) {
                return;
            }
            if (relationship.isSpouse() && containsSomeNotSpouse(visitedPersons.get(person.getId()))) {
                return;
            }
        }

        visitedPersons.merge(
                person.getId(),
                Set.of(relationship),
                PersonService::mergeToTreeSet);

        if (relationship.getDistance() == 32) {
            // If max level or recursion is reached, stop the search
            return;
        }

        resolveRelatives(person, direction)
                .filter(relativeAndDirection -> !relativeAndDirection.getLeft().getId().equals(previousPersonId))
                .forEach(relativeAndDirection -> traversePeopleInTree(
                        relativeAndDirection.getLeft(),
                        person.getId(),
                        visitedPersons,
                        relativeAndDirection.getRight(),
                        relationship.increase(
                                relativeAndDirection.getRight(),
                                isSetHalf(direction, relativeAndDirection.getRight(), previousPersonId, relativeAndDirection.getLeft()),
                                person.getId())));
    }

    private static boolean isSetHalf(
            Sort.Direction currentDirection,
            Sort.Direction nextDirection,
            @Nullable String previousPersonId,
            EnrichedPerson nextPersonToVisit) {

        if (previousPersonId == null || !(currentDirection == Sort.Direction.ASC && nextDirection == Sort.Direction.DESC)) {
            return false;
        }

        EnrichedPerson previousPerson = nextPersonToVisit.getGedcom().getPersonById(previousPersonId);

        // TODO consider biological/adopted parents
        if (previousPerson.getParents().size() != nextPersonToVisit.getParents().size()) {
            return false;
        }

        Set<String> parentIds = previousPerson
                .getParents()
                .stream()
                .map(EnrichedPerson::getId)
                .collect(Collectors.toSet());

        return nextPersonToVisit
                .getParents()
                .stream()
                .map(EnrichedPerson::getId)
                .anyMatch(personId -> !parentIds.contains(personId));
    }

    private static Set<Relationship> mergeToTreeSet(Set<Relationship> t1, Set<Relationship> t2) {
        Assert.isTrue(t2.size() == 1, "Error");
        Relationship relationship = t2.iterator().next();

        if (!relationship.isSpouse()) {
            // Clean spouse relationships if there's a new one not-spouse
            if (t1
                    .stream()
                    .anyMatch(Relationship::isSpouse)) {
                t1 = t1
                        .stream()
                        .filter(r -> !r.isSpouse())
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            }
        }

        TreeSet<Relationship> result = new TreeSet<>(t1);
        result.add(relationship);
        return result;
    }

    private static boolean containsRelationship(Set<Relationship> relationships, Relationship relationship) {
        return relationships.contains(relationship);
    }

    private static boolean containsSomeNotSpouse(Set<Relationship> relationships) {
        return relationships
                .stream()
                .anyMatch(relationship -> !relationship.isSpouse());
    }

    private static Stream<Pair<EnrichedPerson, Sort.Direction>> resolveRelatives(
            EnrichedPerson person,
            @Nullable Sort.Direction direction) {

        if (direction == null) {
            return Stream.of();
        }

        Stream<Pair<List<EnrichedPerson>, Sort.Direction>> relativesAndDirections = Sort.Direction.ASC == direction
                ? Stream.of(
                        Pair.of(person.getParents(), Sort.Direction.ASC),
                        Pair.of(person.getSpouses(), null),
                        Pair.of(person.getChildren(), Sort.Direction.DESC))
                : Stream.of(
                        Pair.of(person.getSpouses(), null),
                        Pair.of(person.getChildren(), Sort.Direction.DESC));

        return relativesAndDirections
                .flatMap(pair -> pair.getLeft()
                        .stream()
                        .map(relative -> Pair.of(relative, pair.getRight())));
    }

}
