package com.j2bugzilla.base;

import org.jsoup.nodes.Document;

import java.util.Map;

/**
 * Created by springfield-home on 5/28/17.
 */
public interface BugzillaHttpParser {
    String getStartOfParse();

    String getEndOfParse();

    void getPropertiesForParse();

    Object getResults();

    Map<Object, Object> getParameters();

    String getExtraPath();

    void parse(Document doc);
}
