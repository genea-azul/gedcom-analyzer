package com.geneaazul.gedcomanalyzer.repository.projection;

import java.util.List;

public interface SearchConnectionProjection {

    String getClientIpAddress();

    Long getCount();

    List<String> getPerson1Surnames();

    List<String> getPerson2Surnames();

}
