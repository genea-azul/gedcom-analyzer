package com.geneaazul.gedcomanalyzer.repository;

import com.geneaazul.gedcomanalyzer.domain.SearchFamily;
import jakarta.annotation.Nullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public interface SearchFamilyRepository extends JpaRepository<SearchFamily, Long>, JpaSpecificationExecutor<SearchFamily> {

    long countByClientIpAddressAndCreateDateBetween(String clientIpAddress, OffsetDateTime createDateFrom, OffsetDateTime createDateTo);

    static Specification<SearchFamily> isMatch(@Nullable Boolean isMatch) {
        return (searchFamily, cq, cb) -> isMatch == null
                ? cb.conjunction()
                : cb.equal(searchFamily.get("isMatch"), isMatch);
    }

    static Specification<SearchFamily> hasContact(@Nullable Boolean hasContact) {
        return (searchFamily, cq, cb) -> hasContact == null
                ? cb.conjunction()
                : (hasContact
                        ? cb.isNotNull(searchFamily.get("contact"))
                        : cb.isNull(searchFamily.get("contact")));
    }

}
