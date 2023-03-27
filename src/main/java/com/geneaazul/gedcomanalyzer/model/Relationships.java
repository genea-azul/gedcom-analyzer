package com.geneaazul.gedcomanalyzer.model;

import org.springframework.util.Assert;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class Relationships {

    private final String personId;
    private final TreeSet<Relationship> relationships;
    private final boolean containsDirect;
    private final boolean containsNotSpouse;

    public static Relationships of(
            String personId,
            Relationship relationship) {
        return new Relationships(
                personId,
                newTreeSet(relationship),
                isDirectRelationship(relationship),
                !relationship.isSpouse());
    }

    public Relationships merge(
            Relationships other,
            RelationshipPriority relationshipPriority) {
        Assert.isTrue(this.personId.equals(other.personId), "Person ID must equal");
        TreeSet<Relationship> treeSet = mergeWithNonSpousePriority(this.relationships, other.relationships, relationshipPriority);
        return new Relationships(
                this.personId,
                treeSet,
                this.containsDirect || other.containsDirect,
                this.containsNotSpouse || other.containsNotSpouse);
    }

    public boolean contains(Relationship relationship) {
        return relationships.contains(relationship);
    }

    public boolean containsSpouseOf(Relationship relationship) {
        return relationships
                .stream()
                .anyMatch(relationship::isSpouseOf);
    }

    public Optional<Relationship> getFirstNonSpouseRelationship() {
        if (!containsNotSpouse) {
            return Optional.empty();
        }
        return relationships
                .stream()
                .filter(relationship -> !relationship.isSpouse())
                .findFirst();
    }

    private static TreeSet<Relationship> newTreeSet(Relationship relationship) {
        TreeSet<Relationship> set = new TreeSet<>();
        set.add(relationship);
        return set;
    }

    private static TreeSet<Relationship> mergeWithNonSpousePriority(
            Set<Relationship> t1,
            Set<Relationship> t2,
            RelationshipPriority relationshipPriority) {
        Assert.isTrue(t2.size() == 1, "Error");
        Relationship relationship = t2.iterator().next();

        Set<Relationship> relationships = t1;

        if (relationshipPriority == Relationships.RelationshipPriority.CLOSEST_SKIPPING_SPOUSE_WHEN_EXISTS_ANY_NON_SPOUSE
                && !relationships.isEmpty()) {
            relationships = Set.of();
        }

        if (!relationship.isSpouse()
                && !relationships.isEmpty()
                && (relationshipPriority.isSkipSpouseWhenAnyNonSpouse() || relationshipPriority.isSkipSpouseWhenNonSpouseOf())) {
            if (relationships
                    .stream()
                    .anyMatch(r
                            -> relationshipPriority.isSkipSpouseWhenAnyNonSpouse() && r.isSpouse()
                            || relationshipPriority.isSkipSpouseWhenNonSpouseOf() && r.isSpouse() && r.isSpouseOf(relationship))) {
                relationships = relationships
                        .stream()
                        .filter(r
                                -> relationshipPriority.isSkipSpouseWhenAnyNonSpouse() && !r.isSpouse()
                                || relationshipPriority.isSkipSpouseWhenNonSpouseOf() && (!r.isSpouse() || !r.isSpouseOf(relationship)))
                        .collect(Collectors.toSet());
            }
        }

        TreeSet<Relationship> result = new TreeSet<>(relationships);
        result.add(relationship);
        return result;
    }

    private static boolean isDirectRelationship(Relationship relationship) {
        return relationship.distanceToAncestor1() == 0 || relationship.distanceToAncestor2() == 0;
    }

    @Getter
    @RequiredArgsConstructor
    public enum RelationshipPriority {
        SKIP_SPOUSE_WHEN_EXISTS_NON_SPOUSE_OF(false, true),
        SKIP_ALL_SPOUSE_WHEN_EXISTS_ANY_NON_SPOUSE(true, false),
        CLOSEST_SKIPPING_SPOUSE_WHEN_EXISTS_ANY_NON_SPOUSE(true, false);

        private final boolean skipSpouseWhenAnyNonSpouse;
        private final boolean skipSpouseWhenNonSpouseOf;

    }

}
