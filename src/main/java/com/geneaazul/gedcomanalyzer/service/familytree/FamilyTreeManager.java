package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FamilyTreeType;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDto;
import com.geneaazul.gedcomanalyzer.service.storage.GedcomHolder;
import com.geneaazul.gedcomanalyzer.task.FamilyTreeTask;
import com.geneaazul.gedcomanalyzer.task.FamilyTreeTaskParams;

import org.springframework.stereotype.Service;

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
    private final ExecutorService executorService;
    private final Map<FamilyTreeType, FamilyTreeService> familyTreeServiceByType;

    public void queueFamilyTreeGeneration(
            List<PersonDto> people,
            boolean obfuscateLiving,
            boolean onlySecondaryDescription,
            boolean forceRewrite,
            List<FamilyTreeType> types) {

        if (people.isEmpty()) {
            return;
        }

        List<UUID> peopleUuids = people
                .stream()
                .map(PersonDto::getUuid)
                .toList();

        FamilyTreeTaskParams taskParams = new FamilyTreeTaskParams(peopleUuids, obfuscateLiving, onlySecondaryDescription, forceRewrite, types);
        FamilyTreeTask task = new FamilyTreeTask(taskParams, this);
        executorService.submit(task);
    }

    public void generateFamilyTrees(
            List<UUID> personUuids,
            boolean obfuscateLiving,
            boolean onlySecondaryDescription,
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
                            .map(familyTreeServiceByType::get)
                            .filter(familyTreeService -> forceRewrite || familyTreeService.isMissingFamilyTree(
                                    person,
                                    familyTreeFileIdPrefix,
                                    familyTreeFileSuffix))
                            .toList();

                    if (familyTreeServices.isEmpty()) {
                        return;
                    }

                    List<List<Relationship>> relationshipsWithNotInLawPriority = familyTreeHelper
                            .getRelationshipsWithNotInLawPriority(person);

                    familyTreeServices
                            .forEach(familyTreeService -> familyTreeService
                                    .generateFamilyTree(
                                            person,
                                            familyTreeFileIdPrefix,
                                            familyTreeFileSuffix,
                                            obfuscateLiving,
                                            onlySecondaryDescription,
                                            relationshipsWithNotInLawPriority));
                });
    }

}
