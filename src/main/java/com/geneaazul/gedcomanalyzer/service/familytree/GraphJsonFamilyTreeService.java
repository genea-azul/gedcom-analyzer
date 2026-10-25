package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.config.GedcomAnalyzerProperties;
import com.geneaazul.gedcomanalyzer.mapper.RelationshipMapper;
import com.geneaazul.gedcomanalyzer.model.Aka;
import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FamilyTree;
import com.geneaazul.gedcomanalyzer.model.FamilyTreeType;
import com.geneaazul.gedcomanalyzer.model.Place;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.dto.FamilyTreeGraphDto;
import com.geneaazul.gedcomanalyzer.model.dto.FamilyTreeGraphDto.FamilyNodeDto;
import com.geneaazul.gedcomanalyzer.model.dto.FamilyTreeGraphDto.PersonNodeDto;
import com.geneaazul.gedcomanalyzer.service.photo.ProfilePictureCache;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

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

import jakarta.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GraphJsonFamilyTreeService extends AbstractFamilyTreeService {

    private final ObjectMapper objectMapper;
    private final RelationshipMapper relationshipMapper;
    private final ProfilePictureCache profilePictureCache;

    public GraphJsonFamilyTreeService(
            GedcomHolder gedcomHolder,
            FamilyTreeHelper familyTreeHelper,
            GedcomAnalyzerProperties properties,
            ObjectMapper objectMapper,
            RelationshipMapper relationshipMapper,
            ProfilePictureCache profilePictureCache) {
        super(gedcomHolder, familyTreeHelper, properties, FamilyTreeType.GRAPH_JSON, "json", MediaType.APPLICATION_JSON);
        this.objectMapper = objectMapper;
        this.relationshipMapper = relationshipMapper;
        this.profilePictureCache = profilePictureCache;
    }

    @Override
    protected void export(
            Path exportFilePath,
            EnrichedPerson person,
            boolean obfuscateLiving,
            boolean onlySecondaryDescription,
            List<List<Relationship>> relationshipsWithNotInLawPriority) {
        generate(exportFilePath, obfuscateLiving, relationshipsWithNotInLawPriority);
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
                .map(r -> toPersonNodeDto(
                        r.person(),
                        obfuscateLiving,
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

    private PersonNodeDto toPersonNodeDto(
            EnrichedPerson ep,
            boolean obfuscateLiving,
            @Nullable String relationship,
            @Nullable Integer generation) {
        boolean obfuscate = obfuscateLiving && ep.isAlive();

        Integer yearOfBirth = null;
        Boolean circaBirth = null;
        if (!obfuscate && ep.getDateOfBirth().isPresent()) {
            Date dob = ep.getDateOfBirth().get();
            yearOfBirth = dob.getYear().getValue();
            circaBirth = dob.getOperator() == Date.Operator.ABT || dob.getOperator() == Date.Operator.EST;
        }

        Integer yearOfDeath = null;
        Boolean circaDeath = null;
        if (ep.getDateOfDeath().isPresent()) {
            Date dod = ep.getDateOfDeath().get();
            yearOfDeath = dod.getYear().getValue();
            circaDeath = dod.getOperator() == Date.Operator.ABT || dod.getOperator() == Date.Operator.EST;
        }

        return PersonNodeDto.builder()
                .id(ep.getId())
                .displayName(PersonUtils.obfuscateName(ep, obfuscate))
                .sex(ep.getSex())
                .aka(obfuscate ? null : ep.getAka().map(Aka::value).orElse(null))
                .profilePicture(obfuscate ? null : profilePictureCache.publicUrl(ep).orElse(null))
                .yearOfBirth(yearOfBirth)
                .circaBirth(circaBirth)
                .yearOfDeath(yearOfDeath)
                .circaDeath(circaDeath)
                .placeOfBirth(obfuscate ? null : ep.getPlaceOfBirth().map(Place::name).orElse(null))
                .isAlive(ep.isAlive())
                .generation(generation)
                .relationship(relationship)
                .build();
    }

}
