package com.geneaazul.gedcomanalyzer.repository;

import com.geneaazul.gedcomanalyzer.domain.TreeBuilderSubmission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public interface TreeBuilderSubmissionRepository extends JpaRepository<TreeBuilderSubmission, Long> {

    long countByClientIpAddressAndCreateDateBetween(String clientIpAddress, OffsetDateTime createDateFrom, OffsetDateTime createDateTo);

}
