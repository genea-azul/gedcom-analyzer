package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.model.dto.TreeSideType;
import com.geneaazul.gedcomanalyzer.utils.SetUtils;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor(staticName = "of")
@ToString(onlyExplicitlyIncluded = true)
public class Relationships {

    @ToString.Include
    private final String personId;
    @ToString.Include
    private final TreeSet<Relationship> orderedRelationships;
    @ToString.Include
    private final boolean containsDirect;
    @ToString.Include
    private final boolean containsNotInLaw;
    @ToString.Include
    @Nullable
    private final Set<TreeSideType> treeSides;

    public static Relationships from(Relationship relationship) {
        return new Relationships(
                relationship.person().getId(),
                newTreeSet(relationship),
                relationship.isDirect(),
                !relationship.isInLaw(),
                relationship.treeSides());
    }

    public Relationships merge(
            Relationships other,
            VisitedRelationshipTraversalStrategy traversalStrategy) {
        Assert.isTrue(this.personId.equals(other.personId), "Person ID must equal");
        Pair<TreeSet<Relationship>, Set<TreeSideType>> relationshipsAndTreeSides =
                mergeWithTraversalStrategy(this.orderedRelationships, other.orderedRelationships, traversalStrategy);
        return new Relationships(
                this.personId,
                relationshipsAndTreeSides.getLeft(),
                this.containsDirect || other.containsDirect,
                this.containsNotInLaw || other.containsNotInLaw,
                relationshipsAndTreeSides.getRight());
    }

    public Relationships mergeTreeSides(Relationships other) {
        if (SetUtils.containsAll(this.treeSides, other.treeSides)) {
            return this;
        }
        return new Relationships(
                this.personId,
                this.orderedRelationships,
                this.containsDirect,
                this.containsNotInLaw,
                SetUtils.merge(this.treeSides, other.treeSides));
    }

    public boolean contains(Relationship relationship) {
        return orderedRelationships.contains(relationship);
    }

    public boolean containsInLawOf(Relationship relationship) {
        Assert.isTrue(this.personId.equals(relationship.person().getId()), "Person ID must equal");
        return orderedRelationships
                .stream()
                .anyMatch(relationship::isInLawOf);
    }

    public Relationship findFirst() {
        return orderedRelationships
                .first();
    }

    @SuppressWarnings("unused")
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
    private static Pair<TreeSet<Relationship>, Set<TreeSideType>> mergeWithTraversalStrategy(
            SortedSet<Relationship> t1,
            SortedSet<Relationship> t2,
            VisitedRelationshipTraversalStrategy traversalStrategy) {
        Assert.isTrue(t2.size() == 1, "Error");

        Collection<Relationship> relationships = t1;
        Relationship toMergeRelationship = t2.iterator().next();

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

        Set<TreeSideType> treeSides = Stream
                .concat(
                        relationships
                                .stream()
                                .map(Relationship::treeSides)
                                .filter(Objects::nonNull)
                                .flatMap(Set::stream),
                        Stream.ofNullable(
                                toMergeRelationship.treeSides())
                                .flatMap(Set::stream))
                .collect(Collectors.toUnmodifiableSet());

        if (traversalStrategy.isClosestDistance() && !relationships.isEmpty()) {
            relationships = List.of();
        }

        TreeSet<Relationship> result = new TreeSet<>(relationships);
        result.add(toMergeRelationship);

        return Pair.of(result, treeSides.isEmpty() ? null : treeSides);
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
