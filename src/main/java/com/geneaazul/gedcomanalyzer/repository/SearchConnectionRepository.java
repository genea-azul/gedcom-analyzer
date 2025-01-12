package com.geneaazul.gedcomanalyzer.repository;

import com.geneaazul.gedcomanalyzer.domain.SearchConnection;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

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

}
