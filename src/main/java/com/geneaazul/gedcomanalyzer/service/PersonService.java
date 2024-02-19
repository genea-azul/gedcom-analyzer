package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.mapper.ObfuscationType;
import com.geneaazul.gedcomanalyzer.mapper.PersonMapper;
import com.geneaazul.gedcomanalyzer.model.AncestryGenerations;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.EnrichedPersonWithReference;
import com.geneaazul.gedcomanalyzer.model.EnrichedSpouseWithChildren;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.Relationships;
import com.geneaazul.gedcomanalyzer.model.TreeTraversalDirection;
import com.geneaazul.gedcomanalyzer.model.dto.AdoptionType;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDto;
import com.geneaazul.gedcomanalyzer.model.dto.ReferenceType;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.model.dto.TreeSideType;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.RelationshipUtils;
import com.geneaazul.gedcomanalyzer.utils.SetUtils;

import org.springframework.stereotype.Service;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final GedcomHolder gedcomHolder;
    private final PersonMapper personMapper;

    public Optional<PersonDto> getPersonDto(UUID personUuid) {
        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson person = gedcom.getPersonByUuid(personUuid);
        return Optional.ofNullable(person)
                .map(p -> personMapper.toPersonDto(p, ObfuscationType.SKIP_MAIN_PERSON_NAME));
    }

    public List<Relationships> setTransientProperties(EnrichedPerson person, boolean excludeRootPerson) {
        List<Relationships> relationships = getPeopleInTree(person, excludeRootPerson, false);

        List<Relationship> lastRelationships = relationships
                .stream()
                // Getting the last will prioritize the not-in-law relationships
                .map(Relationships::findLast)
                .toList();

        Integer surnamesCount = RelationshipUtils.getSurnamesCount(lastRelationships);
        List<String> ancestryCountries = RelationshipUtils.getAncestryCountries(lastRelationships);
        AncestryGenerations ancestryGenerations = RelationshipUtils.getAncestryGenerations(lastRelationships);
        Optional<Relationship> maxDistantRelationship = RelationshipUtils.getMaxDistantRelationship(lastRelationships);
        List<EnrichedPerson> distinguishedPersons = lastRelationships
                .stream()
                .map(Relationship::person)
                .filter(EnrichedPerson::isDistinguishedPerson)
                .toList();

        person.setPersonsCountInTree(relationships.size());
        person.setSurnamesCountInTree(surnamesCount);
        person.setAncestryCountries(ancestryCountries);
        person.setAncestryGenerations(ancestryGenerations);
        person.setMaxDistantRelationship(maxDistantRelationship);
        person.setDistinguishedPersonsInTree(distinguishedPersons);
        person.setOrderKey(null);

        return relationships;
    }

    public List<Relationships> getPeopleInTree(EnrichedPerson person, boolean excludeRootPerson, boolean onlyAscDirection) {
        Map<Integer, Relationships> visitedPersons = new LinkedHashMap<>(128);
        traversePeopleInTree(
                Relationship.empty(person),
                null,
                visitedPersons,
                onlyAscDirection ? TreeTraversalDirection.ONLY_ASC : TreeTraversalDirection.ASC,
                Relationships.VisitedRelationshipTraversalStrategy.CLOSEST_KEEPING_CLOSER_IN_LAW_WHEN_EXISTS_ANY_NOT_IN_LAW,
                false);

        return visitedPersons
                .values()
                .stream()
                .filter(relationships -> !excludeRootPerson || !relationships.getPersonId().equals(person.getId()))
                // Make sure all relationships have the updated list of tree sides
                .peek(Relationships::propagateTreeSidesToRelationships)
                .toList();
    }

    private static void traversePeopleInTree(
            @NonNull Relationship toVisitRelationship,
            @Nullable Integer previousPersonId,
            @NonNull Map<Integer, Relationships> visitedPersons,
            @NonNull TreeTraversalDirection direction,
            @NonNull Relationships.VisitedRelationshipTraversalStrategy visitedRelationshipTraversalStrategy,
            boolean onlyPropagateTreeSides) {

        EnrichedPerson person = toVisitRelationship.person();

        boolean visited = visitedPersons.containsKey(person.getId());
        if (visited) {
            Relationships visitedRelationships = visitedPersons.get(person.getId());

            // Skip re-visiting root person
            if (visitedRelationships.findFirst().getDistance() == 0) {
                return;
            }

            if (onlyPropagateTreeSides
                    && !SetUtils.containsAll(visitedRelationships.getTreeSides(), toVisitRelationship.treeSides())) {
                mergeTreeSides(visitedPersons, toVisitRelationship, previousPersonId, direction, visitedRelationshipTraversalStrategy);
                return;
            }

            // Idempotency check for visited person
            if (visitedRelationships.contains(toVisitRelationship)) {
                return;
            }

            // Check traversal strategies for visited in-law relationship
            if (toVisitRelationship.isInLaw()
                    && visitedRelationshipTraversalStrategy.getInLawMatching().test(visitedRelationships, toVisitRelationship)) {
                return;
            }

            // Check traversal strategies for visited closest distance relationship
            if (visitedRelationshipTraversalStrategy.isClosestDistance()
                    && switch (visitedRelationshipTraversalStrategy.getType()) {
                        case SKIP_IN_LAW_WHEN_EXISTS_SAME_DIST_NOT_IN_LAW
                                -> (toVisitRelationship.isInLaw() || !visitedRelationships.containsInLawOf(toVisitRelationship))
                                && toVisitRelationship.compareTo(visitedRelationships.findFirst()) >= 0;
                        case SKIP_IN_LAW_WHEN_EXISTS_ANY_DIST_NOT_IN_LAW
                                -> (toVisitRelationship.isInLaw() || visitedRelationships.isContainsNotInLaw())
                                && toVisitRelationship.compareTo(visitedRelationships.findFirst()) >= 0;
                        case KEEP_CLOSER_IN_LAW_WHEN_EXISTS_ANY_NOT_IN_LAW
                                // In this case the comparison for the in-law condition was already performed some lines above
                                -> !toVisitRelationship.isInLaw()
                                && visitedRelationships
                                        .findFirstNotInLaw()
                                        .map(relationship -> toVisitRelationship.compareTo(relationship) >= 0)
                                        .orElse(false);
                    }) {
                mergeTreeSides(visitedPersons, toVisitRelationship, previousPersonId, direction, visitedRelationshipTraversalStrategy);
                return;
            }
        } else if (onlyPropagateTreeSides) {
            throw new UnsupportedOperationException();
        }

        Relationships merged = visitedPersons.merge(
                person.getId(),
                Relationships.from(toVisitRelationship),
                (r1, r2) -> r1.merge(r2, visitedRelationshipTraversalStrategy));

        if (toVisitRelationship.getDistance() == 32) {
            // If max level or recursion is reached, stop the search
            return;
        }

        resolveRelativesToTraverse(
                person,
                direction,
                merged.getTreeSides(),
                previousPersonId)
                .forEach(relativeAndDirection -> traversePeopleInTree(
                        toVisitRelationship.increaseWithPerson(
                                relativeAndDirection.person,
                                relativeAndDirection.direction,
                                relativeAndDirection.isHalf,
                                relativeAndDirection.adoptionType,
                                relativeAndDirection.treeSides,
                                relativeAndDirection.relatedPersonIds),
                        person.getId(),
                        visitedPersons,
                        relativeAndDirection.direction,
                        visitedRelationshipTraversalStrategy,
                        onlyPropagateTreeSides));
    }

    private static void mergeTreeSides(
            @NonNull Map<Integer, Relationships> visitedPersons,
            @NonNull Relationship toVisitRelationship,
            @Nullable Integer previousPersonId,
            @NonNull TreeTraversalDirection direction,
            @NonNull Relationships.VisitedRelationshipTraversalStrategy visitedRelationshipTraversalStrategy) {

        Relationships relationships = visitedPersons.get(toVisitRelationship.person().getId());

        boolean isTreeSideCompatible = toVisitRelationship.isTreeSideCompatible(relationships.getOrderedRelationships());
        boolean isMissingTreeSides = !SetUtils.containsAll(relationships.getTreeSides(), toVisitRelationship.treeSides());

        if (!isTreeSideCompatible || !isMissingTreeSides) {
            return;
        }

        Relationships merged = visitedPersons.merge(
                toVisitRelationship.person().getId(),
                Relationships.from(toVisitRelationship),
                Relationships::mergeTreeSides);

        // Propagate merged tree sides to descendants
        resolveRelativesToTraverse(
                toVisitRelationship.person(),
                direction,
                merged.getTreeSides(),
                previousPersonId)
                .forEach(relativeAndDirection -> traversePeopleInTree(
                        toVisitRelationship.increaseWithPerson(
                                relativeAndDirection.person,
                                relativeAndDirection.direction,
                                relativeAndDirection.isHalf,
                                relativeAndDirection.adoptionType,
                                relativeAndDirection.treeSides,
                                relativeAndDirection.relatedPersonIds),
                        toVisitRelationship.person().getId(),
                        visitedPersons,
                        relativeAndDirection.direction,
                        visitedRelationshipTraversalStrategy,
                        true));
    }

    /**
     * <p>Returns the next relatives to visit based on the current direction.</p>
     * <ul>
     *     <li>Direction ASC: when traversing the tree in ASC direction all relatives are considered
     *         <ul>
     *             <li>Previous visited person: a child</li>
     *             <li>Next directions to visit: ASC (parents), DESC (children), null (spouses)</li>
     *         </ul>
     *     </li>
     *     <li>Direction DESC: when traversing the tree in DESC direction only spouses and children are considered
     *         <ul>
     *             <li>Previous visited person: a parent</li>
     *             <li>Next directions to visit: DESC (children), null (spouses)</li>
     *         </ul>
     *     </li>
     *     <li>Direction null: when traversing the tree in null direction no relatives are considered
     *         <ul>
     *             <li>Previous visited person: a spouse</li>
     *             <li>Next directions to visit: -</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    private static Stream<RelativeAndDirection> resolveRelativesToTraverse(
            @NonNull EnrichedPerson person,
            @NonNull TreeTraversalDirection direction,
            @Nullable Set<TreeSideType> treeSides,
            @Nullable Integer previousPersonId) {

        if (direction == TreeTraversalDirection.SAME) {
            return Stream.of();
        }

        List<Integer> singleRelatedPersonIds = List.of(person.getId());

        // Returns current person (which is parent of previous person)
        //   and optionally its couple (in case it is parent of previous person)
        boolean calculateIsHalf = direction == TreeTraversalDirection.ASC
                && previousPersonId != null
                && person.getSpousesWithChildren().size() > 1;

        Optional<Optional<Integer>> previousPersonParent;
        if (calculateIsHalf) {
            List<Pair<Optional<EnrichedPerson>, EnrichedPersonWithReference>> previousPersonParents = person
                    .getSpousesWithChildren()
                    .stream()
                    .map(spouseWithChildren -> Pair.of(
                            spouseWithChildren.getSpouse(),
                            spouseWithChildren
                                    .getChildrenWithReference()
                                    .stream()
                                    .filter(cwr -> previousPersonId.equals(cwr.person().getId()))
                                    .findAny()
                                    .orElse(null)))
                    .filter(pair -> pair.getRight() != null)
                    .toList();

            /*
             * Possible values:
             *   - 1 element with empty spouse (this person, 1 parent family)
             *   - 1 element with spouse (the biological or adopted parent)
             *   - 2 elements (the biological and adopted parents) -> in this case we will consider only the biological one
             */
            if (previousPersonParents.isEmpty() || previousPersonParents.size() > 2) {
                throw new UnsupportedOperationException();
            }

            previousPersonParent = previousPersonParents
                    .stream()
                    .reduce((swc1, swc2) -> swc1.getRight().referenceType().isEmpty()
                            ? swc1
                            : swc2)
                    .map(Pair::getLeft)
                    .map(spouse -> spouse.map(EnrichedPerson::getId));
        } else {
            previousPersonParent = Optional.empty();
        }

        Stream<RelativeAndDirection> relativesAndDirections = (direction != TreeTraversalDirection.ONLY_ASC)
                ? Stream.concat(
                        person
                                .getSpouses()
                                .stream()
                                .map(spouse -> new RelativeAndDirection(
                                        spouse,
                                        TreeTraversalDirection.SAME,
                                        false,
                                        null,
                                        Optional
                                                .ofNullable(treeSides)
                                                .orElseGet(() -> Set.of(TreeSideType.SPOUSE)),
                                        singleRelatedPersonIds)),
                        person
                                .getSpousesWithChildren()
                                .stream()
                                .flatMap(spouseWithChildren -> {
                                    // when traversing children, both parents will be considered the related persons
                                    List<Integer> relatedPersonIds = spouseWithChildren.getSpouse()
                                            .map(spouse -> Stream
                                                    .of(
                                                            person.getId(),
                                                            spouse.getId())
                                                    .sorted()
                                                    .toList())
                                            .orElse(singleRelatedPersonIds);

                                    // If my child's (previous person when it is ASC direction) parents list
                                    //   is not empty (it includes at least me) and it doesn't include my couple,
                                    //   it means these children are half-siblings of the previous one.
                                    boolean isHalf = previousPersonParent
                                            .map(parent -> !parent.equals(spouseWithChildren
                                                    .getSpouse()
                                                    .map(EnrichedPerson::getId)))
                                            .orElse(false);

                                    return spouseWithChildren
                                            .getChildrenWithReference()
                                            .stream()
                                            .map(childWithReference -> new RelativeAndDirection(
                                                    childWithReference.person(),
                                                    TreeTraversalDirection.DESC,
                                                    isHalf,
                                                    childWithReference
                                                            .referenceType()
                                                            .map(PersonService::resolveAdoptionType)
                                                            .orElse(null),
                                                    Optional
                                                            .ofNullable(treeSides)
                                                            .orElseGet(() -> Set.of(TreeSideType.DESCENDANT)),
                                                    relatedPersonIds));
                                }))
                : Stream.empty();

        if (direction == TreeTraversalDirection.ASC || direction == TreeTraversalDirection.ONLY_ASC) {
            relativesAndDirections = Stream.concat(
                    person
                            .getParentsWithReference()
                            .stream()
                            .map(parentWithReference -> new RelativeAndDirection(
                                    parentWithReference.person(),
                                    direction,
                                    false,
                                    parentWithReference
                                            .referenceType()
                                            .map(PersonService::resolveAdoptionType)
                                            .orElse(null),
                                    Optional
                                            .ofNullable(treeSides)
                                            .orElseGet(() -> resolveParentTreeSideTypes(parentWithReference.person().getSex())),
                                    singleRelatedPersonIds)),
                    relativesAndDirections);
        }

        return relativesAndDirections
                .filter(relative -> !relative.person.getId().equals(previousPersonId));
    }

    public Relationship getRelationshipBetween(EnrichedPerson personA, EnrichedPerson personB) {

        Optional<EnrichedPersonWithReference> parentWithReference = personA
                .getParentsWithReference()
                .stream()
                .filter(pwr -> pwr.person().getId().equals(personB.getId()))
                // Prefer non-adoptive parent
                .min(Comparator.comparingInt(cwr -> cwr.referenceType().map(ReferenceType::ordinal).get()));
        if (parentWithReference.isPresent()) {
            return Relationship
                    .empty(personA)
                    .increaseWithPerson(
                            personB,
                            TreeTraversalDirection.ASC,
                            false,
                            parentWithReference
                                    .get()
                                    .referenceType()
                                    .map(PersonService::resolveAdoptionType)
                                    .orElse(null),
                            Set.of(),
                            List.of(personA.getId()));
        }

        Optional<EnrichedSpouseWithChildren> spouseWithChildren = personA
                .getSpousesWithChildren()
                .stream()
                .filter(swc -> swc.getSpouse().isPresent())
                .filter(swc -> swc.getSpouse().get().getId().equals(personB.getId()))
                .findFirst();
        if (spouseWithChildren.isPresent()) {
            return Relationship
                    .empty(personA)
                    .increaseWithPerson(
                            personB,
                            TreeTraversalDirection.SAME,
                            false,
                            null,
                            Set.of(),
                            List.of(personA.getId()));
        }

        Optional<EnrichedPersonWithReference> childWithReference = personA
                .getSpousesWithChildren()
                .stream()
                .map(EnrichedSpouseWithChildren::getChildrenWithReference)
                .flatMap(List::stream)
                .filter(cwr -> cwr.person().getId().equals(personB.getId()))
                // Prefer non-adoptive child
                .min(Comparator.comparingInt(cwr -> cwr.referenceType().map(ReferenceType::ordinal).get()));
        if (childWithReference.isPresent()) {
            return Relationship
                    .empty(personA)
                    .increaseWithPerson(
                            personB,
                            TreeTraversalDirection.DESC,
                            false,
                            childWithReference
                                    .get()
                                    .referenceType()
                                    .map(PersonService::resolveAdoptionType)
                                    .orElse(null),
                            Set.of(),
                            List.of(personA.getId()));
        }

        return null;
    }

    private record RelativeAndDirection(
            @NonNull EnrichedPerson person,
            @NonNull TreeTraversalDirection direction,
            boolean isHalf,
            @Nullable AdoptionType adoptionType,
            @NonNull Set<TreeSideType> treeSides,
            @NonNull List<Integer> relatedPersonIds) {
    }

    private static Set<TreeSideType> resolveParentTreeSideTypes(SexType parentSex) {
        return switch (parentSex) {
            case F -> Set.of(TreeSideType.MOTHER);
            case M -> Set.of(TreeSideType.FATHER);
            default -> Set.of();
        };
    }

    private static AdoptionType resolveAdoptionType(ReferenceType referenceType) {
        return switch (referenceType) {
            case ADOPTIVE_PARENT, ADOPTED_CHILD -> AdoptionType.ADOPTIVE;
            case FOSTER_PARENT, FOSTER_CHILD -> AdoptionType.FOSTER;
            default -> null;
        };
    }

}
