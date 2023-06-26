package com.geneaazul.gedcomanalyzer.repository;

import com.geneaazul.gedcomanalyzer.domain.SearchFamily;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public interface SearchFamilyRepository extends JpaRepository<SearchFamily, Long>, JpaSpecificationExecutor<SearchFamily> {

    long countByClientIpAddressAndCreateDateBetween(String clientIpAddress, OffsetDateTime createDateFrom, OffsetDateTime createDateTo);

    static Specification<SearchFamily> isMatch(@Nullable Boolean isMatch) {
        return (searchFamily, cq, cb) -> evaluateIsTrue(searchFamily, cb, "isMatch", isMatch);
    }

    static Specification<SearchFamily> isReviewed(@Nullable Boolean isReviewed) {
        return (searchFamily, cq, cb) -> evaluateIsTrue(searchFamily, cb, "isReviewed", isReviewed);
    }

    static Specification<SearchFamily> hasContact(@Nullable Boolean hasContact) {
        return (searchFamily, cq, cb) -> hasContact == null
                ? cb.conjunction()
                : (hasContact
                        ? cb.isNotNull(searchFamily.get("contact"))
                        : cb.isNull(searchFamily.get("contact")));
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
