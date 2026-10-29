package com.insightagent.agent;

import com.insightagent.data.QueryResult;

/**
 * @param sql     last successful SQL (null if none ran)
 * @param result  rows from that SQL, shown as a table in the UI
 */
public record AgentAnswer(String answer, String sql, QueryResult result,
                          int queries, int failures, long millis) {
}
