package com.geneaazul.gedcomanalyzer.model.dto;

import org.apache.commons.lang3.StringUtils;

import java.util.Set;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RelationshipDto {

    private SexType personSex;
    private Boolean personIsAlive;
    private String personName;
    private Integer personYearOfBirth;
    private Boolean personYearOfBirthIsAbout;
    private String personCountryOfBirth;
    private ReferenceType referenceType;
    private Integer generation;
    private Integer grade;
    private Boolean isInLaw;
    private Boolean isHalf;
    private AdoptionType adoptionType;
    private SexType spouseSex;
    private Boolean isSeparated;
    private Set<TreeSideType> treeSides;
    private Boolean isObfuscated;

    @Override
    public String toString() {
        return "["
                + personSex + ", "
                + (personIsAlive ? "A" : "D") + ", "
                + StringUtils.leftPad(personName, 26) + ", "
                + StringUtils.leftPad(personCountryOfBirth, 10) + ", "
                + StringUtils.leftPad(referenceType.toString(), 7) + ", "
                + generation + ", "
                + grade + ", "
                + (isInLaw ? " " + isInLaw : isInLaw) + ", "
                + (isHalf ? " " + isHalf : isHalf) + ", "
                + (adoptionType == null ? "      " : adoptionType) + ", "
                + spouseSex + ", "
                + (isSeparated ? " " + isSeparated : isSeparated) + ", "
                + treeSides + "]";
    }

}
