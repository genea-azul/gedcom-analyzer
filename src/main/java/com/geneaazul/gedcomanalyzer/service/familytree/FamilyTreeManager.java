package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FamilyTreeType;
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
    private final ExecutorService familyTreeExecutorService;
    private final Map<FamilyTreeType, FamilyTreeService> familyTreeServiceByType;

    public void queueFamilyTreeGeneration(
            List<PersonDto> people,
            boolean obfuscateLiving,
            List<FamilyTreeType> types) {

        List<UUID> peopleUuids = people
                .stream()
                .map(PersonDto::getUuid)
                .toList();

        FamilyTreeTaskParams taskParams = new FamilyTreeTaskParams(peopleUuids, obfuscateLiving, types);
        FamilyTreeTask task = new FamilyTreeTask(taskParams, this);
        familyTreeExecutorService.submit(task);
    }

    public void generateFamilyTree(
            List<UUID> personUuids,
            boolean obfuscateLiving,
            List<FamilyTreeType> types) {

        personUuids
                .forEach(personUuid -> {
                    EnrichedGedcom gedcom = gedcomHolder.getGedcom();
                    EnrichedPerson person = gedcom.getPersonByUuid(personUuid);
                    if (person == null) {
                        return;
                    }

                    types
                            .stream()
                            .map(this::getFamilyTreeServiceByType)
                            .forEach(familyTreeService -> familyTreeService
                                    .generateFamilyTree(person, obfuscateLiving));
                });
    }

    public FamilyTreeService getFamilyTreeServiceByType(FamilyTreeType type) {
        return familyTreeServiceByType.get(type);
    }

}
