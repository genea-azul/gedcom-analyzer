package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.model.dto.AdoptionType;
import com.geneaazul.gedcomanalyzer.model.dto.TreeSideType;
import com.geneaazul.gedcomanalyzer.utils.CollectionComparator;

import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.annotation.Nullable;

import lombok.NonNull;

public record Relationship(
        EnrichedPerson person,
        // The direct distance between the root person of the tree and the common ancestor shared with this relationship
        int distanceToAncestorRootPerson,
        // The direct distance between this relationship and the common ancestor shared with the root person of the tree
        int distanceToAncestorThisPerson,
        boolean isInLaw,
        // True when this person was introduced via the spouse's ancestry traversal (not via the root person's own blood/in-law tree)
        boolean isSpouseFamily,
        boolean isHalf,
        @Nullable AdoptionType adoptionTypeAsc,
        @Nullable AdoptionType adoptionTypeDesc,
        @Nullable Set<TreeSideType> treeSides,
        @Nullable List<Integer> relatedPersonIds) implements Comparable<Relationship> {

    public static Relationship empty(EnrichedPerson person) {
        return new Relationship(
                person,
                0,
                0,
                false,
                false,
                false,
                null,
                null,
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

    /**
     * Returns a new Relationship stepped one hop in {@code direction} toward {@code person}.
     * Throws when the current traversal state makes it impossible to produce a valid next relationship:
     * <ul>
     *   <li>{@code isInLaw} — the traversal must stop after a SAME (spouse) link; stepping further
     *       from an already-in-law position is never valid in the main traversal.</li>
     *   <li>{@code isHalf && ASC/ONLY_ASC} — a half marker means the path already descended through
     *       a step-parent branch; ascending again would collapse that half boundary incorrectly.</li>
     *   <li>{@code isHalf && isSetHalf} — a relationship cannot be "half" in two independent directions.</li>
     *   <li>{@code newAdoptionType != null && SAME} — adoption types do not apply to spouse links.</li>
     * </ul>
     */
    public Relationship increaseWithPerson(
            @NonNull EnrichedPerson person,
            @NonNull TreeTraversalDirection direction,
            boolean isSetHalf,
            @Nullable AdoptionType newAdoptionType,
            @NonNull Set<TreeSideType> treeSides,
            @NonNull List<Integer> relatedPersonIds) {

        if (isInLaw
                || isHalf && (direction == TreeTraversalDirection.ASC || direction == TreeTraversalDirection.ONLY_ASC)
                || isHalf && isSetHalf
                || newAdoptionType != null && direction == TreeTraversalDirection.SAME) {
            throw new UnsupportedOperationException();
        }

        return switch (direction) {
            case ASC, ONLY_ASC -> new Relationship(
                    person,
                    distanceToAncestorRootPerson + 1,
                    distanceToAncestorThisPerson,
                    false,
                    false,
                    false,
                    newAdoptionType != null ? newAdoptionType : adoptionTypeAsc,
                    adoptionTypeDesc,
                    treeSides,
                    relatedPersonIds);
            case DESC -> new Relationship(
                    person,
                    distanceToAncestorRootPerson,
                    distanceToAncestorThisPerson + 1,
                    false,
                    false,
                    isHalf || isSetHalf,
                    adoptionTypeAsc,
                    newAdoptionType != null ? newAdoptionType : adoptionTypeDesc,
                    treeSides,
                    relatedPersonIds);
            case SAME -> new Relationship(
                    person,
                    distanceToAncestorRootPerson,
                    distanceToAncestorThisPerson,
                    true,
                    false,
                    isHalf,
                    adoptionTypeAsc,
                    adoptionTypeDesc,
                    treeSides,
                    relatedPersonIds);
        };
    }

    /**
     * Returns true when {@code this} and {@code other} are the same genealogical position (same
     * distances) for the same person, and one is in-law while the other is not. Used by
     * {@code Relationships} to detect that a blood relationship and an in-law relationship coexist
     * for the same person, which triggers the "keep closer in-law when a not-in-law exists" dedup
     * strategy.
     */
    public boolean isInLawOf(Relationship other) {
        Assert.isTrue(this.person.getId().equals(other.person().getId()), "Person ID must equal");
        return this.distanceToAncestorRootPerson == other.distanceToAncestorRootPerson
                && this.distanceToAncestorThisPerson == other.distanceToAncestorThisPerson
                && this.isInLaw != other.isInLaw
                && this.isHalf == other.isHalf
                && this.adoptionTypeAsc == other.adoptionTypeAsc
                && this.adoptionTypeDesc == other.adoptionTypeDesc;
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
                && isSpouseFamily == that.isSpouseFamily
                && isHalf == that.isHalf
                && adoptionTypeAsc == that.adoptionTypeAsc
                && adoptionTypeDesc == that.adoptionTypeDesc
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
                isSpouseFamily,
                isHalf,
                adoptionTypeAsc,
                adoptionTypeDesc,
                treeSides,
                relatedPersonIds);
    }

    @Override
    public int compareTo(@NonNull Relationship other) {
        return compareTo(other, false);
    }

    /**
     * Same ordering as {@link #compareTo} except the tie-breaking criteria (everything after total
     * distance) are inverted. Used by {@code RelationshipUtils.getMaxDistantRelationship} to select
     * the most complex/distant relationship when two candidates share the same total distance: the
     * one that would normally sort last (e.g., in-law, half, adoptive) is preferred instead.
     */
    public int compareToWithInvertedPriority(@NonNull Relationship other) {
        return compareTo(other, true);
    }

    /**
     * Convention used throughout this method: "lower priority" means a lower compareTo return value,
     * which causes this relationship to sort first in the backing TreeSet and be returned by
     * {@code Relationships.findFirst()} — i.e., it is the *preferred* relationship for display.
     * Put differently, a "lower-priority" relationship is the one that wins ties.
     */
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
        // not in-law -> is lower priority
        int compareIsInLaw = Boolean.compare(this.isInLaw, other.isInLaw);
        if (compareIsInLaw != 0) {
            return invert(compareIsInLaw, invertedPriority);
        }
        // not spouse-family -> is lower priority (spouse-family has lowest display priority among in-laws)
        int compareIsSpouseFamily = Boolean.compare(this.isSpouseFamily, other.isSpouseFamily);
        if (compareIsSpouseFamily != 0) {
            return invert(compareIsSpouseFamily, invertedPriority);
        }
        // not adoptive -> is lower priority
        int compareAdoptionAsc = ADOPTION_TYPE_COMPARATOR.compare(this.adoptionTypeAsc, other.adoptionTypeAsc);
        if (compareAdoptionAsc != 0) {
            return invert(compareAdoptionAsc, invertedPriority);
        }
        // not adoptive -> is lower priority
        int compareAdoptionDesc = ADOPTION_TYPE_COMPARATOR.compare(this.adoptionTypeDesc, other.adoptionTypeDesc);
        if (compareAdoptionDesc != 0) {
            return invert(compareAdoptionDesc, invertedPriority);
        }
        // not is-half -> is lower priority
        int compareIsHalf = Boolean.compare(this.isHalf, other.isHalf);
        if (compareIsHalf != 0) {
            return invert(compareIsHalf, invertedPriority);
        }
        //
        int compareTreeSides = TREE_SIDE_TYPE_COLLECTION_COMPARATOR.compare(this.treeSides, other.treeSides);
        if (compareTreeSides != 0) {
            return invert(compareTreeSides, invertedPriority);
        }
        //
        int compareRelatedPersonIds = INTEGER_COLLECTION_COMPARATOR.compare(this.relatedPersonIds, other.relatedPersonIds);
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
        return new Relationship(
                person,
                distanceToAncestorRootPerson,
                distanceToAncestorThisPerson,
                isInLaw,
                isSpouseFamily,
                isHalf,
                adoptionTypeAsc,
                adoptionTypeDesc,
                treeSides,
                relatedPersonIds);
    }

    private static final Comparator<AdoptionType> ADOPTION_TYPE_COMPARATOR = Comparator.nullsFirst(Comparator.naturalOrder());
    private static final Comparator<Collection<Integer>> INTEGER_COLLECTION_COMPARATOR = Comparator.nullsFirst(new CollectionComparator<>());
    private static final Comparator<Collection<TreeSideType>> TREE_SIDE_TYPE_COLLECTION_COMPARATOR = Comparator.nullsFirst(new CollectionComparator<>(true));

}
