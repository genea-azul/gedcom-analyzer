package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.Relationships;
import com.geneaazul.gedcomanalyzer.model.Surname;
import com.geneaazul.gedcomanalyzer.service.PersonService;

import org.springframework.stereotype.Service;

import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FamilyTreeHelper {

    private final PersonService personService;

    public String getFamilyTreeFileId(EnrichedPerson person) {
        return Stream.of(
                        person
                                .getGivenName()
                                .map(GivenName::simplified),
                        person
                                .getSurname()
                                .map(Surname::simplified))
                .flatMap(Optional::stream)
                .reduce((n1, n2) -> n1 + "_" + n2)
                .map(name -> name.replaceAll(" ", "_"))
                .orElse("genea-azul");
    }

    public List<List<Relationship>> getRelationshipsWithNotInLawPriority(EnrichedPerson person) {
        List<Relationships> relationshipsList = personService.setTransientProperties(person, false);

        MutableInt orderKey = new MutableInt(1);

        return relationshipsList
                .stream()
                // Make sure each relationship group has 1 or 2 elements (usually an in-law and a not-in-law relationship)
                .peek(relationships -> {
                    if (relationships.size() == 0 || relationships.size() > 2) {
                        throw new UnsupportedOperationException("Something is wrong");
                    }
                })
                // Order internal elements of each relationship group: first not-in-law, then in-law
                .map(relationships -> {
                    if (relationships.size() == 2 && relationships.findFirst().isInLaw()) {
                        return List.of(relationships.findLast(), relationships.findFirst());
                    }
                    return List.copyOf(relationships.getOrderedRelationships());
                })
                .sorted(Comparator.comparing(relationships -> relationships.get(0)))
                .peek(relationships -> relationships.get(0).person().setOrderKey(orderKey.getAndIncrement()))
                .toList();
    }

}
