package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.model.dto.ReferenceType;

import org.apache.commons.lang3.tuple.Pair;
import org.folg.gedcom.model.Person;

import java.util.List;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class SpouseWithChildren {

    private final Optional<Person> spouse;
    private final List<Pair<Person, ReferenceType>> children;
    private final Optional<Date> dateOfPartners;
    private final Optional<Date> dateOfSeparation;
    private final Optional<String> placeOfPartners;
    private final Optional<String> placeOfSeparation;

    public static SpouseWithChildren of(
            Optional<Person> spouse,
            List<Pair<Person, ReferenceType>> children,
            Optional<Date> dateOfPartners,
            Optional<Date> dateOfSeparation,
            Optional<String> placeOfPartners,
            Optional<String> placeOfSeparation) {
        return new SpouseWithChildren(spouse, children, dateOfPartners, dateOfSeparation, placeOfPartners, placeOfSeparation);
    }

}
