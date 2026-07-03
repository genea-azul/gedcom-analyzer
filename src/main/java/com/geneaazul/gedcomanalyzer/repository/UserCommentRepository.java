package com.geneaazul.gedcomanalyzer.repository;

import com.geneaazul.gedcomanalyzer.domain.UserComment;
import com.geneaazul.gedcomanalyzer.model.dto.CommentContextType;
import com.geneaazul.gedcomanalyzer.model.dto.CommentStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface UserCommentRepository extends JpaRepository<UserComment, Long> {

    long countByClientIpAddressAndCreateDateBetween(
            String clientIpAddress, OffsetDateTime from, OffsetDateTime to);

    List<UserComment> findByContextTypeAndContextIdAndStatusOrderByCreateDateAsc(
            CommentContextType contextType, String contextId, CommentStatus status);

    List<UserComment> findByContextTypeAndContextIdAndAuthorWantsContactTrueAndAuthorEmailIsNotNullAndStatus(
            CommentContextType contextType, String contextId, CommentStatus status);

}
