package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FamilyTreeType;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.Relationships;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDto;
import com.geneaazul.gedcomanalyzer.service.PersonService;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.task.FamilyTreeTask;
import com.geneaazul.gedcomanalyzer.task.FamilyTreeTaskParams;

import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FamilyTreeManager {

    private final GedcomHolder gedcomHolder;
    private final FamilyTreeHelper familyTreeHelper;
    private final PersonService personService;
    private final ExecutorService singleThreadExecutorService;
    private final Map<FamilyTreeType, FamilyTreeService> familyTreeServiceByType;

    public void queueFamilyTreeGeneration(
            List<PersonDto> people,
            boolean obfuscateLiving,
            boolean forceRewrite,
            List<FamilyTreeType> types) {

        if (people.isEmpty()) {
            return;
        }

        List<UUID> peopleUuids = people
                .stream()
                .map(PersonDto::getUuid)
                .toList();

        FamilyTreeTaskParams taskParams = new FamilyTreeTaskParams(peopleUuids, obfuscateLiving, forceRewrite, types);
        FamilyTreeTask task = new FamilyTreeTask(taskParams, this);
        singleThreadExecutorService.submit(task);
    }

    public void generateFamilyTrees(
            List<UUID> personUuids,
            boolean obfuscateLiving,
            boolean forceRewrite,
            List<FamilyTreeType> types) {

        personUuids
                .forEach(personUuid -> {
                    EnrichedGedcom gedcom = gedcomHolder.getGedcom();
                    EnrichedPerson person = gedcom.getPersonByUuid(personUuid);
                    if (person == null) {
                        return;
                    }

                    String familyTreeFileIdPrefix = familyTreeHelper.getFamilyTreeFileId(person);
                    String familyTreeFileSuffix = obfuscateLiving ? "" : "_visible";

                    List<FamilyTreeService> familyTreeServices = types
                            .stream()
                            .map(this::getFamilyTreeServiceByType)
                            .filter(familyTreeService -> forceRewrite || familyTreeService.isMissingFamilyTree(
                                    person,
                                    familyTreeFileIdPrefix,
                                    familyTreeFileSuffix,
                                    obfuscateLiving))
                            .toList();

                    if (familyTreeServices.isEmpty()) {
                        return;
                    }

                    List<List<Relationship>> relationshipsWithNotInLawPriority = getRelationshipsWithNotInLawPriority(person);

                    familyTreeServices
                            .forEach(familyTreeService -> familyTreeService
                                    .generateFamilyTree(
                                            person,
                                            familyTreeFileIdPrefix,
                                            familyTreeFileSuffix,
                                            obfuscateLiving,
                                            relationshipsWithNotInLawPriority));
                });
    }

    public FamilyTreeService getFamilyTreeServiceByType(FamilyTreeType type) {
        return familyTreeServiceByType.get(type);
    }

    @VisibleForTesting
    protected List<List<Relationship>> getRelationshipsWithNotInLawPriority(EnrichedPerson person) {
        List<Relationships> relationshipsList = personService.setTransientProperties(person, false);

        MutableInt orderKey = new MutableInt(1);

        return relationshipsList
                .stream()
                // Make sure each relationship group has 1 or 2 elements (usually an in-law and a not-in-law relationship)
                .peek(relationships -> {
                    if (relationships.size() == 0 || relationships.size() > 2) {
                        throw new UnsupportedOperationException("Something is wrong");
                    }
                })
                // Order internal elements of each relationship group: first not-in-law, then in-law
                .map(relationships -> {
                    if (relationships.size() == 2 && relationships.findFirst().isInLaw()) {
                        return List.of(relationships.findLast(), relationships.findFirst());
                    }
                    return List.copyOf(relationships.getOrderedRelationships());
                })
                .sorted(Comparator.comparing(List::getFirst))
                .peek(relationships -> relationships.getFirst().person().setOrderKey(orderKey.getAndIncrement()))
                .toList();
    }

}
