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
            log.info("Executing task [ persons={}, obfuscateLiving={}, types={} ]",
                    taskParams.personUuids(),
                    taskParams.obfuscateLiving(),
                    taskParams.types());

            familyTreeManager.generateFamilyTrees(
                    taskParams.personUuids(),
                    taskParams.obfuscateLiving(),
                    taskParams.types());

            log.info("Task completed [ persons={}, obfuscateLiving={}, types={} ]",
                    taskParams.personUuids(),
                    taskParams.obfuscateLiving(),
                    taskParams.types());
        } catch (Throwable t) {
            log.error("Error while executing task [ persons={}, obfuscateLiving={}, types={} ]",
                    taskParams.personUuids(),
                    taskParams.obfuscateLiving(),
                    taskParams.types(),
                    t);
        }
    }

}
