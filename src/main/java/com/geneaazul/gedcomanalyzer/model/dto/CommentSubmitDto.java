package com.geneaazul.gedcomanalyzer.model.dto;

import org.apache.commons.lang3.StringUtils;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
public class CommentSubmitDto {

    @NotNull
    @ToString.Include
    private CommentContextType contextType;

    @NotBlank
    @Size(max = 255)
    @ToString.Include
    private String contextId;

    @NotNull
    @ToString.Include
    private CommentType commentType;

    @NotBlank
    @Size(max = 100)
    @ToString.Include
    private String authorName;

    @Nullable
    @Size(max = 100)
    private String authorLocation;

    @Nullable
    @Email
    @Size(max = 150)
    private String authorEmail;

    @Builder.Default
    private boolean authorWantsContact = false;

    @NotBlank
    @Size(max = 2000)
    private String body;

    @AssertTrue(message = "authorEmail is required when authorWantsContact is true")
    private boolean isAuthorEmailPresentWhenWantsContact() {
        return !authorWantsContact || StringUtils.isNotBlank(authorEmail);
    }

}
