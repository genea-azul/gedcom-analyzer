package com.geneaazul.gedcomanalyzer.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

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
        @Index(name = "tree_builder_submission_client_ip_address_create_date_idx", columnList = "clientIpAddress, createDate")
})
public class TreeBuilderSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createDate;

    @Column(nullable = false, columnDefinition = "text")
    @ToString.Include
    private String payload;

    @ToString.Include
    private String contact;

    @ToString.Include
    private String clientIpAddress;

    @PrePersist
    protected void onCreate() {
        this.createDate = OffsetDateTime.now();
    }

}
