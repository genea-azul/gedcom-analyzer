package com.geneaazul.gedcomanalyzer.model;

import org.springframework.util.Assert;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class Relationships {

    private final String personId;
    private final TreeSet<Relationship> orderedRelationships;
    private final boolean containsDirect;
    private final boolean containsNotInLaw;

    public static Relationships of(
            String personId,
            Relationship relationship) {
        return new Relationships(
                personId,
                newTreeSet(relationship),
                isDirectRelationship(relationship),
                !relationship.isInLaw());
    }

    public Relationships merge(
            Relationships other,
            VisitedRelationshipTraversalStrategy traversalStrategy) {
        Assert.isTrue(this.personId.equals(other.personId), "Person ID must equal");
        TreeSet<Relationship> treeSet = mergeWithTraversalStrategy(this.orderedRelationships, other.orderedRelationships, traversalStrategy);
        return new Relationships(
                this.personId,
                treeSet,
                this.containsDirect || other.containsDirect,
                this.containsNotInLaw || other.containsNotInLaw);
    }

    public boolean contains(Relationship relationship) {
        return orderedRelationships.contains(relationship);
    }

    public boolean containsInLawOf(Relationship relationship) {
        return orderedRelationships
                .stream()
                .anyMatch(relationship::isInLawOf);
    }

    public Optional<Relationship> findFirst() {
        return orderedRelationships
                .stream()
                .findFirst();
    }

    public Optional<Relationship> findFirstNotInLaw() {
        return orderedRelationships
                .stream()
                .filter(relationship -> !relationship.isInLaw())
                .findFirst();
    }

    private static TreeSet<Relationship> newTreeSet(Relationship relationship) {
        TreeSet<Relationship> set = new TreeSet<>();
        set.add(relationship);
        return set;
    }

    /**
     * Merges the given relationships, it assumes the relationship in the 2nd argument is valid for merging.
     * - if 'closest distance', it is assumed the 2nd argument has a shorter distance
     * - if the 2nd argument is a 'in-law' relationship, it is assumed it matches the relationship rules in 1st argument
     * - if the 2nd argument is not a 'in-law' relationship, all the non-matching relationships in 1st argument will be discarded
     */
    private static TreeSet<Relationship> mergeWithTraversalStrategy(
            SortedSet<Relationship> t1,
            SortedSet<Relationship> t2,
            VisitedRelationshipTraversalStrategy traversalStrategy) {
        Assert.isTrue(t2.size() == 1, "Error");

        Collection<Relationship> relationships = t1;
        Relationship toMergeRelationship = t2.iterator().next();

        if (traversalStrategy.isClosestDistance() && !relationships.isEmpty()) {
            relationships = List.of();
        }

        if (!toMergeRelationship.isInLaw()
                && !relationships.isEmpty()
                && (traversalStrategy.isSkipInLawWhenAnyDistanceNonInLaw() || traversalStrategy.isSkipInLawWhenSameDistanceNotInLaw())) {
            if (relationships
                    .stream()
                    .anyMatch(r
                            -> traversalStrategy.isSkipInLawWhenAnyDistanceNonInLaw() && r.isInLaw()
                            || traversalStrategy.isSkipInLawWhenSameDistanceNotInLaw() && r.isInLaw() && r.isInLawOf(toMergeRelationship))) {
                relationships = relationships
                        .stream()
                        .filter(r
                                -> traversalStrategy.isSkipInLawWhenAnyDistanceNonInLaw() && !r.isInLaw()
                                || traversalStrategy.isSkipInLawWhenSameDistanceNotInLaw() && !(r.isInLaw() && r.isInLawOf(toMergeRelationship)))
                        .collect(Collectors.toList());
            }
        }

        TreeSet<Relationship> result = new TreeSet<>(relationships);
        result.add(toMergeRelationship);
        return result;
    }

    private static boolean isDirectRelationship(Relationship relationship) {
        return relationship.distanceToAncestor1() == 0 || relationship.distanceToAncestor2() == 0;
    }

    /**
     *
     */
    @Getter
    @RequiredArgsConstructor
    public enum VisitedRelationshipTraversalStrategy {
        INCLUDE_ALL(false, false, false),
        SKIP_IN_LAW_WHEN_EXISTS_SAME_DIST_NOT_IN_LAW(true, false, false),
        SKIP_IN_LAW_WHEN_EXISTS_ANY_DIST_NOT_IN_LAW(false, true, false),
        CLOSEST_SKIPPING_IN_LAW_WHEN_EXISTS_ANY_NOT_IN_LAW(false, true, true);

        private final boolean skipInLawWhenSameDistanceNotInLaw;
        private final boolean skipInLawWhenAnyDistanceNonInLaw;
        private final boolean closestDistance;

    }

}
