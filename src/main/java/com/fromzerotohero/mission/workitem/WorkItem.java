package com.fromzerotohero.mission.workitem;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class WorkItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String title;
    @Column(length = 1000)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkStatus status;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    @Version
    @Column(nullable = false)
    private long version;
    @JsonIgnore
    @Column(nullable = false, updatable = false)
    private UUID tenantId;

    protected WorkItem() {}

    WorkItem(String title, String description, Priority priority, WorkStatus status) {
        this(title, description, priority, status,
                UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    public WorkItem(String title, String description, Priority priority, WorkStatus status, UUID tenantId) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.tenantId = tenantId;
    }

    @PrePersist
    void createTimestamps() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public WorkStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
    public UUID getTenantId() { return tenantId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public void setStatus(WorkStatus status) { this.status = status; }
}
