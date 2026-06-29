package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.Relationships;
import com.geneaazul.gedcomanalyzer.model.Surname;
import com.geneaazul.gedcomanalyzer.service.PersonService;

import org.springframework.stereotype.Service;

import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FamilyTreeHelper {

    private final PersonService personService;

    public String getFamilyTreeFileId(EnrichedPerson person) {
        return Stream.of(
                        person
                                .getGivenName()
                                .map(GivenName::simplified),
                        person
                                .getSurname()
                                .map(Surname::simplified))
                .flatMap(Optional::stream)
                .reduce((n1, n2) -> n1 + "_" + n2)
                .map(name -> name.replaceAll(" ", "_"))
                .orElse("genea-azul");
    }

    public List<List<Relationship>> getRelationshipsWithNotInLawPriority(EnrichedPerson person) {
        return getRelationshipsWithNotInLawPriority(person, false);
    }

    /**
     * Returns the relationships list for the given person, optionally including ancestors of the
     * person's spouses. When {@code includeSpouseAncestors} is {@code true}, an ancestor-only
     * traversal ({@code onlyAscDirection=true}) is run from each spouse and the results are merged
     * into the main list tagged with {@code isInLaw=true, isSpouseFamily=true}. People already
     * present in the main list (blood relatives of the root person) are never overridden.
     */
    public List<List<Relationship>> getRelationshipsWithNotInLawPriority(
            EnrichedPerson person,
            boolean includeSpouseAncestors) {

        List<Relationships> relationshipsList = personService.setTransientProperties(person, false);

        if (includeSpouseAncestors && !person.getSpouses().isEmpty()) {
            Set<Integer> knownIds = relationshipsList.stream()
                    .map(Relationships::getPersonId)
                    .collect(Collectors.toCollection(HashSet::new));

            List<Relationships> spouseAncestorRels = person.getSpouses()
                    .stream()
                    .flatMap(spouse -> personService.getPeopleInTree(spouse, true, true, true)
                            .stream()
                            .filter(rel -> rel.findFirst().getDistance() > 0)
                            // Set.add returns false if the id was already present; this deduplicates
                            // across multiple spouses and against the main blood-relative list
                            .filter(rel -> knownIds.add(rel.getPersonId()))
                            // Note: if two spouses share a common ancestor (possible in endogamous
                            // families), the ancestor is added via the first spouse only — the second
                            // spouse's entry is blocked by knownIds. This means the second spouse's
                            // treeSides contribution is silently dropped. The practical impact is
                            // cosmetic (a tree-side label may be missing), not structural. A future
                            // improvement could merge treeSides from all spouses rather than taking
                            // first-wins.
                            .map(rel -> Relationships.from(asInLaw(rel.findFirst(), spouse))))
                    .toList();

            if (!spouseAncestorRels.isEmpty()) {
                List<Relationships> combined = new ArrayList<>(relationshipsList);
                combined.addAll(spouseAncestorRels);
                relationshipsList = combined;
            }
        }

        MutableInt orderKey = new MutableInt(1);

        return relationshipsList
                .stream()
                // Make sure each relationship group has 1 or 2 elements (usually an in-law and a not-in-law relationship)
                .peek(relationships -> {
                    if (relationships.isEmpty() || relationships.size() > 2) {
                        throw new UnsupportedOperationException("Something is wrong");
                    }
                })
                // Order internal elements of each relationship group: first not-in-law, then in-law
                .map(relationships -> {
                    // When using CLOSEST_KEEPING_CLOSER_IN_LAW_WHEN_EXISTS_ANY_NOT_IN_LAW strategy,
                    //   the only case of getting an in-law relationship (size 2) is when it has lower distance than the not-in-law
                    if (relationships.size() == 2 && relationships.findFirst().isInLaw()) {
                        return List.of(relationships.findLast(), relationships.findFirst());
                    }
                    return List.copyOf(relationships.getOrderedRelationships());
                })
                .sorted(Comparator.comparing(List::getFirst))
                .peek(relationships -> relationships.getFirst().person().setOrderKey(orderKey.getAndIncrement()))
                .toList();
    }

    /**
     * Converts a relationship obtained from an ancestor-only traversal rooted at {@code spouse} into
     * an in-law / spouse-family relationship for the root person.
     * <p>
     * {@code relatedPersonIds} is explicitly set to {@code [spouse.getId()]} (overriding whatever
     * value the traversal produced) because the mapper reads {@code relatedPersonIds[0]} to look up
     * the linking spouse — it needs the spouse's ID to determine display labels and separation
     * status, not the ancestor's own related-person chain.
     */
    private static Relationship asInLaw(Relationship r, EnrichedPerson spouse) {
        return new Relationship(
                r.person(),
                r.distanceToAncestorRootPerson(),
                r.distanceToAncestorThisPerson(),
                true,
                true,
                r.isHalf(),
                r.adoptionTypeAsc(),
                r.adoptionTypeDesc(),
                r.treeSides(),
                List.of(spouse.getId()));
    }

}
