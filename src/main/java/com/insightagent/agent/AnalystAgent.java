package com.insightagent.agent;

import com.insightagent.data.ColumnInfo;
import com.insightagent.data.DatasetService;
import com.insightagent.data.DuckDbService;
import com.insightagent.data.SqlGuard;
import com.insightagent.domain.AppUser;
import com.insightagent.domain.Dataset;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Week 2 agent: one LLM with tools + memory, running a plan -> query -> self-correct -> answer loop.
 * In Week 3 this becomes the SQL agent inside an orchestrator with Planner, Critic and Report agents.
 */
@Service
public class AnalystAgent {

    private static final String SYSTEM_PROMPT = """
            You are InsightAgent, a senior data analyst.
            You answer questions about ONE dataset stored in a DuckDB table named "%s".

            Schema (column: type):
            %s

            How to work:
            1. Think about which columns answer the question.
            2. Call runQuery with DuckDB SQL to get real numbers. Never guess or invent numbers.
            3. If a query returns ERROR, read it, fix the SQL and call runQuery again.
            4. Prefer aggregated queries (GROUP BY, ORDER BY, LIMIT) over pulling raw rows.
            5. Call getSampleRows if you are unsure what values look like.

            Final answer: 2 to 5 plain sentences with the key numbers and what they mean.
            Do not put SQL in the final answer. If the data cannot answer the question, say so.
            """;

    private final ChatClient chat;
    private final DatasetService datasets;
    private final DuckDbService duck;
    private final SqlGuard guard;
    private final int maxRows;

    public AnalystAgent(ChatClient.Builder builder,
                        DatasetService datasets,
                        DuckDbService duck,
                        SqlGuard guard,
                        @Value("${insightagent.max-rows:200}") int maxRows) {
        ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(20).build();
        this.chat = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();
        this.datasets = datasets;
        this.duck = duck;
        this.guard = guard;
        this.maxRows = maxRows;
    }

    public AgentAnswer ask(AppUser user, Dataset ds, String question, Consumer<String> steps) throws Exception {
        long start = System.currentTimeMillis();
        steps.accept("Reading schema of " + ds.getTableName());
        List<ColumnInfo> schema = datasets.schema(ds);
        String schemaText = schema.stream()
                .map(c -> "- " + c.name() + ": " + c.type())
                .collect(Collectors.joining("\n"));

        DatasetTools tools = new DatasetTools(ds, datasets, duck, guard, steps, maxRows);
        String conversationId = "user-" + user.getId() + "-dataset-" + ds.getId();

        steps.accept("Planning");
        String answer = chat.prompt()
                .system(SYSTEM_PROMPT.formatted(ds.getTableName(), schemaText))
                .user(question)
                .tools(tools)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        steps.accept("Done");
        return new AgentAnswer(answer, tools.lastSql(), tools.lastResult(),
                tools.queries(), tools.failures(), System.currentTimeMillis() - start);
    }
}
