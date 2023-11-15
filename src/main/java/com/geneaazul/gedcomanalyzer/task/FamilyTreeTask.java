package com.geneaazul.gedcomanalyzer.task;

import com.geneaazul.gedcomanalyzer.service.FamilyTreeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class FamilyTreeTask implements Runnable {

    private final FamilyTreeTaskParams taskParams;
    private final FamilyTreeService familyTreeService;

    @Override
    public void run() {
        try {
            log.info("Executing task");
            familyTreeService.generateFamilyTree(taskParams.personUuids(), taskParams.obfuscateLiving());
            log.info("Task completed");
        } catch (Throwable t) {
            log.error("Error while executing task [ personUuids={} ]", taskParams.personUuids(), t);
        }
    }

}
