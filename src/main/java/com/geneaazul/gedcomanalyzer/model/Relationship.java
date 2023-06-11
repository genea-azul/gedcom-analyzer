package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.model.dto.TreeSideType;
import com.geneaazul.gedcomanalyzer.utils.CollectionComparator;

import org.springframework.data.domain.Sort;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

public record Relationship(
        EnrichedPerson person,
        int distanceToAncestorRootPerson,
        int distanceToAncestorThisPerson,
        boolean isInLaw,
        boolean isHalf,
        @Nullable Set<TreeSideType> treeSides,
        @Nullable List<String> relatedPersonIds) implements Comparable<Relationship> {

    public static Relationship of(
            EnrichedPerson person,
            int ascending,
            int descending,
            boolean isInLaw,
            boolean isHalf,
            @Nullable Set<TreeSideType> treeSides,
            @Nullable List<String> relatedPersonIds) {
        return new Relationship(
                person,
                ascending,
                descending,
                isInLaw,
                isHalf,
                treeSides,
                relatedPersonIds);
    }

    public static Relationship empty(EnrichedPerson person) {
        return new Relationship(
                person,
                0,
                0,
                false,
                false,
                null,
                null);
    }

    public int getDistance() {
        return distanceToAncestorRootPerson + distanceToAncestorThisPerson;
    }

    public int compareDistance(Relationship other) {
        int distance1 = this.getDistance();
        int distance2 = other.getDistance();
        return Integer.compare(distance1, distance2);
    }

    public Relationship increase(
            EnrichedPerson person,
            Sort.Direction direction,
            boolean isSetHalf,
            Set<TreeSideType> treeSides,
            List<String> relatedPersonIds) {

        if (isInLaw || isHalf && direction == Sort.Direction.ASC || isHalf && isSetHalf) {
            throw new UnsupportedOperationException();
        }
        if (direction == Sort.Direction.ASC) {
            return Relationship.of(
                    person,
                    distanceToAncestorRootPerson + 1,
                    distanceToAncestorThisPerson,
                    false,
                    false,
                    treeSides,
                    relatedPersonIds);
        }
        if (direction == Sort.Direction.DESC) {
            return Relationship.of(
                    person,
                    distanceToAncestorRootPerson,
                    distanceToAncestorThisPerson + 1,
                    false,
                    isHalf || isSetHalf,
                    treeSides,
                    relatedPersonIds);
        }
        return Relationship.of(
                person,
                distanceToAncestorRootPerson,
                distanceToAncestorThisPerson,
                true,
                isHalf,
                treeSides,
                relatedPersonIds);
    }

    public boolean isInLawOf(Relationship other) {
        return this.distanceToAncestorRootPerson == other.distanceToAncestorRootPerson
                && this.distanceToAncestorThisPerson == other.distanceToAncestorThisPerson
                && this.isInLaw != other.isInLaw
                && this.isHalf == other.isHalf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Relationship that = (Relationship) o;
        return Objects.equals(person.getId(), that.person.getId())
                && distanceToAncestorRootPerson == that.distanceToAncestorRootPerson
                && distanceToAncestorThisPerson == that.distanceToAncestorThisPerson
                && isInLaw == that.isInLaw
                && isHalf == that.isHalf
                && Objects.equals(this.treeSides, that.treeSides)
                && Objects.equals(this.relatedPersonIds, that.relatedPersonIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                person.getId(),
                distanceToAncestorRootPerson,
                distanceToAncestorThisPerson,
                isInLaw,
                isHalf,
                treeSides,
                relatedPersonIds);
    }

    @Override
    public int compareTo(@NotNull Relationship other) {
        // min distance -> is lower priority
        int compareDistance = compareDistance(other);
        if (compareDistance != 0) {
            return compareDistance;
        }
        // min generation difference -> is lower priority
        int thisGeneration = Math.abs(this.distanceToAncestorRootPerson - this.distanceToAncestorThisPerson);
        int otherGeneration = Math.abs(other.distanceToAncestorRootPerson - other.distanceToAncestorThisPerson);
        int compareGeneration = Integer.compare(thisGeneration, otherGeneration);
        if (compareGeneration != 0) {
            return compareGeneration;
        }
        // max distance to ancestor root person -> is lower priority
        int compareDistanceToAncestor = Integer.compare(this.distanceToAncestorRootPerson, other.distanceToAncestorRootPerson);
        if (compareDistanceToAncestor != 0) {
            return Math.negateExact(compareDistanceToAncestor);
        }
        //
        int compareTreeSides = TREE_SIDE_TYPE_COLLECTION_COMPARATOR.compare(this.treeSides, other.treeSides);
        if (compareTreeSides != 0) {
            return compareTreeSides;
        }
        // not is-half -> is lower priority
        int compareIsHalf = Boolean.compare(this.isHalf, other.isHalf);
        if (compareIsHalf != 0) {
            return compareIsHalf;
        }
        // not in-law -> is lower priority
        int compareIsInLaw = Boolean.compare(this.isInLaw, other.isInLaw);
        if (compareIsInLaw != 0) {
            return compareIsInLaw;
        }
        int compareRelatedPersonIds = STRING_COLLECTION_COMPARATOR.compare(this.relatedPersonIds, other.relatedPersonIds);
        //noinspection RedundantIfStatement
        if (compareRelatedPersonIds != 0) {
            return compareRelatedPersonIds;
        }
        return 0;
    }

    public int compareToWithInvertedPriority(Relationship other) {
        // min distance -> is lower priority
        int compareDistance = compareDistance(other);
        if (compareDistance != 0) {
            return compareDistance;
        }
        // min generation difference -> is higher priority
        int thisGeneration = Math.abs(this.distanceToAncestorRootPerson - this.distanceToAncestorThisPerson);
        int otherGeneration = Math.abs(other.distanceToAncestorRootPerson - other.distanceToAncestorThisPerson);
        int compareGeneration = Integer.compare(thisGeneration, otherGeneration);
        if (compareGeneration != 0) {
            return Math.negateExact(compareGeneration);
        }
        // max distance to ancestor root person -> is higher priority
        int compareDistanceToAncestor = Integer.compare(this.distanceToAncestorRootPerson, other.distanceToAncestorRootPerson);
        if (compareDistanceToAncestor != 0) {
            return compareDistanceToAncestor;
        }
        //
        int compareTreeSides = TREE_SIDE_TYPE_COLLECTION_COMPARATOR
                .reversed()
                .compare(this.treeSides, other.treeSides);
        if (compareTreeSides != 0) {
            return compareTreeSides;
        }
        // not is-half -> is higher priority
        int compareIsHalf = Boolean.compare(this.isHalf, other.isHalf);
        if (compareIsHalf != 0) {
            return Math.negateExact(compareIsHalf);
        }
        // not in-law -> is higher priority
        int compareIsInLaw = Boolean.compare(this.isInLaw, other.isInLaw);
        if (compareIsInLaw != 0) {
            return Math.negateExact(compareIsInLaw);
        }
        int compareRelatedPersonIds = STRING_COLLECTION_COMPARATOR
                .reversed()
                .compare(this.relatedPersonIds, other.relatedPersonIds);
        //noinspection RedundantIfStatement
        if (compareRelatedPersonIds != 0) {
            return compareRelatedPersonIds;
        }
        return 0;
    }

    public Relationship withTreeSides(@Nullable Set<TreeSideType> treeSides) {
        if (Objects.equals(this.treeSides, treeSides)) {
            return this;
        }
        return Relationship.of(
                person,
                distanceToAncestorRootPerson,
                distanceToAncestorThisPerson,
                isInLaw,
                isHalf,
                treeSides,
                relatedPersonIds);
    }

    private static final Comparator<Collection<String>> STRING_COLLECTION_COMPARATOR = Comparator.nullsFirst(new CollectionComparator<>());
    private static final Comparator<Collection<TreeSideType>> TREE_SIDE_TYPE_COLLECTION_COMPARATOR = Comparator.nullsFirst(new CollectionComparator<>());

}
