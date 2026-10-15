package com.smartats.backend.ml;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MlScoreResponse(
        int score,
        @JsonProperty("skill_score") int skillScore,
        @JsonProperty("semantic_score") int semanticScore,
        @JsonProperty("matched_skills") List<String> matchedSkills,
        @JsonProperty("missing_skills") List<String> missingSkills,
        @JsonProperty("experience_score") Integer experienceScore,
        @JsonProperty("required_years") Integer requiredYears,
        @JsonProperty("candidate_years") Double candidateYears
) {
}
