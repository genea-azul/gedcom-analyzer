package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.model.dto.AlivePersonFilter;

import jakarta.annotation.Nullable;

import lombok.Builder;

/**
 * Options of {@link com.geneaazul.gedcomanalyzer.service.GedcomParsingService#format}.
 * {@link #builder()} starts from "keep everyone": no trimming, unlimited depth, in-laws included.
 *
 * @param alivePersonFilter           how to handle living persons
 * @param displayOnlyBasic            copy only essential event tags (birth, death, marriage…) and basic name
 *                                    fields; notes on events are preserved either way
 * @param directLineageOnly           exclude collateral relatives (siblings, cousins, etc.)
 * @param rootPersonId                if non-null, written as {@code _ROOT} in the output header
 * @param trimTriggerSize             null = never trim; 0 = always trim; N &gt; 0 = trim only when the tree exceeds N people
 * @param maxAscDepth                 max ancestor levels included in the main range
 * @param maxDescDepth                max descendant levels included in the main range
 * @param distantAncestorDescLimit    desc cap for ancestors beyond the main asc range (null = no ancestor overflow)
 * @param includeInLawsAtMaxDescDepth if false, in-law relatives at exactly {@code desc == maxDescDepth} are excluded
 * @param includeInLawsAtMaxAscDepth  if false, in-law relatives at exactly {@code asc == maxAscDepth} are excluded
 * @param maxCollateralDescDepth      null = no restriction on collateral relatives; N &gt;= 0 = exclude all
 *                                    non-direct in-law entries and restrict non-direct blood entries to
 *                                    {@code desc <= N}; no effect when {@code directLineageOnly} is true
 * @param profilePictureUrlPrefix     with {@code displayOnlyBasic}, people shown in full get their photo as an
 *                                    {@code OBJE}/{@code FILE} of {@code prefix + cacheKey + ".jpg"} (the
 *                                    {@code /api/photos/} cache, whose keys survive re-exports); null = no photos
 */
@Builder(toBuilder = true)
public record SubGedcomOptions(
        AlivePersonFilter alivePersonFilter,
        boolean displayOnlyBasic,
        boolean directLineageOnly,
        @Nullable Integer rootPersonId,
        @Nullable Integer trimTriggerSize,
        int maxAscDepth,
        int maxDescDepth,
        @Nullable Integer distantAncestorDescLimit,
        boolean includeInLawsAtMaxDescDepth,
        boolean includeInLawsAtMaxAscDepth,
        @Nullable Integer maxCollateralDescDepth,
        @Nullable String profilePictureUrlPrefix) {

    public SubGedcomOptions {
        if (alivePersonFilter == null) {
            throw new IllegalArgumentException("alivePersonFilter is required");
        }
        if (maxAscDepth < 0) {
            throw new IllegalArgumentException("maxAscDepth must be >= 0: " + maxAscDepth);
        }
        if (maxDescDepth < 0) {
            throw new IllegalArgumentException("maxDescDepth must be >= 0: " + maxDescDepth);
        }
        if (trimTriggerSize != null && trimTriggerSize < 0) {
            throw new IllegalArgumentException("trimTriggerSize must be >= 0 when non-null: " + trimTriggerSize);
        }
        if (distantAncestorDescLimit != null && distantAncestorDescLimit < 0) {
            throw new IllegalArgumentException("distantAncestorDescLimit must be >= 0 when non-null: " + distantAncestorDescLimit);
        }
        if (distantAncestorDescLimit != null && distantAncestorDescLimit >= maxDescDepth) {
            throw new IllegalArgumentException(
                    "distantAncestorDescLimit must be < maxDescDepth when non-null: "
                            + distantAncestorDescLimit + " >= " + maxDescDepth);
        }
        if (distantAncestorDescLimit != null && maxAscDepth == 0) {
            throw new IllegalArgumentException(
                    "distantAncestorDescLimit requires maxAscDepth > 0: with maxAscDepth=0 every ancestor"
                            + " satisfies asc > 0 so clause 2 would apply to all of them, making maxAscDepth meaningless");
        }
        if (maxCollateralDescDepth != null && maxCollateralDescDepth < 0) {
            throw new IllegalArgumentException(
                    "maxCollateralDescDepth must be >= 0 when non-null: " + maxCollateralDescDepth);
        }
    }

    public static SubGedcomOptionsBuilder builder() {
        return new SubGedcomOptionsBuilder()
                .alivePersonFilter(AlivePersonFilter.ALLOW)
                .maxAscDepth(Integer.MAX_VALUE)
                .maxDescDepth(Integer.MAX_VALUE)
                .includeInLawsAtMaxDescDepth(true)
                .includeInLawsAtMaxAscDepth(true);
    }

}
