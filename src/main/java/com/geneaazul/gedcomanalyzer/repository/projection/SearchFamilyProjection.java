package com.geneaazul.gedcomanalyzer.repository.projection;

import java.util.List;

public interface SearchFamilyProjection {

    String getClientIpAddress();

    Long getCount();

    List<String> getIndividualSurnames();

}
