package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.mapper.PersonMapper;
import com.geneaazul.gedcomanalyzer.model.Date;
import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.PersonComparisonResults;
import com.geneaazul.gedcomanalyzer.model.Reference;
import com.geneaazul.gedcomanalyzer.model.dto.GedcomAnalysisDto;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDto;
import com.geneaazul.gedcomanalyzer.model.dto.PersonDuplicateDto;
import com.geneaazul.gedcomanalyzer.model.dto.ReferenceType;
import com.geneaazul.gedcomanalyzer.utils.DateUtils;
import com.geneaazul.gedcomanalyzer.utils.EnumCollectionUtils;
import com.geneaazul.gedcomanalyzer.utils.FamilyUtils;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folg.gedcom.model.ChildRef;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.ParentFamilyRef;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.SpouseFamilyRef;
import org.folg.gedcom.model.SpouseRef;
import org.folg.gedcom.model.Visitor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GedcomAnalyzerService {

    private final SearchService searchService;
    private final PersonMapper personMapper;

    public GedcomAnalysisDto analyze(EnrichedGedcom gedcom) {
        log.info("Analyze gedcom: {}", gedcom.getGedcomName());

        List<PersonComparisonResults> duplicatedPersons = searchService.findDuplicatedPersons(gedcom);
        List<PersonDuplicateDto> personDuplicatesDto = personMapper.toPersonDuplicateDto(duplicatedPersons);

        List<EnrichedPerson> alivePersons = searchService.findAlivePersonsTooOldOrWithFamilyMembersTooOld(gedcom.getPeople());
        List<PersonDto> invalidAlivePersonsDto = personMapper.toPersonDto(alivePersons);

        log.info("Gedcom analyzed: {}", gedcom.getGedcomName());
        return GedcomAnalysisDto.builder()
                .personsCount(gedcom.getGedcom().getPeople().size())
                .familiesCount(gedcom.getGedcom().getFamilies().size())
                .personDuplicates(personDuplicatesDto)
                .invalidAlivePersons(invalidAlivePersonsDto)
                .build();
    }

    /**
     * Analyse GEDCOM for one way relationships and return the missing ones.
     * i.e: On MyHeritage GEDCOM files adopted children are not included in the family group.
     */
    public List<Reference> getMissingReferences(Gedcom gedcom) {

        List<Reference> references = new ArrayList<>();

        for (Person person : gedcom.getPeople()) {
            for (SpouseFamilyRef spouseRef : person.getSpouseFamilyRefs()) {
                Family family = gedcom.getFamily(spouseRef.getRef());
                boolean spouseRefInHusbands = family.getHusbandRefs().stream().anyMatch(ref -> ref.getRef().equals(person.getId()));
                boolean spouseRefInWives = family.getWifeRefs().stream().anyMatch(ref -> ref.getRef().equals(person.getId()));
                if (!spouseRefInHusbands && !spouseRefInWives) {
                    ReferenceType spouseType = resolveSpouseReferenceType(person.getEventsFacts());
                    references.add(Reference.of(family, ReferenceType.FAMILY, person, spouseType));
                }
            }

            for (ParentFamilyRef parentRef : person.getParentFamilyRefs()) {
                Family family = gedcom.getFamily(parentRef.getRef());
                boolean containsReference = family.getChildRefs().stream().anyMatch(ref -> ref.getRef().equals(person.getId()));
                if (!containsReference) {
                    ReferenceType childType = resolveChildReferenceType(parentRef.getRelationshipType());
                    references.add(Reference.of(family, ReferenceType.FAMILY, person, childType));
                }
            }
        }

        for (Family family : gedcom.getFamilies()) {
            for (SpouseRef ref : family.getHusbandRefs()) {
                Person person = gedcom.getPerson(ref.getRef());
                boolean containsRef = person.getSpouseFamilyRefs().stream().anyMatch(spouseFamilyRef -> spouseFamilyRef.getRef().equals(family.getId()));
                if (!containsRef) {
                    ReferenceType spouseType = resolveSpouseReferenceType(ReferenceType.HUSB, person.getEventsFacts());
                    references.add(Reference.of(person, spouseType, family, ReferenceType.FAMILY));
                }
            }

            for (SpouseRef ref : family.getWifeRefs()) {
                Person person = gedcom.getPerson(ref.getRef());
                boolean containsRef = person.getSpouseFamilyRefs().stream().anyMatch(spouseFamilyRef -> spouseFamilyRef.getRef().equals(family.getId()));
                if (!containsRef) {
                    ReferenceType spouseType = resolveSpouseReferenceType(ReferenceType.WIFE, person.getEventsFacts());
                    references.add(Reference.of(person, spouseType, family, ReferenceType.FAMILY));
                }
            }

            for (ChildRef ref : family.getChildRefs()) {
                Person person = gedcom.getPerson(ref.getRef());
                boolean containsRef = person.getParentFamilyRefs().stream().anyMatch(parentFamilyRef -> parentFamilyRef.getRef().equals(family.getId()));
                if (!containsRef) {
                    references.add(Reference.of(person, ReferenceType.CHILD, family, ReferenceType.FAMILY));
                }
            }
        }

        return List.copyOf(references);
    }

    private static ReferenceType resolveSpouseReferenceType(List<EventFact> eventsFacts) {
        ReferenceType spouseType = eventsFacts
                .stream()
                .filter(event -> PersonUtils.SEX_TAGS.contains(event.getTag()))
                .findFirst()
                .filter(event -> event.getValue().equals("F"))
                .map(event -> ReferenceType.WIFE)
                .orElse(ReferenceType.HUSB);

        return resolveSpouseReferenceType(spouseType, eventsFacts);
    }

    private static ReferenceType resolveSpouseReferenceType(ReferenceType referenceType, List<EventFact> eventsFacts) {
        return eventsFacts
                .stream()
                .filter(event -> FamilyUtils.DIVORCE_TAGS.contains(event.getTag()))
                .findFirst()
                .filter(event -> event.getValue().equals("Y"))
                .map(event -> referenceType == ReferenceType.WIFE ? ReferenceType.DIV_WIFE : ReferenceType.DIV_HUSB)
                .orElse(referenceType);
    }

    private static ReferenceType resolveChildReferenceType(String relationshipType) {
        if ("Adopted".equalsIgnoreCase(relationshipType)) {
            return ReferenceType.ADOPTED_CHILD;
        }
        if ("Foster".equalsIgnoreCase(relationshipType)) {
            return ReferenceType.FOSTER_CHILD;
        }
        return ReferenceType.CHILD;
    }

    public List<String> getAllPlaces(Gedcom gedcom, boolean reversePlace) {
        Set<String> places = new HashSet<>();

        gedcom.accept(new Visitor() {
            @Override
            public boolean visit(EventFact eventFact) {
                if (StringUtils.isNotBlank(eventFact.getPlace())) {
                    places.add(eventFact.getPlace().trim());
                }
                return true;
            }
        });

        return places
                .stream()
                .map(place -> StringUtils.splitByWholeSeparator(place, ", "))
                .peek(ArrayUtils::reverse)
                .sorted((a1, a2) -> {
                    int cmp = 0;
                    int i = 0;
                    while (i < a1.length && i < a2.length && (cmp = a1[i].compareTo(a2[i])) == 0) {
                        i++;
                    }
                    return cmp == 0 ? Integer.compare(a1.length, a2.length) : cmp;
                })
                .peek(place -> {
                    if (!reversePlace) {
                        ArrayUtils.reverse(place);
                    }})
                .map(place -> StringUtils.join(place, ", "))
                .toList();
    }

    /**
     * Analyse GEDCOM for orphan trees: sub-graph not connected to first person's sub-graph.
     */
    public List<EnrichedPerson> getInitialPersonOfOrphanTrees(EnrichedGedcom gedcom) {

        Map<String, UUID> visitedPersons = new HashMap<>();
        Set<String> orphanPersons = new LinkedHashSet<>();

        gedcom.getPeople()
                .forEach(person -> getReachedSubTreeIds(person, visitedPersons, orphanPersons, UUID.randomUUID(), 0));

        String firstPersonId = gedcom.getPeople().isEmpty() ? null : gedcom.getPeople().get(0).getId();

        return orphanPersons
                .stream()
                // Skip first person of the GEDCOM
                .filter(personId -> firstPersonId == null || !firstPersonId.equals(personId))
                .map(gedcom::getPersonById)
                .toList();
    }

    /**
     * Returns the reached sub-tree ids for a person.
     * Modifies visitedPersons and orphanPersons collections.
     */
    private static Set<UUID> getReachedSubTreeIds(
            EnrichedPerson person,
            Map<String, UUID> visitedPersons,
            Set<String> orphanPersons,
            UUID subTreeId,
            int level) {

        if (visitedPersons.containsKey(person.getId())) {
            // Return the run UUID in which this person was visited
            return Set.of(visitedPersons.get(person.getId()));
        }

        visitedPersons.put(person.getId(), subTreeId);

        if (level == 100) {
            // If max level or recursion is reached, stop the search
            return Set.of(subTreeId);
        }

        Set<UUID> reachedSubTreeIds = Stream.of(
                        person.getParents(),
                        person.getSpouses(),
                        person.getChildren())
                .flatMap(List::stream)
                .map(relative -> getReachedSubTreeIds(
                        relative,
                        visitedPersons,
                        orphanPersons,
                        subTreeId,
                        level + 1))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        // Add current sub-tree id to the set of reached sub-tree ids
        reachedSubTreeIds.add(subTreeId);

        if (level == 0) {
            if (reachedSubTreeIds.size() == 1 && reachedSubTreeIds.contains(subTreeId)) {
                orphanPersons.add(person.getId());
            } else {
                // If a considered "orphan" sub-tree is reached later, then remove it from result list
                orphanPersons
                        .stream()
                        .filter(personId -> reachedSubTreeIds.contains(visitedPersons.get(personId)))
                        .toList()
                        .forEach(orphanPersons::remove);
            }
        }

        return reachedSubTreeIds;
    }

    /**
     * Analyse GEDCOM for orphan trees: sub-graph not connected to first person's sub-graph.
     */
    public Map<EnrichedPerson, String> getMostFrequentSurnamesByPersonSubTree(List<EnrichedPerson> people) {

        Set<String> visitedPersons = new HashSet<>();

        return people
                .stream()
                .map(person -> {
                    List<String> surnames = treeTraversal(
                            person,
                            visitedPersons,
                            0,
                            p -> p.getSurname()
                                    .orElse("Undefined"));
                    Map<String, Integer> cardinality = CollectionUtils.getCardinalityMap(surnames);
                    return Map.entry(
                            person,
                            cardinality
                                    .entrySet()
                                    .stream()
                                    .reduce((e1, e2) -> e1.getValue() < e2.getValue() ? e2 : e1)
                                    .map(Map.Entry::getKey));
                })
                .filter(entry -> entry.getValue().isPresent())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get(),
                        (u, v) -> u,
                        LinkedHashMap::new));
    }

    /**
     * .
     */
    public List<Pair<String, Integer>> getPlacesOfBirthCardinality(List<EnrichedPerson> people, boolean includeEmptyValues) {

        List<String> placesOfBirth = people
                .stream()
                .map(EnrichedPerson::getPlaceOfBirth)
                .map(countryOfBirth -> countryOfBirth.or(() -> includeEmptyValues ? Optional.of("<no place>") : Optional.empty()))
                .flatMap(Optional::stream)
                .toList();

        Map<String, Integer> cardinality = CollectionUtils.getCardinalityMap(placesOfBirth);

        return cardinality
                .entrySet()
                .stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed())
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * .
     */
    public List<Pair<String, Integer>> getPlacesOfBirthCardinalityGroupedByCountry(List<EnrichedPerson> people, boolean includeEmptyValues) {

        List<String> countriesOfBirth = people
                .stream()
                .map(EnrichedPerson::getCountryOfBirthForSearch)
                .map(countryOfBirth -> countryOfBirth.or(() -> includeEmptyValues ? Optional.of("<no country>") : Optional.empty()))
                .flatMap(Optional::stream)
                .toList();

        Map<String, Integer> cardinality = CollectionUtils.getCardinalityMap(countriesOfBirth);

        return cardinality
                .entrySet()
                .stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed())
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * .
     */
    public Map<DateUtils.AstrologicalSign, Integer> getAstrologicalSignsCardinality(List<EnrichedPerson> people, boolean useMinDayOnOverlap) {

        List<DateUtils.AstrologicalSign> astrologicalSigns = people
                .stream()
                .flatMap(person -> person.getDateOfBirth()
                        .filter(Date::isFullDate)
                        .map(dob -> DateUtils.getAstrologicalSign(dob.getMonth(), dob.getDay(), useMinDayOnOverlap))
                        .stream())
                .toList();

        return EnumCollectionUtils.getCardinalityMap(astrologicalSigns, DateUtils.AstrologicalSign.class);
    }

    /**
     * .
     */
    public Pair<Optional<EnrichedPerson>, Optional<EnrichedPerson>> getMinMaxFullDateOfBirth(List<EnrichedPerson> people) {

        List<EnrichedPerson> peopleWithFullDateOfBirth = people
                .stream()
                .filter(person -> person.getDateOfBirth().isPresent())
                .filter(person -> person.getDateOfBirth().get().isFullDate())
                .toList();

        Optional<EnrichedPerson> minDateOfBirth = peopleWithFullDateOfBirth
                .stream()
                .reduce((p1, p2) -> p1.getDateOfBirth().get().isBefore(p2.getDateOfBirth().get().toLocalDate()) ? p1 : p2);

        Optional<EnrichedPerson> maxDateOfBirth = peopleWithFullDateOfBirth
                .stream()
                .reduce((p1, p2) -> p2.getDateOfBirth().get().isBefore(p1.getDateOfBirth().get().toLocalDate()) ? p1 : p2);

        return Pair.of(minDateOfBirth, maxDateOfBirth);
    }

    /**
     *
     */
    private static <T> List<T> treeTraversal(
            EnrichedPerson person,
            Set<String> visitedPersons,
            int level,
            Function<EnrichedPerson, T> valueExtractor) {

        if (visitedPersons.contains(person.getId())) {
            return List.of();
        }

        visitedPersons.add(person.getId());

        if (level == 100) {
            // If max level or recursion is reached, stop the search
            T value = valueExtractor.apply(person);
            return value == null ? List.of() : List.of(value);
        }

        List<T> results = Stream.of(
                        person.getParents(),
                        person.getSpouses(),
                        person.getChildren())
                .flatMap(List::stream)
                .map(relative -> treeTraversal(
                        relative,
                        visitedPersons,
                        level + 1,
                        valueExtractor))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // Add current node value to the results list
        T value = valueExtractor.apply(person);
        if (value != null) {
            results.add(value);
        }

        return List.copyOf(results);
    }

    /**
     *
     */
    public EnrichedGedcom extractSubGedcom(EnrichedPerson person) {
        Gedcom subGedcom = person.getGedcom().getGedcom();
        String subGedcomName = person.getSurname()
                .map(surname -> surname + "-tree")
                .orElse("sub-gedcom");
        return EnrichedGedcom.of(subGedcom, subGedcomName, person.getProperties());
    }

}
