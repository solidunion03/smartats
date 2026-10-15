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

    @Column(name = "skill_score")
    private Integer skillScore;

    @Column(name = "semantic_score")
    private Integer semanticScore;

    @Column(name = "matched_skills")
    private String matchedSkills;

    @Column(name = "missing_skills")
    private String missingSkills;

    @Column(name = "experience_score")
    private Integer experienceScore;

    @Column(name = "required_years")
    private Integer requiredYears;

    @Column(name = "candidate_years")
    private Double candidateYears;

    protected Scan() {
    }

    public Scan(UUID id, String ticket, UUID cvId, UUID jobPostId, OffsetDateTime createdAt,
                Integer score, Integer skillScore, Integer semanticScore,
                String matchedSkills, String missingSkills,
                Integer experienceScore, Integer requiredYears, Double candidateYears) {
        this.id = id;
        this.ticket = ticket;
        this.cvId = cvId;
        this.jobPostId = jobPostId;
        this.createdAt = createdAt;
        this.score = score;
        this.skillScore = skillScore;
        this.semanticScore = semanticScore;
        this.matchedSkills = matchedSkills;
        this.missingSkills = missingSkills;
        this.experienceScore = experienceScore;
        this.requiredYears = requiredYears;
        this.candidateYears = candidateYears;
    }

    public UUID getId() { return id; }
    public String getTicket() { return ticket; }
    public UUID getCvId() { return cvId; }
    public UUID getJobPostId() { return jobPostId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public Integer getScore() { return score; }
    public Integer getSkillScore() { return skillScore; }
    public Integer getSemanticScore() { return semanticScore; }
    public String getMatchedSkills() { return matchedSkills; }
    public String getMissingSkills() { return missingSkills; }
    public Integer getExperienceScore() { return experienceScore; }
    public Integer getRequiredYears() { return requiredYears; }
    public Double getCandidateYears() { return candidateYears; }
}