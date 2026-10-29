package com.insightagent.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class ChatMessage {

    public enum Role { USER, ASSISTANT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Dataset dataset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role;

    @Column(nullable = false, length = 10000)
    private String content;

    /** The SQL the agent ran for this answer, so it can be re-run from history. */
    @Column(length = 4000)
    private String sqlQuery;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected ChatMessage() {
    }

    public ChatMessage(Dataset dataset, Role role, String content, String sqlQuery) {
        this.dataset = dataset;
        this.role = role;
        this.content = truncate(content, 10000);
        this.sqlQuery = truncate(sqlQuery, 4000);
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    public Long getId() { return id; }
    public Dataset getDataset() { return dataset; }
    public Role getRole() { return role; }
    public String getContent() { return content; }
    public String getSqlQuery() { return sqlQuery; }
    public Instant getCreatedAt() { return createdAt; }
}
