package com.geneaazul.gedcomanalyzer.domain;

import com.geneaazul.gedcomanalyzer.model.dto.SexType;

import java.time.OffsetDateTime;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
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
@Entity
public class SearchFamily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createDate;
    @Column(nullable = false)
    private OffsetDateTime updateDate;
    @Column(nullable = false)
    @Version
    @Builder.Default
    private Integer version = 0;
    @ToString.Include
    @Enumerated(EnumType.STRING)
    private SexType individualSex;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "givenName", column = @Column(name = "individual_given_name")),
            @AttributeOverride(name = "surname", column = @Column(name = "individual_surname")),
            @AttributeOverride(name = "isAlive", column = @Column(name = "individual_is_alive")),
            @AttributeOverride(name = "yearOfBirth", column = @Column(name = "individual_year_of_birth")),
            @AttributeOverride(name = "yearOfDeath", column = @Column(name = "individual_year_of_death"))
    })
    @ToString.Include
    private SearchPerson individual;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "givenName", column = @Column(name = "father_given_name")),
            @AttributeOverride(name = "surname", column = @Column(name = "father_surname")),
            @AttributeOverride(name = "isAlive", column = @Column(name = "father_is_alive")),
            @AttributeOverride(name = "yearOfBirth", column = @Column(name = "father_year_of_birth")),
            @AttributeOverride(name = "yearOfDeath", column = @Column(name = "father_year_of_death"))
    })
    @ToString.Include
    private SearchPerson father;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "givenName", column = @Column(name = "mother_given_name")),
            @AttributeOverride(name = "surname", column = @Column(name = "mother_surname")),
            @AttributeOverride(name = "isAlive", column = @Column(name = "mother_is_alive")),
            @AttributeOverride(name = "yearOfBirth", column = @Column(name = "mother_year_of_birth")),
            @AttributeOverride(name = "yearOfDeath", column = @Column(name = "mother_year_of_death"))
    })
    @ToString.Include
    private SearchPerson mother;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "givenName", column = @Column(name = "paternal_grandfather_given_name")),
            @AttributeOverride(name = "surname", column = @Column(name = "paternal_grandfather_surname")),
            @AttributeOverride(name = "isAlive", column = @Column(name = "paternal_grandfather_is_alive")),
            @AttributeOverride(name = "yearOfBirth", column = @Column(name = "paternal_grandfather_year_of_birth")),
            @AttributeOverride(name = "yearOfDeath", column = @Column(name = "paternal_grandfather_year_of_death"))
    })
    @ToString.Include
    private SearchPerson paternalGrandfather;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "givenName", column = @Column(name = "paternal_grandmother_given_name")),
            @AttributeOverride(name = "surname", column = @Column(name = "paternal_grandmother_surname")),
            @AttributeOverride(name = "isAlive", column = @Column(name = "paternal_grandmother_is_alive")),
            @AttributeOverride(name = "yearOfBirth", column = @Column(name = "paternal_grandmother_year_of_birth")),
            @AttributeOverride(name = "yearOfDeath", column = @Column(name = "paternal_grandmother_year_of_death"))
    })
    @ToString.Include
    private SearchPerson paternalGrandmother;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "givenName", column = @Column(name = "maternal_grandfather_given_name")),
            @AttributeOverride(name = "surname", column = @Column(name = "maternal_grandfather_surname")),
            @AttributeOverride(name = "isAlive", column = @Column(name = "maternal_grandfather_is_alive")),
            @AttributeOverride(name = "yearOfBirth", column = @Column(name = "maternal_grandfather_year_of_birth")),
            @AttributeOverride(name = "yearOfDeath", column = @Column(name = "maternal_grandfather_year_of_death"))
    })
    @ToString.Include
    private SearchPerson maternalGrandfather;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "givenName", column = @Column(name = "maternal_grandmother_given_name")),
            @AttributeOverride(name = "surname", column = @Column(name = "maternal_grandmother_surname")),
            @AttributeOverride(name = "isAlive", column = @Column(name = "maternal_grandmother_is_alive")),
            @AttributeOverride(name = "yearOfBirth", column = @Column(name = "maternal_grandmother_year_of_birth")),
            @AttributeOverride(name = "yearOfDeath", column = @Column(name = "maternal_grandmother_year_of_death"))
    })
    @ToString.Include
    private SearchPerson maternalGrandmother;
    @ToString.Include
    private String contact;

    @PrePersist
    protected void onCreate() {
        this.createDate = OffsetDateTime.now();
        this.updateDate = this.createDate;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateDate = OffsetDateTime.now();
    }

}
