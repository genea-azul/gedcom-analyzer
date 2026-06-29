package com.geneaazul.gedcomanalyzer.service.familytree;

import tools.jackson.databind.ObjectMapper;
import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FamilyTree;
import com.geneaazul.gedcomanalyzer.model.FamilyTreeType;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.dto.FamilyTreeGraphDto;
import com.geneaazul.gedcomanalyzer.model.dto.FamilyTreeGraphDto.FamilyNodeDto;
import com.geneaazul.gedcomanalyzer.model.dto.FamilyTreeGraphDto.PersonNodeDto;
import com.geneaazul.gedcomanalyzer.service.GedcomParsingService;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphJsonFamilyTreeService implements FamilyTreeService {

    private final GedcomHolder gedcomHolder;
    private final FamilyTreeHelper familyTreeHelper;
    private final GedcomAnalyzerProperties properties;
    private final ObjectMapper objectMapper;
    private final RelationshipMapper relationshipMapper;

    public Path getJsonFile(
            EnrichedPerson person,
            String familyTreeFileIdPrefix,
            String familyTreeFileSuffix) {

        return properties
                .getTempDir()
                .resolve("family-trees")
                .resolve(familyTreeFileIdPrefix + "_" + person.getUuid() + familyTreeFileSuffix + ".json");
    }

    @Override
    public boolean isMissingFamilyTree(
            EnrichedPerson person,
            String familyTreeFileIdPrefix,
            String familyTreeFileSuffix) {

        return Files.notExists(getJsonFile(person, familyTreeFileIdPrefix, familyTreeFileSuffix));
    }

    @Override
    public void generateFamilyTree(
            EnrichedPerson person,
            String familyTreeFileIdPrefix,
            String familyTreeFileSuffix,
            boolean obfuscateLiving,
            boolean onlySecondaryDescription,
            List<List<Relationship>> relationshipsWithNotInLawPriority) {

        log.info("Generating graph JSON family tree [ personId={}, personUuid={} ]", person.getId(), person.getUuid());
        long startTime = System.currentTimeMillis();

        Path jsonFilePath = getJsonFile(person, familyTreeFileIdPrefix, familyTreeFileSuffix);

        generate(jsonFilePath, obfuscateLiving, relationshipsWithNotInLawPriority);

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Completed graph JSON family tree [ personId={}, personUuid={}, ms={} ]", person.getId(), person.getUuid(), totalTime);
    }

    @Override
    public Optional<FamilyTree> getFamilyTree(
            UUID personUuid,
            boolean obfuscateLiving,
            boolean onlySecondaryDescription,
            boolean forceRewrite) {

        EnrichedGedcom gedcom = gedcomHolder.getGedcom();
        EnrichedPerson person = gedcom.getPersonByUuid(personUuid);
        if (person == null) {
            log.warn("Person not found [ personUuid={}, modifiedDateTime={} ]", personUuid, gedcom.getModifiedDateTime());
            return Optional.empty();
        }

        String familyTreeFileIdPrefix = familyTreeHelper.getFamilyTreeFileId(person);
        String familyTreeFileSuffix = obfuscateLiving ? "" : "_visible";

        Path jsonFilePath = getJsonFile(person, familyTreeFileIdPrefix, familyTreeFileSuffix);

        if (forceRewrite || Files.notExists(jsonFilePath)) {
            List<List<Relationship>> relationshipsWithNotInLawPriority = familyTreeHelper
                    .getRelationshipsWithNotInLawPriority(person);

            generateFamilyTree(
                    person,
                    familyTreeFileIdPrefix,
                    familyTreeFileSuffix,
                    obfuscateLiving,
                    onlySecondaryDescription,
                    relationshipsWithNotInLawPriority);
        }

        return Optional.of(new FamilyTree(
                FamilyTreeType.GRAPH_JSON,
                person,
                "genea_azul_arbol_" + familyTreeFileIdPrefix + ".json",
                jsonFilePath,
                MediaType.APPLICATION_JSON,
                properties.getLocale()));
    }

    private void generate(
            Path jsonFilePath,
            boolean obfuscateLiving,
            List<List<Relationship>> peopleInTree) {

        int totalPersons = peopleInTree.size();

        List<List<Relationship>> limitedPeopleInTree = peopleInTree
                .stream()
                .limit(properties.getMaxGraphJsonNodesToExport())
                .toList();

        try {
            Files.createDirectories(jsonFilePath.getParent());
            FamilyTreeGraphDto graph = buildGraph(limitedPeopleInTree, totalPersons, obfuscateLiving);
            objectMapper.writeValue(jsonFilePath.toFile(), graph);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private FamilyTreeGraphDto buildGraph(List<List<Relationship>> peopleInTree, int totalPersons, boolean obfuscateLiving) {
        List<Relationship> firstRelationships = peopleInTree
                .stream()
                .map(List::getFirst)
                .toList();

        Integer focalPersonId = firstRelationships.isEmpty() ? null : firstRelationships.getFirst().person().getId();
        boolean truncated = totalPersons > peopleInTree.size();

        Map<Integer, String> relationshipByPersonId = firstRelationships
                .stream()
                .collect(Collectors.toMap(
                        r -> r.person().getId(),
                        r -> relationshipMapper.formatInSpanish(
                                        relationshipMapper.toRelationshipDto(r, false), false)
                                .relationshipDesc()));

        List<EnrichedPerson> people = firstRelationships
                .stream()
                .map(Relationship::person)
                .toList();

        Set<Integer> personIdsInTree = people
                .stream()
                .map(EnrichedPerson::getId)
                .collect(Collectors.toUnmodifiableSet());

        List<PersonNodeDto> personNodes = firstRelationships
                .stream()
                .map(r -> GedcomParsingService.toPersonNodeDto(
                        r.person(),
                        relationshipByPersonId.get(r.person().getId()),
                        r.getGeneration()))
                .toList();

        List<FamilyNodeDto> familyNodes = new ArrayList<>();
        AtomicInteger familyCounter = new AtomicInteger(0);

        people.forEach(person -> person
                .getSpousesWithChildren()
                .forEach(swc -> {
                    EnrichedPerson spouse = swc.getSpouse().orElse(null);

                    // Each couple-family is owned by the person with the lower ID to avoid duplicates.
                    // Single-parent families (no spouse) are always owned by the person.
                    if (spouse != null && personIdsInTree.contains(spouse.getId())
                            && person.getId() > spouse.getId()) {
                        return;
                    }

                    List<Integer> husbandIds = new ArrayList<>();
                    List<Integer> wifeIds = new ArrayList<>();
                    addSpouseToFamily(person, husbandIds, wifeIds);
                    if (spouse != null && personIdsInTree.contains(spouse.getId())) {
                        addSpouseToFamily(spouse, husbandIds, wifeIds);
                    }

                    List<Integer> childIds = swc.getChildren()
                            .stream()
                            .map(EnrichedPerson::getId)
                            .filter(personIdsInTree::contains)
                            .toList();

                    if (husbandIds.size() + wifeIds.size() == 1 && childIds.isEmpty()) {
                        return;
                    }

                    familyNodes.add(FamilyNodeDto.builder()
                            .id("F" + familyCounter.incrementAndGet())
                            .husbandIds(husbandIds)
                            .wifeIds(wifeIds)
                            .childIds(childIds)
                            .build());
                }));

        return new FamilyTreeGraphDto(focalPersonId, truncated, totalPersons, personNodes, familyNodes);
    }

    private static void addSpouseToFamily(EnrichedPerson person, List<Integer> husbandIds, List<Integer> wifeIds) {
        switch (person.getSex()) {
            case M -> husbandIds.add(person.getId());
            case F -> wifeIds.add(person.getId());
            default -> husbandIds.add(person.getId());
        }
    }

}
