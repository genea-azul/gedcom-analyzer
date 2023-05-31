package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.utils.SortedSetComparator;

import org.springframework.data.domain.Sort;

import java.util.SortedSet;

import jakarta.annotation.Nullable;

public record Relationship(
        int distanceToAncestor1,
        int distanceToAncestor2,
        boolean isInLaw,
        boolean isHalf,
        @Nullable SortedSet<String> relatedPersonIds) implements Comparable<Relationship> {

    public static Relationship of(
            int ascending,
            int descending,
            boolean isInLaw,
            boolean isHalf,
            @Nullable SortedSet<String> relatedPersonIds) {
        return new Relationship(
                ascending,
                descending,
                isInLaw,
                isHalf,
                relatedPersonIds);
    }

    public static Relationship empty() {
        return new Relationship(0, 0, false, false, null);
    }

    public int getDistance() {
        return distanceToAncestor1 + distanceToAncestor2 + (isInLaw ? 1 : 0);
    }

    public int compareDistance(Relationship other) {
        int distance1 = this.getDistance();
        int distance2 = other.getDistance();
        return Integer.compare(distance1, distance2);
    }

    public Relationship increase(Sort.Direction direction, boolean isSetHalf, @Nullable SortedSet<String> relatedPersonIds) {
        if (isInLaw || isHalf && direction == Sort.Direction.ASC || isHalf && isSetHalf) {
            throw new UnsupportedOperationException();
        }
        if (direction == Sort.Direction.ASC) {
            return Relationship.of(distanceToAncestor1 + 1, distanceToAncestor2, false, false, relatedPersonIds);
        }
        if (direction == Sort.Direction.DESC) {
            return Relationship.of(distanceToAncestor1, distanceToAncestor2 + 1, false, isHalf || isSetHalf, relatedPersonIds);
        }
        return Relationship.of(distanceToAncestor1, distanceToAncestor2, true, isHalf, relatedPersonIds);
    }

    public boolean isInLawOf(Relationship other) {
        return this.distanceToAncestor1 == other.distanceToAncestor1
                && this.distanceToAncestor2 == other.distanceToAncestor2
                && this.isInLaw != other.isInLaw
                && this.isHalf == other.isHalf;
    }

    @Override
    public int compareTo(Relationship other) {
        int compareDistance = compareDistance(other);
        if (compareDistance != 0) {
            return compareDistance;
        }
        int compareIsInLaw = Boolean.compare(this.isInLaw, other.isInLaw);
        if (compareIsInLaw != 0) {
            return compareIsInLaw;
        }
        int compareIsHalf = Boolean.compare(this.isHalf, other.isHalf);
        if (compareIsHalf != 0) {
            return compareIsHalf;
        }
        return SORTED_SET_COMPARATOR.compare(this.relatedPersonIds, other.relatedPersonIds);
    }

    private static final SortedSetComparator<String> SORTED_SET_COMPARATOR = new SortedSetComparator<>();

}
