package com.insightagent.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

/**
 * An uploaded file. The actual data lives in its own DuckDB file (storagePath),
 * in a single table called tableName.
 */
@Entity
public class Dataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private AppUser owner;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 64)
    private String tableName;

    @Column(nullable = false, length = 500)
    private String storagePath;

    private long rowCount;
    private int columnCount;

    @Column(nullable = false)
    private Instant uploadedAt = Instant.now();

    protected Dataset() {
    }

    public Dataset(AppUser owner, String name, String tableName, String storagePath, long rowCount, int columnCount) {
        this.owner = owner;
        this.name = name;
        this.tableName = tableName;
        this.storagePath = storagePath;
        this.rowCount = rowCount;
        this.columnCount = columnCount;
    }

    public Long getId() { return id; }
    public AppUser getOwner() { return owner; }
    public String getName() { return name; }
    public String getTableName() { return tableName; }
    public String getStoragePath() { return storagePath; }
    public long getRowCount() { return rowCount; }
    public int getColumnCount() { return columnCount; }
    public Instant getUploadedAt() { return uploadedAt; }

    // equality by id so Vaadin ComboBox can match freshly loaded entities
    @Override
    public boolean equals(Object o) {
        return o instanceof Dataset other && id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
