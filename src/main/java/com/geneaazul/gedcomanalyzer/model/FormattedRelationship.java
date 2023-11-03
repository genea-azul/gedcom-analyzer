package com.geneaazul.gedcomanalyzer.model;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public record FormattedRelationship(
        String index,
        String personName,
        String personSex,
        String personIsAlive,
        String personYearOfBirth,
        String personCountryOfBirth,
        String adoption,
        String distinguishedPerson,
        String treeSide,
        String relationshipDesc,
        boolean isObfuscated) {

    public FormattedRelationship mergeRelationshipDesc(FormattedRelationship other) {
        if (Objects.equals(this.relationshipDesc, other.relationshipDesc)) {
            return this;
        }
        if (StringUtils.isBlank(this.relationshipDesc)) {
            return other;
        }
        if (StringUtils.isBlank(other.relationshipDesc)) {
            return this;
        }
        return new FormattedRelationship(
                this.index,
                this.personName,
                this.personSex,
                this.personIsAlive,
                this.personYearOfBirth,
                this.personCountryOfBirth,
                this.adoption,
                this.distinguishedPerson,
                this.treeSide,
                this.relationshipDesc + " | " + other.relationshipDesc,
                this.isObfuscated);
    }

}
