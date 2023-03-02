package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.model.AncestryGenerations;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.Relationships;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;

@Service
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

    private void setNumberOfPeopleInTreeAndMaxDistantRelationship(EnrichedPerson person) {
        Map<String, Relationships> visitedPersons = new LinkedHashMap<>();
        traversePeopleInTree(
                person,
                null,
                visitedPersons,
                Sort.Direction.ASC,
                Relationship.empty(),
                Relationships.RelationshipPriority.CLOSEST_SKIPPING_SPOUSE_WHEN_EXISTS_ANY_NON_SPOUSE);

        Optional<Pair<String, Relationship>> maxDistantRelationshipNonSpouse = visitedPersons
                .values()
                .stream()
                .filter(relationships -> !relationships.getPersonId().equals(person.getId()))
                .map(relationships -> Pair.of(relationships.getPersonId(), relationships.getFirstNonSpouseRelationship()))
                .filter(pair -> pair.getRight().isPresent())
                .map(pair -> Pair.of(pair.getLeft(), pair.getRight().get()))
                .reduce((p1, p2) -> p1.getRight().compareTo(p2.getRight()) >= 0 ? p1 : p2);

        person.setNumberOfPeopleInTree(visitedPersons.size());
        person.setMaxDistantRelationship(maxDistantRelationshipNonSpouse);
    }

    public List<Pair<EnrichedPerson, Relationships>> getPeopleInTree(EnrichedPerson person) {
        Map<String, Relationships> visitedPersons = new LinkedHashMap<>();
        traversePeopleInTree(
                person,
                null,
                visitedPersons,
                Sort.Direction.ASC,
                Relationship.empty(),
                Relationships.RelationshipPriority.SKIP_SPOUSE_WHEN_EXISTS_NON_SPOUSE_OF);
        EnrichedGedcom gedcom = person.getGedcom();
        return visitedPersons
                .values()
                .stream()
                .map(relationships -> Pair.of(gedcom.getPersonById(relationships.getPersonId()), relationships))
                .toList();
    }

    private static void traversePeopleInTree(
            EnrichedPerson person,
            @Nullable String previousPersonId,
            Map<String, Relationships> visitedPersons,
            @Nullable Sort.Direction direction,
            Relationship relationship,
            Relationships.RelationshipPriority relationshipPriority) {

        boolean visited = visitedPersons.containsKey(person.getId());
        if (visited) {
            Relationships relationships = visitedPersons.get(person.getId());
            if (relationships.contains(relationship)) {
                return;
            }
            if (relationship.isSpouse()) {
                if (relationshipPriority.isSkipSpouseWhenAnyNonSpouse() && relationships.isContainsNotSpouse()) {
                    return;
                }
                if (relationshipPriority.isSkipSpouseWhenNonSpouseOf() && relationships.containsSpouseOf(relationship)) {
                    return;
                }
            }
            if (relationshipPriority == Relationships.RelationshipPriority.CLOSEST_SKIPPING_SPOUSE_WHEN_EXISTS_ANY_NON_SPOUSE) {
                if ((!relationship.isSpouse() && relationships.isContainsNotSpouse()
                        || relationship.isSpouse() && !relationships.isContainsNotSpouse())
                        && relationship.compareTo(relationships.getRelationships().first()) >= 0) {
                    return;
                }
            }
        }

        visitedPersons.merge(
                person.getId(),
                Relationships.of(person.getId(), relationship),
                (r1, r2) -> r1.merge(r2, relationshipPriority));

        if (relationship.getDistance() == 32) {
            // If max level or recursion is reached, stop the search
            return;
        }

        resolveRelatives(person, direction)
                .filter(relativeAndDirection -> !relativeAndDirection.person.getId().equals(previousPersonId))
                .forEach(relativeAndDirection -> traversePeopleInTree(
                        relativeAndDirection.person,
                        person.getId(),
                        visitedPersons,
                        relativeAndDirection.direction,
                        relationship.increase(
                                relativeAndDirection.direction,
                                isSetHalf(direction, relativeAndDirection.direction, previousPersonId, relativeAndDirection.person),
                                relativeAndDirection.relatedPersonIds),
                        relationshipPriority));
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

    private static Stream<A> resolveRelatives(
            EnrichedPerson person,
            @Nullable Sort.Direction direction) {

        if (direction == null) {
            return Stream.of();
        }

        Stream<B> relativesAndDirections = Stream.concat(
                Stream.of(
                        B.of(person.getSpouses(), null, newTreeSet(person.getId()))),
                person
                        .getSpousesWithChildren()
                        .stream()
                        .map(pair -> B.of(
                                pair.getRight(),
                                Sort.Direction.DESC,
                                pair.getLeft()
                                        .map(spouse -> newTreeSet(person.getId(), spouse.getId()))
                                        .orElseGet(() -> newTreeSet(person.getId())))));

        if (direction == Sort.Direction.ASC) {
            relativesAndDirections = Stream.concat(
                    Stream.of(
                            B.of(person.getParents(), Sort.Direction.ASC, newTreeSet(person.getId()))),
                    relativesAndDirections);
        }

        return relativesAndDirections
                .flatMap(b -> b.persons
                        .stream()
                        .map(relative -> A.of(relative, b.direction, b.relatedPersonIds)));
    }

    private static <E extends Comparable<E>> TreeSet<E> newTreeSet(E elem) {
        TreeSet<E> set = new TreeSet<>();
        set.add(elem);
        return set;
    }

    private static <E extends Comparable<E>> TreeSet<E> newTreeSet(E elem1, E elem2) {
        TreeSet<E> set = new TreeSet<>();
        set.add(elem1);
        set.add(elem2);
        return set;
    }

    private record A(
            EnrichedPerson person,
            @Nullable Sort.Direction direction,
            @Nullable SortedSet<String> relatedPersonIds) {

        public static A of(
                EnrichedPerson person,
                @Nullable Sort.Direction direction,
                @Nullable SortedSet<String> relatedPersonIds) {
            return new A(
                    person,
                    direction,
                    relatedPersonIds);
        }

    }

    private record B(
            List<EnrichedPerson> persons,
            @Nullable Sort.Direction direction,
            @Nullable SortedSet<String> relatedPersonIds) {

        public static B of(
                List<EnrichedPerson> persons,
                @Nullable Sort.Direction direction,
                @Nullable SortedSet<String> relatedPersonIds) {
            return new B(
                    persons,
                    direction,
                    relatedPersonIds);
        }

    }

}
