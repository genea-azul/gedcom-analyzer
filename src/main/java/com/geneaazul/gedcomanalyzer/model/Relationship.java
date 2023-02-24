package com.geneaazul.gedcomanalyzer.model;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;

import jakarta.annotation.Nullable;

public record Relationship(
        int distanceToAncestor1,
        int distanceToAncestor2,
        boolean isSpouse,
        boolean isHalf,
        @Nullable String relatedPersonId) implements Comparable<Relationship> {

    public static Relationship of(int ascending, int descending, boolean isSpouse, boolean isHalf, @Nullable String relatedPersonId) {
        return new Relationship(ascending, descending, isSpouse, isHalf, relatedPersonId);
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

    public Relationship increase(Sort.Direction direction, boolean isSetHalf, @Nullable String relatedPersonId) {
        if (isSpouse || isHalf && direction == Sort.Direction.ASC || isHalf && isSetHalf) {
            throw new UnsupportedOperationException();
        }
        if (direction == Sort.Direction.ASC) {
            return Relationship.of(distanceToAncestor1 + 1, distanceToAncestor2, false, false, relatedPersonId);
        }
        if (direction == Sort.Direction.DESC) {
            return Relationship.of(distanceToAncestor1, distanceToAncestor2 + 1, false, isHalf || isSetHalf, relatedPersonId);
        }
        return Relationship.of(distanceToAncestor1, distanceToAncestor2, true, isHalf, relatedPersonId);
    }

    @Override
    public int compareTo(Relationship other) {
        int compareDistance = compareDistance(other);
        if (compareDistance != 0) {
            return compareDistance;
        }
        if (!this.isSpouse && other.isSpouse) {
            return -1;
        }
        if (this.isSpouse && !other.isSpouse) {
            return 1;
        }
        if (!this.isHalf && other.isHalf) {
            return -1;
        }
        if (this.isHalf && !other.isHalf) {
            return 1;
        }
        return StringUtils.compare(this.relatedPersonId, other.relatedPersonId);
    }

}
