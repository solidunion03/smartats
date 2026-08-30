package com.smartats.backend.scan;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "scan")
public class Scan {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String ticket;

    @Column(name = "cv_id", nullable = false)
    private UUID cvId;

    @Column(name = "job_post_id", nullable = false)
    private UUID jobPostId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "score")
    private Integer score;

    protected Scan() {
    }

    public Scan(UUID id, String ticket, UUID cvId, UUID jobPostId, OffsetDateTime createdAt, Integer score) {
        this.id = id;
        this.ticket = ticket;
        this.cvId = cvId;
        this.jobPostId = jobPostId;
        this.createdAt = createdAt;
        this.score = score;
    }

    public UUID getId() { return id; }
    public String getTicket() { return ticket; }
    public UUID getCvId() { return cvId; }
    public UUID getJobPostId() { return jobPostId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public Integer getScore() { return score; }
}