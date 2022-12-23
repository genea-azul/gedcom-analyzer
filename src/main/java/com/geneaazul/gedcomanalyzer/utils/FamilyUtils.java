package com.geneaazul.gedcomanalyzer.utils;

import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;

import java.util.List;
import java.util.Set;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FamilyUtils {

    /**
     * Tags taken from EventFact class.
     */
    public static final Set<String> DIVORCE_TAGS = Set.of("DIV", "DIVF", "DIVORCE", "_DIV");
    public static final Set<String> EVENT_TAGS = Set.of("EVEN", "EVENT");

    public static List<EventFact> getCustomEventFacts(Family family) {
        return family.getEventsFacts()
                .stream()
                .filter(eventFact -> EVENT_TAGS.contains(eventFact.getTag()))
                .toList();
    }

}
