package com.geneaazul.gedcomanalyzer.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
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
@Table(indexes = {
        @Index(name = "search_connection_client_ip_address_create_date_idx", columnList = "clientIpAddress, createDate"),
        @Index(name = "search_connection_is_match_idx", columnList = "isMatch"),
        @Index(name = "search_connection_is_reviewed_idx", columnList = "isReviewed")
})
public class SearchConnection {

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

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "givenName", column = @Column(name = "person_1_given_name")),
            @AttributeOverride(name = "surname", column = @Column(name = "person_1_surname")),
            @AttributeOverride(name = "yearOfBirth", column = @Column(name = "person_1_year_of_birth"))
    })
    @ToString.Include
    private SearchPersonSimple person1;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "givenName", column = @Column(name = "person_2_given_name")),
            @AttributeOverride(name = "surname", column = @Column(name = "person_2_surname")),
            @AttributeOverride(name = "yearOfBirth", column = @Column(name = "person_2_year_of_birth"))
    })
    @ToString.Include
    private SearchPersonSimple person2;

    @ToString.Include
    private Boolean isMatch;

    @ToString.Include
    private Integer distance;

    @ToString.Include
    private String errorMessages;

    @ToString.Include
    private Boolean isReviewed;

    @ToString.Include
    private String clientIpAddress;

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
