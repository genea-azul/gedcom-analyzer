package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.model.dto.TreeSideType;
import com.geneaazul.gedcomanalyzer.utils.SetUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiPredicate;
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

    // there is a in-law-of with lower distance or there is a (same in-law) with lower distance
    public boolean containsWithLowerDistance(Relationship relationship) {
        Assert.isTrue(this.personId.equals(relationship.person().getId()), "Person ID must equal");
        return relationship.isInLaw()
                && orderedRelationships
                        .stream()
                        .anyMatch(r
                                -> !r.isInLaw() && relationship.compareTo(r) >= 0
                                || r.isInLaw() && relationship.compareTo(r) >= 0);
    }

    public Relationship findFirst() {
        return orderedRelationships.first();
    }

    public Relationship findLast() {
        return orderedRelationships.last();
    }

    public int size() {
        return orderedRelationships.size();
    }

    public Optional<Relationship> findFirstNotInLaw() {
        return orderedRelationships
                .stream()
                .filter(relationship -> !relationship.isInLaw())
                .findFirst();
    }

    public void propagateTreeSidesToRelationships() {
        TreeSet<Relationship> withPropagatedTreeSides = orderedRelationships
                .stream()
                .map(relationship -> relationship.withTreeSides(treeSides))
                .collect(Collectors.toCollection(TreeSet::new));
        orderedRelationships.clear();
        orderedRelationships.addAll(withPropagatedTreeSides);
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
                && !relationships.isEmpty()) {
            if (relationships
                    .stream()
                    .anyMatch(r -> traversalStrategy.notInLawMatching.test(r, toMergeRelationship))) {
                relationships = relationships
                        .stream()
                        .filter(r -> !traversalStrategy.notInLawMatching.test(r, toMergeRelationship))
                        .collect(Collectors.toList());
            }
        } else if (toMergeRelationship.isInLaw()
                && !relationships.isEmpty()
                && traversalStrategy.type == VisitedRelationshipTraversalType.KEEP_CLOSER_IN_LAW_WHEN_EXISTS_ANY_NOT_IN_LAW) {
            if (relationships
                    .stream()
                    .anyMatch(Relationship::isInLaw)) {
                relationships = relationships
                        .stream()
                        .filter(r -> !r.isInLaw())
                        .collect(Collectors.toList());
            }
        }

        if (CollectionUtils.isNotEmpty(toMergeRelationship.treeSides())) {
            relationships = relationships
                    .stream()
                    .filter(toMergeRelationship::isTreeSideCompatible)
                    .toList();
        }

        Set<TreeSideType> treeSides = Stream
                .concat(
                        relationships
                                .stream()
                                .map(Relationship::treeSides)
                                .filter(Objects::nonNull)
                                .flatMap(Set::stream),
                        Stream
                                .ofNullable(toMergeRelationship.treeSides())
                                .flatMap(Set::stream))
                .collect(Collectors.toUnmodifiableSet());

        if (traversalStrategy.isClosestDistance()
                // for this strategy we don't need to clean the list
                && traversalStrategy.type != VisitedRelationshipTraversalType.KEEP_CLOSER_IN_LAW_WHEN_EXISTS_ANY_NOT_IN_LAW
                && !relationships.isEmpty()) {
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
        SKIP_IN_LAW_WHEN_EXISTS_SAME_DIST_NOT_IN_LAW(
                VisitedRelationshipTraversalType.SKIP_IN_LAW_WHEN_EXISTS_SAME_DIST_NOT_IN_LAW,
                false,
                // The visited in-law relationship appears also as not in-law (with lower distance) or as same in-law (with lower distance)
                Relationships::containsInLawOf,
                (rel, notInLawRel) -> rel.isInLaw() && rel.isInLawOf(notInLawRel)),
        SKIP_IN_LAW_WHEN_EXISTS_ANY_DIST_NOT_IN_LAW(
                VisitedRelationshipTraversalType.SKIP_IN_LAW_WHEN_EXISTS_ANY_DIST_NOT_IN_LAW,
                false,
                // The visited in-law relationship appears also as not in-law (with any distance)
                (rels, rel) -> rels.isContainsNotInLaw(),
                (rel, notInLawRel) -> rel.isInLaw()),
        CLOSEST_SKIPPING_IN_LAW_WHEN_EXISTS_ANY_NOT_IN_LAW(
                VisitedRelationshipTraversalType.SKIP_IN_LAW_WHEN_EXISTS_ANY_DIST_NOT_IN_LAW,
                true,
                // The visited in-law relationship appears also as not in-law (with any distance)
                (rels, rel) -> rels.isContainsNotInLaw(),
                (rel, notInLawRel) -> rel.isInLaw()),
        /*
         * When a person has a not in-law relationship, and also it has an in-law relationship closer than the not in-law.
         * i.e: a guy married to his aunt --> [distance 0, in-law, spouse] , [distance 3, not-in-law, aunt]
         */
        CLOSEST_KEEPING_CLOSER_IN_LAW_WHEN_EXISTS_ANY_NOT_IN_LAW(
                VisitedRelationshipTraversalType.KEEP_CLOSER_IN_LAW_WHEN_EXISTS_ANY_NOT_IN_LAW,
                true,
                // The visited in-law relationship appears also as not in-law (with the same distance)
                Relationships::containsWithLowerDistance,
                (rel, notInLawRel) -> !rel.isInLaw() || rel.compareTo(notInLawRel) >= 0);

        private final VisitedRelationshipTraversalType type;
        private final boolean closestDistance;
        private final BiPredicate<Relationships, Relationship> inLawMatching;
        private final BiPredicate<Relationship, Relationship> notInLawMatching;

    }

    @Getter
    @RequiredArgsConstructor
    public enum VisitedRelationshipTraversalType {
        SKIP_IN_LAW_WHEN_EXISTS_SAME_DIST_NOT_IN_LAW,
        SKIP_IN_LAW_WHEN_EXISTS_ANY_DIST_NOT_IN_LAW,
        /*
         * When a person has a not in-law relationship, and also it has an in-law relationship closer than the not in-law.
         * i.e: a guy married to his aunt --> [distance 0, in-law, spouse] , [distance 3, not-in-law, aunt]
         */
        KEEP_CLOSER_IN_LAW_WHEN_EXISTS_ANY_NOT_IN_LAW
    }

}
