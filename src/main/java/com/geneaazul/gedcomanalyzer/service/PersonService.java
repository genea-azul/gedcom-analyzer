package com.geneaazul.gedcomanalyzer.service;

import com.geneaazul.gedcomanalyzer.model.AncestryGenerations;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PersonService {

    public List<String> getAncestryCountries(EnrichedPerson person) {

        Set<String> visitedPersons = new HashSet<>();

        return person
                .getParents()
                .stream()
                .map(parent -> getAncestryCountries(parent, visitedPersons, 0))
                .flatMap(Set::stream)
                .distinct()
                .sorted()
                .toList();
    }

    private static Set<String> getAncestryCountries(
            EnrichedPerson person,
            Set<String> visitedPersons,
            int level) {

        if (visitedPersons.contains(person.getId())) {
            return Set.of();
        }

        visitedPersons.add(person.getId());

        if (level == 20) {
            // If max level or recursion is reached, stop the search
            return person.getCountryOfBirthForSearch()
                    .map(Set::of)
                    .orElseGet(Set::of);
        }

        Set<String> ancestryCountries = person
                .getParents()
                .stream()
                .map(parent -> getAncestryCountries(
                        parent,
                        visitedPersons,
                        level + 1))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        // Add person's country to the set of ancestry countries
        person.getCountryOfBirthForSearch()
                .ifPresent(ancestryCountries::add);

        return ancestryCountries;
    }

    public AncestryGenerations getAncestryGenerations(EnrichedPerson person) {

        Set<String> visitedPersons = new HashSet<>();

        int ascendingGenerations = getAncestryGenerations(person, visitedPersons, EnrichedPerson::getParents, 0);
        int descendingGenerations = getAncestryGenerations(person, visitedPersons, EnrichedPerson::getChildren, 0);

        return AncestryGenerations.of(ascendingGenerations, descendingGenerations);
    }

    private static Integer getAncestryGenerations(
            EnrichedPerson person,
            Set<String> visitedPersons,
            Function<EnrichedPerson, List<EnrichedPerson>> relativesResolver,
            int level) {

        if (level > 0 && visitedPersons.contains(person.getId())) {
            return level;
        }

        visitedPersons.add(person.getId());

        if (level == 20) {
            // If max level or recursion is reached, stop the search
            return level;
        }

        return relativesResolver
                .apply(person)
                .stream()
                .map(parent -> getAncestryGenerations(
                        parent,
                        visitedPersons,
                        relativesResolver,
                        level + 1))
                .reduce(Integer::max)
                .orElse(level);
    }

}
