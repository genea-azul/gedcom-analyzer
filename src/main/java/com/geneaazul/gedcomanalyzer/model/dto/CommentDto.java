package com.geneaazul.gedcomanalyzer.model.dto;

import java.time.OffsetDateTime;

import jakarta.annotation.Nullable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class CommentDto {

    @ToString.Include
    private Long id;
    @ToString.Include
    private CommentType commentType;
    @ToString.Include
    private String authorName;
    @Nullable
    @ToString.Include
    private String authorLocation;
    @ToString.Include
    private String body;
    @ToString.Include
    private OffsetDateTime createDate;

}
