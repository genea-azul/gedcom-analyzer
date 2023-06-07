package com.geneaazul.gedcomanalyzer.model.dto;

import org.apache.commons.lang3.StringUtils;

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
    private String personName;
    private ReferenceType referenceType;
    private Integer generation;
    private Integer grade;
    private Boolean isInLaw;
    private SexType spouseSex;
    private Boolean isHalf;

    @Override
    public String toString() {
        return "[" + personSex + ", "
                + StringUtils.leftPad(referenceType.toString(), 7) + ", "
                + generation + ", "
                + grade + ", "
                + (isInLaw ? " " + isInLaw : isInLaw) + ", "
                + (isHalf ? " " + isHalf : isHalf) + "]";
    }

}
