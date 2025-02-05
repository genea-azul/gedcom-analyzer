package com.geneaazul.gedcomanalyzer.repository.projection;

import java.time.OffsetDateTime;
import java.util.List;

public interface SearchFamilyProjection {

    String getClientIpAddress();

    Long getCount();

    OffsetDateTime getMinCreateDate();

    OffsetDateTime getMaxCreateDate();

    List<String> getIndividualSurnames();

}
