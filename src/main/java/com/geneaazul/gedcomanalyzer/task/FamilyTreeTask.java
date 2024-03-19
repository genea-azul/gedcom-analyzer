package com.geneaazul.gedcomanalyzer.task;

import com.geneaazul.gedcomanalyzer.service.familytree.FamilyTreeManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class FamilyTreeTask implements Runnable {

    private final FamilyTreeTaskParams taskParams;
    private final FamilyTreeManager familyTreeManager;

    @Override
    public void run() {
        try {
            log.info("Executing task [ persons={}, obfuscateLiving={}, onlySecondaryDescription={}, forceRewrite={}, types={} ]",
                    taskParams.personUuids(),
                    taskParams.obfuscateLiving(),
                    taskParams.onlySecondaryDescription(),
                    taskParams.forceRewrite(),
                    taskParams.types());

            familyTreeManager.generateFamilyTrees(
                    taskParams.personUuids(),
                    taskParams.obfuscateLiving(),
                    taskParams.onlySecondaryDescription(),
                    taskParams.forceRewrite(),
                    taskParams.types());

            log.info("Task completed [ persons={}, obfuscateLiving={}, onlySecondaryDescription={}, forceRewrite={}, types={} ]",
                    taskParams.personUuids(),
                    taskParams.obfuscateLiving(),
                    taskParams.onlySecondaryDescription(),
                    taskParams.forceRewrite(),
                    taskParams.types());
        } catch (Throwable t) {
            log.error("Error while executing task [ persons={}, obfuscateLiving={}, onlySecondaryDescription={}, forceRewrite={}, types={} ]",
                    taskParams.personUuids(),
                    taskParams.obfuscateLiving(),
                    taskParams.onlySecondaryDescription(),
                    taskParams.forceRewrite(),
                    taskParams.types(),
                    t);
        }
    }

}
