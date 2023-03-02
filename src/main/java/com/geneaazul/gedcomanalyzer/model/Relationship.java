package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.utils.SortedSetComparator;

import org.springframework.data.domain.Sort;

import java.util.SortedSet;

import jakarta.annotation.Nullable;

public record Relationship(
        int distanceToAncestor1,
        int distanceToAncestor2,
        boolean isSpouse,
        boolean isHalf,
        @Nullable SortedSet<String> relatedPersonIds) implements Comparable<Relationship> {

    public static Relationship of(
            int ascending,
            int descending,
            boolean isSpouse,
            boolean isHalf,
            @Nullable SortedSet<String> relatedPersonIds) {
        return new Relationship(
                ascending,
                descending,
                isSpouse,
                isHalf,
                relatedPersonIds);
    }

    public static Relationship empty() {
        return new Relationship(0, 0, false, false, null);
    }

    public int getDistance() {
        return distanceToAncestor1 + distanceToAncestor2 + (isSpouse ? 1 : 0);
    }

    public int compareDistance(Relationship other) {
        int distance1 = this.getDistance();
        int distance2 = other.getDistance();
        return Integer.compare(distance1, distance2);
    }

    public Relationship increase(Sort.Direction direction, boolean isSetHalf, @Nullable SortedSet<String> relatedPersonIds) {
        if (isSpouse || isHalf && direction == Sort.Direction.ASC || isHalf && isSetHalf) {
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

    public boolean isSpouseOf(Relationship other) {
        return this.distanceToAncestor1 == other.distanceToAncestor1
                && this.distanceToAncestor2 == other.distanceToAncestor2
                && this.isSpouse != other.isSpouse
                && this.isHalf == other.isHalf;
    }

    @Override
    public int compareTo(Relationship other) {
        int compareDistance = compareDistance(other);
        if (compareDistance != 0) {
            return compareDistance;
        }
        int compareIsSpouse = Boolean.compare(this.isSpouse, other.isSpouse);
        if (compareIsSpouse != 0) {
            return compareIsSpouse;
        }
        int compareIsHalf = Boolean.compare(this.isHalf, other.isHalf);
        if (compareIsHalf != 0) {
            return compareIsHalf;
        }
        return SORTED_SET_COMPARATOR.compare(this.relatedPersonIds, other.relatedPersonIds);
    }

    private static final SortedSetComparator<String> SORTED_SET_COMPARATOR = new SortedSetComparator<>();

}
