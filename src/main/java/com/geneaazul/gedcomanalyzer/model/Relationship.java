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

    public boolean isDirect() {
        return distanceToAncestorRootPerson == 0 || distanceToAncestorThisPerson == 0;
    }

    public int getDistance() {
        return distanceToAncestorRootPerson + distanceToAncestorThisPerson;
    }

    public int getGeneration() {
        return distanceToAncestorRootPerson - distanceToAncestorThisPerson;
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
        return compareTo(other, false);
    }

    public int compareToWithInvertedPriority(@NotNull Relationship other) {
        return compareTo(other, true);
    }

    private int compareTo(Relationship other, boolean invertedPriority) {
        // min distance -> is lower priority
        int compareDistance = compareDistance(other);
        if (compareDistance != 0) {
            return compareDistance;
        }
        // min generation difference -> is lower priority
        int compareGeneration = Integer.compare(Math.abs(this.getGeneration()), Math.abs(other.getGeneration()));
        if (compareGeneration != 0) {
            return invert(compareGeneration, invertedPriority);
        }
        // max distance to ancestor root person -> is lower priority
        int compareDistanceToAncestor = -Integer.compare(this.distanceToAncestorRootPerson, other.distanceToAncestorRootPerson);
        if (compareDistanceToAncestor != 0) {
            return invert(compareDistanceToAncestor, invertedPriority);
        }
        //
        int compareTreeSides = TREE_SIDE_TYPE_COLLECTION_COMPARATOR.compare(this.treeSides, other.treeSides);
        if (compareTreeSides != 0) {
            return invert(compareTreeSides, invertedPriority);
        }
        // not is-half -> is lower priority
        int compareIsHalf = Boolean.compare(this.isHalf, other.isHalf);
        if (compareIsHalf != 0) {
            return invert(compareIsHalf, invertedPriority);
        }
        // not in-law -> is lower priority
        int compareIsInLaw = Boolean.compare(this.isInLaw, other.isInLaw);
        if (compareIsInLaw != 0) {
            return invert(compareIsInLaw, invertedPriority);
        }
        int compareRelatedPersonIds = STRING_COLLECTION_COMPARATOR.compare(this.relatedPersonIds, other.relatedPersonIds);
        if (compareRelatedPersonIds != 0) {
            return invert(compareRelatedPersonIds, invertedPriority);
        }
        return 0;
    }

    private int invert(int value, boolean invert) {
        return invert ? -value : value;
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
