package com.geneaazul.gedcomanalyzer.repository;

import com.geneaazul.gedcomanalyzer.domain.SearchConnection;
import com.geneaazul.gedcomanalyzer.repository.projection.SearchConnectionProjection;

import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.annotation.Nullable;

@Repository
public interface SearchConnectionRepository extends JpaRepository<SearchConnection, Long>, JpaSpecificationExecutor<SearchConnection> {

    long countByClientIpAddressAndCreateDateBetween(String clientIpAddress, OffsetDateTime createDateFrom, OffsetDateTime createDateTo);

    static PredicateSpecification<SearchConnection> isMatch(@Nullable Boolean isMatch) {
        return evaluateIsTrue("isMatch", isMatch);
    }

    static PredicateSpecification<SearchConnection> isReviewed(@Nullable Boolean isReviewed) {
        return evaluateIsTrue("isReviewed", isReviewed);
    }

    static PredicateSpecification<SearchConnection> hasContact(@Nullable Boolean hasContact) {
        return (root, cb) -> hasContact == null
                ? cb.conjunction()
                : (hasContact
                        ? cb.isNotNull(root.get("contact"))
                        : cb.isNull(root.get("contact")));
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
          ARRAY_AGG(s.person1.surname) AS person1Surnames,
          ARRAY_AGG(s.person2.surname) AS person2Surnames
      FROM SearchConnection s
      GROUP BY s.clientIpAddress
    """)
    List<SearchConnectionProjection> groupByClientIpAddress();

}
