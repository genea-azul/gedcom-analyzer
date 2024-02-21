package com.geneaazul.gedcomanalyzer.model;

import com.geneaazul.gedcomanalyzer.model.dto.ReferenceType;

import org.apache.commons.lang3.tuple.Pair;
import org.folg.gedcom.model.Person;

import java.util.List;
import java.util.Optional;

public record SpouseWithChildren(
        Optional<Person> spouse,
        List<Pair<Integer, Optional<ReferenceType>>> children,
        boolean isSeparated,
        Optional<Date> dateOfPartners,
        Optional<Date> dateOfSeparation,
        Optional<Place> placeOfPartners,
        Optional<Place> placeOfSeparation) {

}
