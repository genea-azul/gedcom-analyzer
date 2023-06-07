package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.dto.ReferenceType;
import com.geneaazul.gedcomanalyzer.model.dto.RelationshipDto;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.annotation.Nullable;

@Component
public class RelationshipMapper {

    public RelationshipDto toRelationshipDto(
            @Nullable EnrichedPerson person,
            @Nullable Relationship relationship,
            Predicate<EnrichedPerson> obfuscatePredicate) {

        if (person == null || relationship == null) {
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
            referenceType = grade < 0
                    ? (relationship.isInLaw() ? ReferenceType.SPOUSE : ReferenceType.SELF)
                    : (grade == 0 ? ReferenceType.SIBLING : ReferenceType.COUSIN);
        } else {
            generation = relationship.distanceToAncestor2() - relationship.distanceToAncestor1();
            grade = relationship.distanceToAncestor1();
            referenceType = grade == 0 ? ReferenceType.CHILD : ReferenceType.NIBLING;
        }

        SexType spouseSex = null;
        if (relationship.isInLaw()) {
            Integer relatedPersons = Optional
                    .ofNullable(relationship.relatedPersonIds())
                    .map(Set::size)
                    .orElse(0);
            if (relatedPersons == 1) {
                spouseSex = person
                        .getGedcom()
                        .getPersonById(relationship.relatedPersonIds().first())
                        .getSex();
            }
        }

        return RelationshipDto.builder()
                .personSex(person.getSex())
                .personName(PersonUtils.obfuscateName(person, obfuscatePredicate.test(person)))
                .referenceType(referenceType)
                .generation(generation)
                .grade(grade)
                .isInLaw(relationship.isInLaw())
                .spouseSex(spouseSex)
                .isHalf(relationship.isHalf())
                .build();
    }

}
