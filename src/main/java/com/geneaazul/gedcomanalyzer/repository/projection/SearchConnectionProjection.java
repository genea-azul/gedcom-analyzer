package com.geneaazul.gedcomanalyzer.repository.projection;

import java.time.OffsetDateTime;
import java.util.List;

public interface SearchConnectionProjection {

    String getClientIpAddress();

    Long getCount();

    OffsetDateTime getMinCreateDate();

    OffsetDateTime getMaxCreateDate();

    List<String> getPerson1Surnames();

    List<String> getPerson2Surnames();

}
