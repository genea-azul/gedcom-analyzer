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
import com.geneaazul.gedcomanalyzer.model.TreeTraversalDirection;
import com.geneaazul.gedcomanalyzer.model.dto.AdoptionType;
import com.geneaazul.gedcomanalyzer.model.dto.ReferenceType;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.model.dto.TreeSideType;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.RelationshipUtils;
import com.geneaazul.gedcomanalyzer.utils.SearchUtils;
import com.geneaazul.gedcomanalyzer.utils.SetUtils;

import org.apache.commons.lang3.mutable.MutableInt;
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
import lombok.NonNull;
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
        String suffix = obfuscateLiving ? "" : "_visible";

        Path path = properties
                .getTempDir()
                .resolve("family-trees")
                .resolve(fileId + "_" + personUuid + suffix + ".pdf");

        if (!Files.exists(path)) {
            List<Relationships> relationshipsList = setTransientProperties(person, false);

            MutableInt index = new MutableInt(1);
            List<FormattedRelationship> peopleInTree = relationshipsList
                    .stream()
                    .sorted()
                    .map(relationships -> relationships
                            .getOrderedRelationships()
                            .stream()
                            .map(relationship -> relationshipMapper.toRelationshipDto(
                                    relationship,
                                    obfuscateLiving
                                            // don't obfuscate root person
                                            && !relationship.person().getId().equals(person.getId())
                                            && (person.isAlive() && relationship.getDistance() <= MAX_DISTANCE_TO_OBFUSCATE
                                                    || relationship.person().isAlive())))
                            .map(relationship -> relationshipMapper.formatInSpanish(relationship, index.getAndIncrement(), true))
                            .toList())
                    .map(formattedRelationships -> {
                        if (formattedRelationships.size() > 2 || formattedRelationships.isEmpty()) {
                            throw new UnsupportedOperationException("Something is wrong");
                        }
                        if (formattedRelationships.size() == 1) {
                            return formattedRelationships.get(0);
                        }
                        FormattedRelationship first = formattedRelationships.get(0);
                        FormattedRelationship second = formattedRelationships.get(1);
                        return new FormattedRelationship(
                                first.index(),
                                first.personName(),
                                first.personSex(),
                                first.personIsAlive(),
                                first.personYearOfBirth(),
                                first.personCountryOfBirth(),
                                first.adoption(),
                                first.treeSide(),
                                first.relationshipDesc() + " / " + second.relationshipDesc());
                    })
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

        person.setPersonsCountInTree(relationships.size());
        person.setSurnamesCountInTree(surnamesCount);
        person.setAncestryCountries(ancestryCountries);
        person.setAncestryGenerations(ancestryGenerations);
        person.setMaxDistantRelationship(maxDistantRelationship);

        return relationships;
    }

    public List<Relationships> getPeopleInTree(EnrichedPerson person, boolean excludeRootPerson, boolean onlyAscDirection) {
        Map<String, Relationships> visitedPersons = new LinkedHashMap<>();
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
            @Nullable String previousPersonId,
            @NonNull Map<String, Relationships> visitedPersons,
            @NonNull TreeTraversalDirection direction,
            @NonNull Relationships.VisitedRelationshipTraversalStrategy visitedRelationshipTraversalStrategy,
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
            @NonNull Map<String, Relationships> visitedPersons,
            @NonNull Relationship toVisitRelationship,
            @Nullable String previousPersonId,
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
            @Nullable String previousPersonId) {

        if (direction == TreeTraversalDirection.SAME) {
            return Stream.of();
        }

        List<String> singleRelatedPersonIds = List.of(person.getId());

        List<Optional<String>> previousPersonParents =
                (direction == TreeTraversalDirection.ASC
                        && previousPersonId != null
                        && !person.getSpousesWithChildren().isEmpty())
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
                                                            TreeTraversalDirection.DESC,
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

    private record RelativeAndDirection(
            @NonNull EnrichedPerson person,
            @NonNull TreeTraversalDirection direction,
            boolean isHalf,
            @Nullable AdoptionType adoptionType,
            @NonNull Set<TreeSideType> treeSides,
            @NonNull List<String> relatedPersonIds) {
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
