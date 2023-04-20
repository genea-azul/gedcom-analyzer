package com.geneaazul.gedcomanalyzer.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(onlyExplicitlyIncluded = true)
@Embeddable
public class SearchPerson {

    @ToString.Include
    private String givenName;
    @ToString.Include
    private String surname;
    @ToString.Include
    private Boolean isAlive;
    @ToString.Include
    private Integer yearOfBirth;
    @ToString.Include
    private Integer yearOfDeath;
    @ToString.Include
    private String placeOfBirth;

}
