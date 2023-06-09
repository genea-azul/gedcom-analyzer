package com.geneaazul.gedcomanalyzer.utils;

import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.EnrichedSpouseWithChildren;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FamilyUtils {

    /**
     * Tags taken from EventFact class.
     */
    public static final Set<String> MARRIAGE_TAGS = Set.of("MARR", "MARRIAGE");
    public static final Set<String> CIVIL_MARRIAGE_TAGS = Set.of("MARL");
    public static final Set<String> OTHER_MARRIAGE_TAGS = Set.of("MARB", "MARS");
    public static final Set<String> DIVORCE_TAGS = Set.of("DIV", "DIVF", "DIVORCE", "_DIV");
    public static final Set<String> EVENT_TAGS = Set.of("EVEN", "EVENT");
    public static final Set<String> PARTNERS_EVENT_TYPES = Set.of("Partners", "PARTNERS", "MYHERITAGE:REL_PARTNERS", "MYHERITAGE:REL_UNKNOWN");
    public static final Set<String> SEPARATION_EVENT_TYPES = Set.of("Separation", "SEPARATION");
    public static final Set<String> ADOPTED_CHILD_RELATIONSHIP_TYPES = Set.of("Adopted", "ADOPTED");
    public static final Set<String> FOSTER_CHILD_RELATIONSHIP_TYPES = Set.of("Foster", "FOSTER");

    public static List<EventFact> getCustomEventFacts(Family family) {
        return family.getEventsFacts()
                .stream()
                .filter(eventFact -> EVENT_TAGS.contains(eventFact.getTag()))
                .toList();
    }

    public static boolean isSeparated(Family family) {
        return family.getEventsFacts()
                .stream()
                .anyMatch(eventFact
                        -> DIVORCE_TAGS.contains(eventFact.getTag()) && eventFact.getValue().equals("Y")
                        || EVENT_TAGS.contains(eventFact.getTag()) && SEPARATION_EVENT_TYPES.contains(eventFact.getType()));
    }

    public static Optional<String> getDateOfPartners(Family family) {
        return family.getEventsFacts()
                .stream()
                .filter(eventFact -> MARRIAGE_TAGS.contains(eventFact.getTag()))
                .findFirst()
                .or(() -> family.getEventsFacts()
                        .stream()
                        .filter(eventFact -> CIVIL_MARRIAGE_TAGS.contains(eventFact.getTag()))
                        .findFirst())
                .or(() -> family.getEventsFacts()
                        .stream()
                        .filter(eventFact -> OTHER_MARRIAGE_TAGS.contains(eventFact.getTag()))
                        .findFirst())
                .or(() -> family.getEventsFacts()
                        .stream()
                        .filter(eventFact -> EVENT_TAGS.contains(eventFact.getTag()) && PARTNERS_EVENT_TYPES.contains(eventFact.getType()))
                        .findFirst())
                .map(EventFact::getDate)
                .map(StringUtils::trimToNull);
    }

    public static Optional<String> getDateOfSeparation(Family family) {
        return family.getEventsFacts()
                .stream()
                .filter(eventFact -> DIVORCE_TAGS.contains(eventFact.getTag()) && eventFact.getValue().equals("Y"))
                .findFirst()
                .or(() -> family.getEventsFacts()
                        .stream()
                        .filter(eventFact -> EVENT_TAGS.contains(eventFact.getTag()) && SEPARATION_EVENT_TYPES.contains(eventFact.getType()))
                        .findFirst())
                .map(EventFact::getDate)
                .map(StringUtils::trimToNull);
    }

    public static Optional<String> getPlaceOfPartners(Family family) {
        return family.getEventsFacts()
                .stream()
                .filter(eventFact -> MARRIAGE_TAGS.contains(eventFact.getTag()))
                .findFirst()
                .or(() -> family.getEventsFacts()
                        .stream()
                        .filter(eventFact -> CIVIL_MARRIAGE_TAGS.contains(eventFact.getTag()))
                        .findFirst())
                .or(() -> family.getEventsFacts()
                        .stream()
                        .filter(eventFact -> OTHER_MARRIAGE_TAGS.contains(eventFact.getTag()))
                        .findFirst())
                .or(() -> family.getEventsFacts()
                        .stream()
                        .filter(eventFact -> EVENT_TAGS.contains(eventFact.getTag()) && PARTNERS_EVENT_TYPES.contains(eventFact.getType()))
                        .findFirst())
                .map(EventFact::getPlace)
                .map(StringUtils::trimToNull);
    }

    public static Optional<String> getPlaceOfSeparation(Family family) {
        return family.getEventsFacts()
                .stream()
                .filter(eventFact -> DIVORCE_TAGS.contains(eventFact.getTag()) && eventFact.getValue().equals("Y"))
                .findFirst()
                .or(() -> family.getEventsFacts()
                        .stream()
                        .filter(eventFact -> EVENT_TAGS.contains(eventFact.getTag()) && SEPARATION_EVENT_TYPES.contains(eventFact.getType()))
                        .findFirst())
                .map(EventFact::getPlace)
                .map(StringUtils::trimToNull);
    }

    public static final Comparator<EnrichedSpouseWithChildren> DATES_COMPARATOR = Comparator
            .nullsLast((m1, m2) -> {
                Date dop1 = m1.getDateOfPartners().orElse(null);
                Date dop2 = m2.getDateOfPartners().orElse(null);
                int cmp = ObjectUtils.compare(dop1, dop2, true);
                if (cmp != 0) {
                    return cmp;
                }
                Date dos1 = m1.getDateOfSeparation().orElse(null);
                Date dos2 = m2.getDateOfSeparation().orElse(null);
                cmp = ObjectUtils.compare(dos1, dos2, true);
                if (cmp != 0) {
                    return cmp;
                }
                String spouseId1 = m1.getSpouse().map(EnrichedPerson::getId).orElse("");
                String spouseId2 = m2.getSpouse().map(EnrichedPerson::getId).orElse("");
                return spouseId1.compareTo(spouseId2);
            });

}
