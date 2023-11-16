package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.Relationship;

import java.util.List;

public interface FamilyTreeService {

    boolean isMissingFamilyTree(
            EnrichedPerson person,
            String familyTreeFileIdPrefix,
            String familyTreeFileSuffix,
            boolean obfuscateLiving);

    void generateFamilyTree(
            EnrichedPerson person,
            String familyTreeFileIdPrefix,
            String familyTreeFileSuffix,
            boolean obfuscateLiving,
            List<List<Relationship>> relationshipsWithNotInLawPriority);

}
