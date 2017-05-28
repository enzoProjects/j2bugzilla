package com.j2bugzilla.http;

import com.j2bugzilla.base.Bug;
import com.j2bugzilla.base.BugFactory;
import com.j2bugzilla.base.BugzillaHttpParser;
import com.j2bugzilla.base.HttpBug;
import org.apache.commons.lang3.text.WordUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * Created by springfield-home on 5/28/17.
 */
public class BugSearchParser implements BugzillaHttpParser {
    public static final String START = "buglistSorter";
    public static final String END = "</table>";
    public static final String PATH = "buglist.cgi";
    public static final char[] MINUS = new char[]{'_'};
    public static final String UNDERSCORE = "_";
    public static final String JOIN_ESPACE = "";
    public static final int OFFSET_BZ = 3;
    public static final int OFFSET_COLUMN = 7;
    public final List<HttpBug> listaBugs = new LinkedList();

    private final Map<Object, Object> params = new HashMap<Object, Object>();

    public enum SearchColumn {
        CREATION_DATE("creation_ts"),
        LAST_UPDATE("delta_ts"),
        SEVERITY("bug_severity"),
        PRIORITY("priority"),
        STATUS("bug_status"),
        CASE_COUNT("case_count"),
        ASSIGNED("assigned_to"),
        NEED_INFO("needinfo"),
        REPORTER("reporter"),
        CATEGORY("category"),
        COMPONENT("component"),
        FIX_BY("fix_by"),
        BRANCH("cf_branch"),
        VISS("cf_viss"),
        SUMMARY("short_desc");

        private final String name;

        /**
         * Creates a new {@link BugSearchParser.SearchColumn} with the
         * designated name
         *
         * @param name The name Bugzilla expects for this search limiter
         */
        SearchColumn(String name) {
            this.name = name;
        }

        /**
         * Get the name Bugzilla expects for this search limiter
         *
         * @return A <code>String</code> representing the search limiter
         */
        String getName() {
            return this.name;
        }
    }

    public enum SearchLimiter {

        /**
         * The person who the bug is assigned to.
         */
        ASSIGNED_TO("email1"),

        /**
         * Type of the email default should be exact.
         */
        EMAIL_TYPE("emailtype1"),

        /**
         * This should be on 1 if you want the assigned from assigned.
         */
        EMAIL_ASSIGNED("emailassigned_to1"),

        /**
         * This should be on 1 if you want the need info from assigned.
         */
        EMAIL_NEED_INFO("emailneedinfo1"),

        /**
         * Status of the bug ex: open.
         */
        BUG_STATUS("bug_status"),

        /**
         * Properties to get from bug search
         */
        BUG_PROPERTIES("columnlist"),


        QUERY_FORMAT("query_format");


        private final String name;

        /**
         * Creates a new {@link BugSearchParser.SearchLimiter} with the
         * designated name
         *
         * @param name The name Bugzilla expects for this search limiter
         */
        SearchLimiter(String name) {
            this.name = name;
        }

        /**
         * Get the name Bugzilla expects for this search limiter
         *
         * @return A <code>String</code> representing the search limiter
         */
        String getName() {
            return this.name;
        }
    }

    public BugSearchParser(SearchQuery... queries) {
        params.put(SearchLimiter.QUERY_FORMAT.getName(), "advanced");
        if (queries.length == 0) {
            throw new IllegalArgumentException("At least one search query is required");
        }

        for (BugSearchParser.SearchQuery query : queries) {
            params.put(query.getLimiter().getName(), query.getQuery());
        }
    }

    public void setSearchColumns(BugSearchParser.SearchColumn... columns) {
        if (columns.length == 0) {
            throw new IllegalArgumentException("At least one search column is required");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.length - 1; i++) {
            sb.append(columns[i].getName() + ",");
        }
        sb.append(columns[columns.length - 1].getName());
        params.put(SearchLimiter.BUG_PROPERTIES.getName(), sb.toString());
    }

    @Override
    public String getStartOfParse() {
        return START;
    }

    @Override
    public String getEndOfParse() {
        return END;
    }

    public Map<Object, Object> getParameters() {
        return params;
    }

    @Override
    public String getExtraPath() {
        return PATH;
    }

    @Override
    public void parse(Document doc) {
        Elements columns = doc.select("col");
        String[] columnsName = new String[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            String columnText = columns.get(i).className();
            columnText = columnText.substring(OFFSET_BZ, columnText.length() - OFFSET_COLUMN);
            columnsName[i] = WordUtils.capitalizeFully(columnText, MINUS).replaceAll(UNDERSCORE, JOIN_ESPACE);
        }
        Elements bugs = doc.select("tr");
        for (int i = 1; i < bugs.size(); i++) {
            Map<String, Object> bugMap = new LinkedHashMap();
            Elements rows = bugs.get(i).select("td");
            for (int j = 0; j < columns.size(); j++) {
                Element row = rows.get(j);
                if (row.children().size() > 0 && row.child(0).is("span")) {
                    bugMap.put(columnsName[j], row.child(0).attr("title"));
                } else {
                    bugMap.put(columnsName[j], row.text());
                }

            }
            listaBugs.add(new BugFactory().createHttpBug(bugMap));
        }
    }

    @Override
    public List<HttpBug> getResults() {
        return listaBugs;
    }


    /**
     * The {@code SearchQuery} class encapsulates a query against the bug collection on a given
     * Bugzilla database. It consists of a limiter to apply and the value for that limiter. For
     * example, a valid {@code SearchQuery} might consist of the limiter
     * {@link BugSearchParser.SearchLimiter# "Product"} and the query {@code "J2Bugzilla"}.
     * <p>
     * When a {@code SearchQuery} is applied within the {@link BugSearchParser} class, it is joined with the
     * other queries in a logical AND. That is, bugs will be returned that match all the criteria, not
     * any of them.
     *
     * @author Tom
     */
    public static class SearchQuery {

        private final BugSearchParser.SearchLimiter limiter;

        private final String query;

        /**
         * Creates a new {@link BugSearchParser.SearchQuery} to filter the bug database through.
         *
         * @param limiter A {@link BugSearchParser.SearchLimiter} enum.
         * @param query   A {@code String} to filter with. The whole string will be matched, or not at all --
         *                Bugzilla does not perform substring matching.
         */
        public SearchQuery(BugSearchParser.SearchLimiter limiter, String query) {
            this.limiter = limiter;
            this.query = query;
        }

        /**
         * Returns the {@link BugSearchParser.SearchLimiter} to apply to this query.
         *
         * @return A facet of a bug to search against.
         */
        public BugSearchParser.SearchLimiter getLimiter() {
            return limiter;
        }

        /**
         * Returns the value of the specified query.
         *
         * @return A {@code String} to query for within the specified limiter.
         */
        public String getQuery() {
            return query;
        }
    }

}

