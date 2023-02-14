package com.geneaazul.gedcomanalyzer.repository;

import com.geneaazul.gedcomanalyzer.domain.SearchFamily;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public interface SearchFamilyRepository extends JpaRepository<SearchFamily, Long> {

    int countByClientIpAddressAndCreateDateBetween(String clientIpAddress, OffsetDateTime createDateFrom, OffsetDateTime createDateTo);

}
