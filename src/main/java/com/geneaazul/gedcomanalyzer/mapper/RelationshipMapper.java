package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.model.EnrichedGedcom;
import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.dto.ReferenceType;
import com.geneaazul.gedcomanalyzer.model.dto.RelationshipDto;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;

import org.springframework.stereotype.Component;

import java.util.function.Predicate;

import jakarta.annotation.Nullable;

@Component
public class RelationshipMapper {

    public RelationshipDto toRelationshipDto(
            @Nullable String personId,
            @Nullable Relationship relationship,
            EnrichedGedcom gedcom,
            Predicate<EnrichedPerson> obfuscatePredicate) {

        if (personId == null || relationship == null) {
            return null;
        }

        ReferenceType referenceType;
        int generation;
        int grade;

        if (relationship.distanceToAncestor1() > relationship.distanceToAncestor2()) {
            generation = relationship.distanceToAncestor1() - relationship.distanceToAncestor2();
            grade = relationship.distanceToAncestor2();
            referenceType = grade == 0 ? ReferenceType.PARENT : ReferenceType.PIBLING;
        } else if (relationship.distanceToAncestor1() == relationship.distanceToAncestor2()) {
            generation = 0;
            grade = relationship.distanceToAncestor1() - 1;
            referenceType = grade < 0 ? ReferenceType.SELF : (grade == 0 ? ReferenceType.SIBLING : ReferenceType.COUSIN);
        } else {
            generation = relationship.distanceToAncestor2() - relationship.distanceToAncestor1();
            grade = relationship.distanceToAncestor1();
            referenceType = grade == 0 ? ReferenceType.CHILD : ReferenceType.NIBLING;
        }

        EnrichedPerson person = gedcom.getPersonById(personId);

        return RelationshipDto.builder()
                .personSex(person.getSex())
                .personName(PersonUtils.obfuscateName(person, obfuscatePredicate.test(person)))
                .referenceType(referenceType)
                .generation(generation)
                .grade(grade)
                .isSpouse(relationship.isSpouse())
                .isHalf(relationship.isHalf())
                .build();
    }

}
