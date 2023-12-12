package com.geneaazul.gedcomanalyzer.task;

import com.geneaazul.gedcomanalyzer.model.FamilyTreeType;

import java.util.List;
import java.util.UUID;

public record FamilyTreeTaskParams(
        List<UUID> personUuids,
        boolean obfuscateLiving,
        boolean forceRewrite,
        List<FamilyTreeType> types) {

}
