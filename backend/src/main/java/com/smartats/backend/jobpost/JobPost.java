package com.smartats.backend.jobpost;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_post")
public class JobPost {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String ticket;

    @Column(nullable = false)
    private String title;

    @Column(name = "raw_text", nullable = false)
    private String rawText;

    @Column(nullable = true)
    private String language;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected JobPost() {
    }

    public JobPost(UUID id, String ticket, String title, String rawText, String language, OffsetDateTime createdAt) {
        this.id = id;
        this.ticket = ticket;
        this.title = title;
        this.rawText = rawText;
        this.language = language;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getTicket() { return ticket; }
    public String getTitle() { return title; }
    public String getRawText() { return rawText; }
    public String getLanguage() { return language; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}