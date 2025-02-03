package com.geneaazul.gedcomanalyzer.repository;

import com.geneaazul.gedcomanalyzer.domain.SearchConnection;
import com.geneaazul.gedcomanalyzer.repository.projection.SearchConnectionProjection;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Repository
public interface SearchConnectionRepository extends JpaRepository<SearchConnection, Long>, JpaSpecificationExecutor<SearchConnection> {

    long countByClientIpAddressAndCreateDateBetween(String clientIpAddress, OffsetDateTime createDateFrom, OffsetDateTime createDateTo);

    static Specification<SearchConnection> isMatch(@Nullable Boolean isMatch) {
        return (searchConnection, cq, cb) -> evaluateIsTrue(searchConnection, cb, "isMatch", isMatch);
    }

    static Specification<SearchConnection> isReviewed(@Nullable Boolean isReviewed) {
        return (searchConnection, cq, cb) -> evaluateIsTrue(searchConnection, cb, "isReviewed", isReviewed);
    }

    static Specification<SearchConnection> hasContact(@Nullable Boolean hasContact) {
        return (searchConnection, cq, cb) -> hasContact == null
                ? cb.conjunction()
                : (hasContact
                        ? cb.isNotNull(searchConnection.get("contact"))
                        : cb.isNull(searchConnection.get("contact")));
    }

    static <T> Predicate evaluateIsTrue(
            Root<T> root,
            CriteriaBuilder cb,
            String field,
            Boolean value) {
        if (value == null) {
            return cb.conjunction();
        }
        return value
                // matches true values
                ? cb.isTrue(root.get(field))
                // matches false and null values
                : cb.or(cb.isNull(root.get(field)), cb.isFalse(root.get(field)));
    }

    @Query("""
      SELECT
          s.clientIpAddress AS clientIpAddress,
          COUNT(*) AS count,
          ARRAY_AGG(s.person1.surname) AS person1Surnames,
          ARRAY_AGG(s.person2.surname) AS person2Surnames
      FROM SearchConnection s
      GROUP BY s.clientIpAddress
    """)
    List<SearchConnectionProjection> groupByClientIpAddress();

}
