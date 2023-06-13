package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.model.AncestryGenerations;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FamilyTree;
import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.Relationships;
import com.geneaazul.gedcomanalyzer.model.Surname;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.model.dto.TreeSideType;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
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
import java.util.stream.Collectors;
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

        String fileId = Stream.of(
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

    public List<String> getAncestryCountries(EnrichedPerson person) {
        if (person.getAncestryCountries() == null) {
            setTransientProperties(person, true);
        }
        return person.getAncestryCountries();
    }

    public AncestryGenerations getAncestryGenerations(EnrichedPerson person) {
        if (person.getAncestryGenerations() == null) {
            setTransientProperties(person, true);
        }
        return person.getAncestryGenerations();
    }

    public Integer getNumberOfPeopleInTree(EnrichedPerson person) {
        if (person.getNumberOfPeopleInTree() == null) {
            setTransientProperties(person, true);
        }
        return person.getNumberOfPeopleInTree();
    }

    @SuppressWarnings("OptionalAssignedToNull")
    public Optional<Relationship> getMaxDistantRelationship(EnrichedPerson person) {
        if (person.getMaxDistantRelationship() == null) {
            setTransientProperties(person, true);
        }
        return person.getMaxDistantRelationship();
    }

    public List<Relationship> setTransientProperties(EnrichedPerson person, boolean excludeRootPerson) {
        List<Relationship> relationships = getPeopleInTree(person, excludeRootPerson);

        List<String> ancestryCountries = relationships
                .stream()
                .reduce(Set.<String>of(),
                        (s, r) -> (r.isDirect() && r.getGeneration() > 0 && !r.isInLaw())
                                ? r
                                        .person()
                                        .getCountryOfBirth()
                                        .map(country -> SetUtils.add(s, country))
                                        .orElse(s)
                                : s,
                        SetUtils::merge)
                .stream()
                .sorted()
                .toList();

        AncestryGenerations ancestryGenerations = relationships
                .stream()
                .reduce(AncestryGenerations.empty(), AncestryGenerations::mergeRelationship, AncestryGenerations::merge);

        Optional<Relationship> maxDistantRelationship = relationships
                .stream()
                .reduce((r1, r2) -> r1.compareToWithInvertedPriority(r2) >= 0 ? r1 : r2);

        person.setNumberOfPeopleInTree(relationships.size());
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
                        toVisitRelationship.increase(
                                relativeAndDirection.person,
                                relativeAndDirection.direction,
                                isSetHalf(direction, relativeAndDirection.direction, previousPersonId, relativeAndDirection.person),
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
                            toVisitRelationship.increase(
                                    relativeAndDirection.person,
                                    relativeAndDirection.direction,
                                    false,
                                    relativeAndDirection.treeSides,
                                    relativeAndDirection.relatedPersonIds),
                            toVisitRelationship.person().getId(),
                            visitedPersons,
                            relativeAndDirection.direction,
                            visitedRelationshipTraversalStrategy,
                            true));
        }
    }

    private static boolean isSetHalf(
            Sort.Direction currentDirection,
            Sort.Direction nextDirection,
            @Nullable String previousPersonId,
            EnrichedPerson nextPersonToVisit) {

        if (previousPersonId == null || !(currentDirection == Sort.Direction.ASC && nextDirection == Sort.Direction.DESC)) {
            return false;
        }

        EnrichedPerson previousPerson = nextPersonToVisit.getGedcom().getPersonById(previousPersonId);

        // TODO consider biological/adopted parents
        //noinspection DataFlowIssue
        if (previousPerson.getParents().size() != nextPersonToVisit.getParents().size()) {
            return false;
        }

        Set<String> parentIds = previousPerson
                .getParents()
                .stream()
                .map(EnrichedPerson::getId)
                .collect(Collectors.toSet());

        return nextPersonToVisit
                .getParents()
                .stream()
                .map(EnrichedPerson::getId)
                .anyMatch(personId -> !parentIds.contains(personId));
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
    private static Stream<A> resolveRelativesToTraverse(
            EnrichedPerson person,
            @Nullable Sort.Direction direction,
            @Nullable Set<TreeSideType> treeSides,
            @Nullable String previousPersonId) {

        if (direction == null) {
            return Stream.of();
        }

        List<String> singleRelatedPersonIds = List.of(person.getId());

        Stream<A> relativesAndDirections = Stream.concat(
                person
                        .getSpouses()
                        .stream()
                        .map(spouse -> A.of(
                                spouse,
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
                                            .getChildren()
                                            .stream()
                                            .map(child -> A.of(
                                                    child,
                                                    Sort.Direction.DESC,
                                                    Optional
                                                            .ofNullable(treeSides)
                                                            .orElseGet(() -> Set.of(TreeSideType.DESCENDANT)),
                                                    relatedPersonIds));
                                }));

        if (direction == Sort.Direction.ASC) {
            relativesAndDirections = Stream.concat(
                    person
                            .getParents()
                            .stream()
                            .map(parent -> A.of(
                                    parent,
                                    Sort.Direction.ASC,
                                    Optional
                                            .ofNullable(treeSides)
                                            .orElseGet(() -> resolveParentTreeSideTypes(parent.getSex())),
                                    singleRelatedPersonIds)),
                    relativesAndDirections);
        }

        return relativesAndDirections
                .filter(relative -> !relative.person.getId().equals(previousPersonId));
    }

    private record A(
            EnrichedPerson person,
            @Nullable Sort.Direction direction,
            Set<TreeSideType> treeSides,
            List<String> relatedPersonIds) {

        public static A of(
                EnrichedPerson person,
                @Nullable Sort.Direction direction,
                Set<TreeSideType> treeSides,
                List<String> relatedPersonIds) {
            return new A(
                    person,
                    direction,
                    treeSides,
                    relatedPersonIds);
        }
    }

    private static Set<TreeSideType> resolveParentTreeSideTypes(SexType parentSex) {
        switch (parentSex) {
            case F -> { return Set.of(TreeSideType.MOTHER); }
            case M -> { return Set.of(TreeSideType.FATHER); }
            default -> { return Set.of(); }
        }
    }

}
