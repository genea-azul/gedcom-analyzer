package com.geneaazul.gedcomanalyzer.domain;

import com.geneaazul.gedcomanalyzer.model.dto.CommentContextType;
import com.geneaazul.gedcomanalyzer.model.dto.CommentStatus;
import com.geneaazul.gedcomanalyzer.model.dto.CommentType;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        @Index(name = "user_comment_context_idx", columnList = "contextType, contextId, status"),
        @Index(name = "user_comment_client_ip_date_idx", columnList = "clientIpAddress, createDate")
})
public class UserComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ToString.Include
    private CommentContextType contextType;

    @Column(nullable = false, length = 255)
    @ToString.Include
    private String contextId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ToString.Include
    private CommentType commentType;

    @Column(nullable = false, length = 100)
    @ToString.Include
    private String authorName;

    @Column(length = 100)
    private String authorLocation;

    @Column(length = 150)
    private String authorEmail;

    @Builder.Default
    @Column(nullable = false)
    private Boolean authorWantsContact = false;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommentStatus status = CommentStatus.PENDING;

    private String clientIpAddress;

    @PrePersist
    protected void onCreate() {
        this.createDate = OffsetDateTime.now();
    }

}
