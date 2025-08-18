package com.geneaazul.gedcomanalyzer.repository;

import com.geneaazul.gedcomanalyzer.domain.SearchFamily;
import com.geneaazul.gedcomanalyzer.repository.projection.SearchFamilyProjection;

import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.annotation.Nullable;

@Repository
public interface SearchFamilyRepository extends JpaRepository<SearchFamily, Long>, JpaSpecificationExecutor<SearchFamily> {

    long countByClientIpAddressAndCreateDateBetween(String clientIpAddress, OffsetDateTime createDateFrom, OffsetDateTime createDateTo);

    static PredicateSpecification<SearchFamily> isMatch(@Nullable Boolean isMatch) {
        return evaluateIsTrue("isMatch", isMatch);
    }

    static PredicateSpecification<SearchFamily> isReviewed(@Nullable Boolean isReviewed) {
        return evaluateIsTrue("isReviewed", isReviewed);
    }

    static PredicateSpecification<SearchFamily> isIgnored(@Nullable Boolean isIgnored) {
        return evaluateIsTrue("isIgnored", isIgnored);
    }

    static PredicateSpecification<SearchFamily> hasContact(@Nullable Boolean hasContact) {
        return (searchFamily, cb) -> hasContact == null
                ? cb.conjunction()
                : (hasContact
                        ? cb.isNotNull(searchFamily.get("contact"))
                        : cb.isNull(searchFamily.get("contact")));
    }

    static <T> PredicateSpecification<T> evaluateIsTrue(String field, Boolean value) {
        return (root, cb) -> {
            if (value == null) {
                return cb.conjunction();
            }
            return value
                    // matches true values
                    ? cb.isTrue(root.get(field))
                    // matches false and null values
                    : cb.or(cb.isNull(root.get(field)), cb.isFalse(root.get(field)));
        };
    }

    @Query("""
      SELECT
          s.clientIpAddress AS clientIpAddress,
          COUNT(*) AS count,
          MIN(s.createDate) AS minCreateDate,
          MAX(s.createDate) AS maxCreateDate,
          ARRAY_AGG(s.individual.surname) AS individualSurnames
      FROM SearchFamily s
      GROUP BY s.clientIpAddress
    """)
    List<SearchFamilyProjection> groupByClientIpAddress();

}
