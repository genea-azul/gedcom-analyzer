package com.geneaazul.gedcomanalyzer.model.dto;

import java.time.OffsetDateTime;

import jakarta.annotation.Nullable;

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
public class CommentDetailsDto {

    @ToString.Include
    private Long id;
    @ToString.Include
    private CommentContextType contextType;
    @ToString.Include
    private String contextId;
    @ToString.Include
    private CommentType commentType;
    @ToString.Include
    private String authorName;
    @Nullable
    @ToString.Include
    private String authorLocation;
    @Nullable
    private String authorEmail;
    @ToString.Include
    private Boolean authorWantsContact;
    @ToString.Include
    private String body;
    @ToString.Include
    private CommentStatus status;
    @ToString.Include
    private OffsetDateTime createDate;
    @Nullable
    private String clientIpAddress;

}
