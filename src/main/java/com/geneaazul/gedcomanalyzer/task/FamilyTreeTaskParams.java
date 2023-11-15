package com.geneaazul.gedcomanalyzer.task;

import java.util.List;
import java.util.UUID;

public record FamilyTreeTaskParams(List<UUID> personUuids, boolean obfuscateLiving) {

}
