package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.model.AncestryGenerations;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.EnrichedSpouseWithChildren;
import com.geneaazul.gedcomanalyzer.model.FamilyTree;
import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.Relationships;
import com.geneaazul.gedcomanalyzer.model.Surname;
import com.geneaazul.gedcomanalyzer.model.dto.AdoptionType;
import com.geneaazul.gedcomanalyzer.model.dto.ReferenceType;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.model.dto.TreeSideType;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.RelationshipUtils;
import com.geneaazul.gedcomanalyzer.utils.SearchUtils;
import com.geneaazul.gedcomanalyzer.utils.SetUtils;

import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PersonService {

    private static final int MAX_DISTANCE_TO_OBFUSCATE = 3;

    private final FamilyTreeService familyTreeService;
    private final GedcomHolder gedcomHolder;
    private final GedcomAnalyzerProperties properties;
    private final RelationshipMapper relationshipMapper;

    public Optional<FamilyTree> getFamilyTree(UUID personUuid, boolean obfuscateLiving) {

        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson person = gedcom.getPersonByUuid(personUuid);
        if (person == null) {
            return Optional.empty();
        }

        String fileId = getFamilyTreeFileId(person);

        Path path = properties
                .getTempDir()
                .resolve("family-trees")
                .resolve(fileId + "_" + personUuid + ".pdf");

        if (!Files.exists(path)) {
            List<Relationship> relationships = setTransientProperties(person, false);

            MutableInt index = new MutableInt(1);
            List<FormattedRelationship> peopleInTree = relationships
                    .stream()
                    .sorted()
                    .map(relationship -> relationshipMapper.toRelationshipDto(
                            relationship,
                            obfuscateLiving
                                    // don't obfuscate root person
                                    && !relationship.person().getId().equals(person.getId())
                                    && (person.isAlive() && relationship.getDistance() <= MAX_DISTANCE_TO_OBFUSCATE
                                            || relationship.person().isAlive())))
                    .map(relationship -> relationshipMapper.formatInSpanish(relationship, index.getAndIncrement(), true))
                    .toList();

            try {
                familyTreeService.exportToPDF(path, person, peopleInTree);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        return Optional.of(new FamilyTree(
                person,
                "genea_azul_arbol_" + fileId + ".pdf",
                path,
                MediaType.APPLICATION_PDF,
                new Locale("es", "AR")));
    }

    private String getFamilyTreeFileId(EnrichedPerson person) {
        return Stream.of(
                        person
                                .getGivenName()
                                .map(GivenName::value),
                        person
                                .getSurname()
                                .map(Surname::value))
                .flatMap(Optional::stream)
                .reduce((n1, n2) -> n1 + "_" + n2)
                .map(SearchUtils::simplifyName)
                .map(name -> name.replaceAll(" ", "_"))
                .orElse("genea-azul");
    }

    public List<Relationship> setTransientProperties(EnrichedPerson person, boolean excludeRootPerson) {
        List<Relationship> relationships = getPeopleInTree(person, excludeRootPerson);

        Integer surnamesCount = RelationshipUtils.getSurnamesCount(relationships);
        List<String> ancestryCountries = RelationshipUtils.getAncestryCountries(relationships);
        AncestryGenerations ancestryGenerations = RelationshipUtils.getAncestryGenerations(relationships);
        Optional<Relationship> maxDistantRelationship = RelationshipUtils.getMaxDistantRelationship(relationships);

        person.setPersonsCountInTree(relationships.size());
        person.setSurnamesCountInTree(surnamesCount);
        person.setAncestryCountries(ancestryCountries);
        person.setAncestryGenerations(ancestryGenerations);
        person.setMaxDistantRelationship(maxDistantRelationship);

        return relationships;
    }

    public List<Relationship> getPeopleInTree(EnrichedPerson person, boolean excludeRootPerson) {
        Map<String, Relationships> visitedPersons = new LinkedHashMap<>();
        traversePeopleInTree(
                Relationship.empty(person),
                null,
                visitedPersons,
                Sort.Direction.ASC,
                Relationships.VisitedRelationshipTraversalStrategy.CLOSEST_SKIPPING_IN_LAW_WHEN_EXISTS_ANY_NOT_IN_LAW,
                false);

        return visitedPersons
                .values()
                .stream()
                .filter(relationships -> !excludeRootPerson || !relationships.getPersonId().equals(person.getId()))
                .map(relationships -> {
                    // Set the collected tree sides from all relationships for the current person
                    Relationship relationship = relationships.findFirst();
                    return relationship.withTreeSides(relationships.getTreeSides());
                })
                .toList();
    }

    private static void traversePeopleInTree(
            Relationship toVisitRelationship,
            @Nullable String previousPersonId,
            Map<String, Relationships> visitedPersons,
            @Nullable Sort.Direction direction,
            Relationships.VisitedRelationshipTraversalStrategy visitedRelationshipTraversalStrategy,
            boolean onlyPropagateTreeSides) {

        EnrichedPerson person = toVisitRelationship.person();
        boolean visited = visitedPersons.containsKey(person.getId());
        if (visited) {
            Relationships visitedRelationships = visitedPersons.get(person.getId());

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
            if (toVisitRelationship.isInLaw()) {
                if (visitedRelationshipTraversalStrategy.isSkipInLawWhenSameDistanceNotInLaw() && visitedRelationships.containsInLawOf(toVisitRelationship)) {
                    // The visited in-law relationship appears also as not in-law (with the same distance)
                    return;
                }
                if (visitedRelationshipTraversalStrategy.isSkipInLawWhenAnyDistanceNonInLaw() && visitedRelationships.isContainsNotInLaw()) {
                    // The visited in-law relationship appears also as not in-law (with any distance)
                    return;
                }
            }

            // Check traversal strategies for visited closest distance relationship
            if (visitedRelationshipTraversalStrategy.isClosestDistance()) {
                if (visitedRelationshipTraversalStrategy.isSkipInLawWhenSameDistanceNotInLaw()
                        && (toVisitRelationship.isInLaw() || !visitedRelationships.containsInLawOf(toVisitRelationship))
                        && toVisitRelationship.compareTo(visitedRelationships.getOrderedRelationships().first()) >= 0) {
                    mergeTreeSides(visitedPersons, toVisitRelationship, previousPersonId, direction, visitedRelationshipTraversalStrategy);
                    return;
                }
                if (visitedRelationshipTraversalStrategy.isSkipInLawWhenAnyDistanceNonInLaw()
                        && (toVisitRelationship.isInLaw() || visitedRelationships.isContainsNotInLaw())
                        && toVisitRelationship.compareTo(visitedRelationships.getOrderedRelationships().first()) >= 0) {
                    mergeTreeSides(visitedPersons, toVisitRelationship, previousPersonId, direction, visitedRelationshipTraversalStrategy);
                    return;
                }
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
            Map<String, Relationships> visitedPersons,
            Relationship toVisitRelationship,
            @Nullable String previousPersonId,
            Sort.Direction direction,
            Relationships.VisitedRelationshipTraversalStrategy visitedRelationshipTraversalStrategy) {

        @Nullable Set<TreeSideType> previousTreeSides = visitedPersons
                .get(toVisitRelationship.person().getId())
                .getTreeSides();

        boolean merge = !SetUtils.containsAll(previousTreeSides, toVisitRelationship.treeSides());

        if (merge) {
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
            EnrichedPerson person,
            @Nullable Sort.Direction direction,
            @Nullable Set<TreeSideType> treeSides,
            @Nullable String previousPersonId) {

        if (direction == null) {
            return Stream.of();
        }

        List<String> singleRelatedPersonIds = List.of(person.getId());

        List<Optional<String>> previousPersonParents =
                (direction == Sort.Direction.ASC && previousPersonId != null && !person.getSpousesWithChildren().isEmpty())
                ? person
                        .getSpousesWithChildren()
                        .stream()
                        .filter(spouseWithChildren -> spouseWithChildren
                                .getChildren()
                                .stream()
                                .map(EnrichedPerson::getId)
                                .anyMatch(previousPersonId::equals))
                        .map(EnrichedSpouseWithChildren::getSpouse)
                        .map(spouse -> spouse.map(EnrichedPerson::getId))
                        .toList()
                : null;

        Stream<RelativeAndDirection> relativesAndDirections = Stream.concat(
                person
                        .getSpouses()
                        .stream()
                        .map(spouse -> new RelativeAndDirection(
                                spouse,
                                null,
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
                                    List<String> relatedPersonIds = spouseWithChildren.getSpouse()
                                            .map(spouse -> Stream
                                                    .of(
                                                            person.getId(),
                                                            spouse.getId())
                                                    .sorted()
                                                    .toList())
                                            .orElse(singleRelatedPersonIds);

                                    return spouseWithChildren
                                            .getChildrenWithReference()
                                            .stream()
                                            .map(childWithReference -> new RelativeAndDirection(
                                                    childWithReference.person(),
                                                    Sort.Direction.DESC,
                                                    previousPersonParents != null && !previousPersonParents
                                                            .contains(spouseWithChildren.getSpouse().map(EnrichedPerson::getId)),
                                                    childWithReference
                                                            .referenceType()
                                                            .map(PersonService::resolveAdoptionType)
                                                            .orElse(null),
                                                    Optional
                                                            .ofNullable(treeSides)
                                                            .orElseGet(() -> Set.of(TreeSideType.DESCENDANT)),
                                                    relatedPersonIds));
                                }));

        if (direction == Sort.Direction.ASC) {
            relativesAndDirections = Stream.concat(
                    person
                            .getParentsWithReference()
                            .stream()
                            .map(parentWithReference -> new RelativeAndDirection(
                                    parentWithReference.person(),
                                    Sort.Direction.ASC,
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

    private record RelativeAndDirection(
            EnrichedPerson person,
            @Nullable Sort.Direction direction,
            boolean isHalf,
            AdoptionType adoptionType,
            Set<TreeSideType> treeSides,
            List<String> relatedPersonIds) {
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
