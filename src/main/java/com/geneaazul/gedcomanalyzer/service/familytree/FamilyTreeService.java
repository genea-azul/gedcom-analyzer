package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.FamilyTree;
import com.geneaazul.gedcomanalyzer.model.Relationship;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamilyTreeService {

    boolean isMissingFamilyTree(
            EnrichedPerson person,
            String familyTreeFileIdPrefix,
            String familyTreeFileSuffix);

    void generateFamilyTree(
            EnrichedPerson person,
            String familyTreeFileIdPrefix,
            String familyTreeFileSuffix,
            boolean obfuscateLiving,
            boolean onlySecondaryDescription,
            List<List<Relationship>> relationshipsWithNotInLawPriority);

    Optional<FamilyTree> getFamilyTree(
            UUID personUuid,
            boolean obfuscateLiving,
            boolean onlySecondaryDescription,
            boolean forceRewrite);

}
