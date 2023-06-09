package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.model.AncestryGenerations;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FamilyTree;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PersonService {

    private static final int MAX_LISTED_PEOPLE_IN_TREE = 1000;
    private static final int MAX_DISTANCE_TO_OBFUSCATE = 3;

    private final GedcomAnalyzerProperties properties;
    private final GedcomHolder gedcomHolder;
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
                .resolve(fileId + "_" + personUuid + ".txt");

        if (!Files.exists(path)) {
            List<Relationship> relationships = getPeopleInTree(person, false);

            MutableInt index = new MutableInt();
            List<String> peopleInTree = relationships
                    .stream()
                    .sorted()
                    .limit(MAX_LISTED_PEOPLE_IN_TREE)
                    .map(relationship -> relationshipMapper.toRelationshipDto(
                            relationship,
                            obfuscateLiving
                                    // don't obfuscate root person
                                    && !relationship.person().getId().equals(person.getId())
                                    && (person.isAlive() && relationship.getDistance() <= MAX_DISTANCE_TO_OBFUSCATE
                                            || relationship.person().isAlive())))
                    .map(relationship -> relationshipMapper.formatInSpanish(relationship, index.getAndIncrement()))
                    .toList();

            Stream<String> lines = Stream.concat(
                    Stream.of(
                            "Genea Azul - 2023",
                            "http://geneaazul.com.ar/",
                            "IG: @genea.azul", "", "",
                            "Leyenda:",
                            " • ♀  mujer", " • ♂  varón", " • ✝  difunto/a",
                            " • ←  rama paterna                  →  rama materna                  ↔  rama paterna y materna",
                            " • ↙  rama descendente y paterna    ↘  rama descendente y materna    ⇊  rama descendente, paterna y materna",
                            " • ↓  rama descendente",
                            " • Los datos de personas vivas o cercanas a la persona principal serán ocultados: <nombre privado>", "", "",
                            "Árbol genealógico de " + person.getDisplayName(), "",
                            " • Personas: " + relationships.size(), ""),
                    peopleInTree
                            .stream());

            if (relationships.size() > peopleInTree.size()) {
                lines = Stream.concat(
                        lines,
                        Stream.of("", "[ Debido a una limitación técnica se listaron un máximo de " + MAX_LISTED_PEOPLE_IN_TREE
                                + " personas, para obtener el árbol completo ponete en contacto con nosotros. ]"));
            }

            lines = Stream.concat(
                    lines,
                    Stream.of("", "Generado el " + ZonedDateTime
                            .now(properties.getZoneId())
                            .format(DateTimeFormatter
                                    .ofLocalizedDateTime(FormatStyle.FULL)
                                    .localizedBy(new Locale("es", "AR")))));

            try {
                Files.createDirectories(path.getParent());
                Files.write(path, lines.toList(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        return Optional.of(new FamilyTree(
                person,
                "genea_azul_arbol_" + fileId + ".txt",
                path,
                MediaType.TEXT_PLAIN,
                new Locale("es", "AR")));
    }

    public List<String> getAncestryCountries(EnrichedPerson person) {
        if (person.getAncestryCountries() == null) {
            Set<String> visitedPersons = new HashSet<>();
            List<String> ancestryCountries = person
                    .getParents()
                    .stream()
                    .map(parent -> getAncestryCountries(parent, visitedPersons, 0))
                    .flatMap(Set::stream)
                    .distinct()
                    .sorted()
                    .toList();
            person.setAncestryCountries(ancestryCountries);
        }

        return person.getAncestryCountries();
    }

    private static Set<String> getAncestryCountries(
            EnrichedPerson person,
            Set<String> visitedPersons,
            int level) {

        // Corner case: parents are cousins -> skip visiting a person twice
        if (visitedPersons.contains(person.getId())) {
            return Set.of();
        }

        visitedPersons.add(person.getId());

        if (level == 20) {
            // If max level or recursion is reached, stop the search
            return person.getCountryOfBirth()
                    .map(Set::of)
                    .orElseGet(Set::of);
        }

        Set<String> ancestryCountries = person
                .getParents()
                .stream()
                .map(parent -> getAncestryCountries(
                        parent,
                        visitedPersons,
                        level + 1))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        // Add person's country to the set of ancestry countries
        person.getCountryOfBirth()
                .ifPresent(ancestryCountries::add);

        return ancestryCountries;
    }

    public AncestryGenerations getAncestryGenerations(EnrichedPerson person) {
        if (person.getAncestryGenerations() == null) {
            int ascendingGenerations = getAncestryGenerations(person, new HashSet<>(), EnrichedPerson::getParents, 0, 0);
            int descendingGenerations = getAncestryGenerations(person, new HashSet<>(), EnrichedPerson::getChildren, 0, 0);
            person.setAncestryGenerations(AncestryGenerations.of(ascendingGenerations, descendingGenerations));
        }

        return person.getAncestryGenerations();
    }

    private static int getAncestryGenerations(
            EnrichedPerson person,
            Set<String> visitedPersons,
            Function<EnrichedPerson, List<EnrichedPerson>> relativesResolver,
            int level,
            int maxLevel) {

        // Corner case: parents are cousins -> skip visiting
        // Corner case: parent is cousin of spouse's parent -> visit higher distance
        if (visitedPersons.contains(person.getId()) && level <= maxLevel) {
            return level;
        }

        visitedPersons.add(person.getId());

        if (level == 20) {
            // If max level or recursion is reached, stop the search
            return level;
        }

        MutableInt maxLevelHolder = new MutableInt(maxLevel);

        return relativesResolver
                .apply(person)
                .stream()
                .map(parent -> {
                    int generations = getAncestryGenerations(
                            parent,
                            visitedPersons,
                            relativesResolver,
                            level + 1,
                            maxLevelHolder.getValue());

                    int newMaxLevel = Math.max(maxLevelHolder.getValue(), generations);
                    maxLevelHolder.setValue(newMaxLevel);

                    return generations;
                })
                .reduce(Integer::max)
                .orElse(level);
    }

    public Integer getNumberOfPeopleInTree(EnrichedPerson person) {
        if (person.getNumberOfPeopleInTree() == null) {
            setNumberOfPeopleInTreeAndMaxDistantRelationship(person);
        }

        return person.getNumberOfPeopleInTree();
    }

    public Optional<Relationship> getMaxDistantRelationship(EnrichedPerson person) {
        if (person.getMaxDistantRelationship() == null) {
            setNumberOfPeopleInTreeAndMaxDistantRelationship(person);
        }

        return person.getMaxDistantRelationship();
    }

    private void setNumberOfPeopleInTreeAndMaxDistantRelationship(EnrichedPerson person) {
        List<Relationship> peopleInTree = getPeopleInTree(person, true);
        Optional<Relationship> maxDistantRelationship = peopleInTree
                .stream()
                .reduce((r1, r2) -> r1.compareToWithInvertedPriority(r2) >= 0 ? r1 : r2);

        person.setNumberOfPeopleInTree(peopleInTree.size() + 1);
        person.setMaxDistantRelationship(maxDistantRelationship);
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
