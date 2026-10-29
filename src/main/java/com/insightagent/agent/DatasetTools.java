package com.insightagent.agent;

import com.insightagent.data.DatasetService;
import com.insightagent.data.DuckDbService;
import com.insightagent.data.QueryResult;
import com.insightagent.data.SqlGuard;
import com.insightagent.domain.Dataset;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.function.Consumer;

/**
 * Tools the LLM can call. One instance per question, so it is bound to a single
 * dataset and can record what happened (steps, last SQL, last result).
 *
 * Errors are returned as text instead of thrown: that is what lets the model
 * read the error, fix its SQL and try again (the self-correction loop).
 */
public class DatasetTools {

    static final int MAX_QUERIES = 5;

    private final Dataset dataset;
    private final DatasetService datasets;
    private final DuckDbService duck;
    private final SqlGuard guard;
    private final Consumer<String> steps;
    private final int maxRows;

    private int queries = 0;
    private int failures = 0;
    private String lastSql;
    private QueryResult lastResult;

    public DatasetTools(Dataset dataset, DatasetService datasets, DuckDbService duck,
                        SqlGuard guard, Consumer<String> steps, int maxRows) {
        this.dataset = dataset;
        this.datasets = datasets;
        this.duck = duck;
        this.guard = guard;
        this.steps = steps;
        this.maxRows = maxRows;
    }

    @Tool(description = "Get 5 sample rows from the dataset table to understand what the values look like")
    public String getSampleRows() {
        steps.accept("Looking at sample rows");
        try {
            return datasets.preview(dataset, 5).toMarkdown(5);
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Tool(description = "Run ONE read-only DuckDB SQL SELECT query against the dataset table and get the result rows")
    public String runQuery(@ToolParam(description = "A single DuckDB SQL SELECT statement") String sql) {
        if (++queries > MAX_QUERIES) {
            steps.accept("Query limit reached, writing answer");
            return "Query limit reached. Answer using the results you already have.";
        }
        steps.accept("Running SQL:\n" + sql);
        try {
            String safeSql = guard.check(sql);
            QueryResult result = duck.query(datasets.path(dataset), safeSql, maxRows);
            lastSql = safeSql;
            lastResult = result;
            steps.accept("Got " + result.rows().size() + (result.truncated() ? "+" : "") + " rows");
            return result.toMarkdown(50);
        } catch (Exception e) {
            failures++;
            String msg = firstLine(e.getMessage());
            steps.accept("Query failed, self-correcting: " + msg);
            return "ERROR: " + msg + "\nRead the error, fix the SQL and call runQuery again.";
        }
    }

    public String lastSql() { return lastSql; }
    public QueryResult lastResult() { return lastResult; }
    public int queries() { return Math.min(queries, MAX_QUERIES); }
    public int failures() { return failures; }

    private static String firstLine(String s) {
        if (s == null) return "unknown error";
        int nl = s.indexOf('\n');
        return nl > 0 ? s.substring(0, nl) : s;
    }
}
