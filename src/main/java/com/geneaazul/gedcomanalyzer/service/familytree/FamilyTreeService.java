package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.Relationships;
import com.geneaazul.gedcomanalyzer.model.Surname;
import com.geneaazul.gedcomanalyzer.service.PersonService;
import com.geneaazul.gedcomanalyzer.utils.NameUtils;

import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class FamilyTreeService {

    private final PersonService personService;

    public abstract void generateFamilyTree(
            EnrichedPerson person,
            boolean obfuscateLiving);

    protected String getFamilyTreeFileId(EnrichedPerson person) {
        return Stream.of(
                        person
                                .getGivenName()
                                .map(GivenName::value),
                        person
                                .getSurname()
                                .map(Surname::value))
                .flatMap(Optional::stream)
                .reduce((n1, n2) -> n1 + "_" + n2)
                .map(NameUtils::simplifyName)
                .map(name -> name.replaceAll(" ", "_"))
                .orElse("genea-azul");
    }

    protected List<List<Relationship>> getRelationshipsWithNotInLawPriority(EnrichedPerson person) {
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
