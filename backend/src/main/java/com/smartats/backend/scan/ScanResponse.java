package com.smartats.backend.scan;

import java.time.OffsetDateTime;
import java.util.List;

public record ScanResponse(
        String ticket,
        OffsetDateTime createdAt,
        Integer score,
        Integer skillScore,
        Integer semanticScore,
        List<String> matchedSkills,
        List<String> missingSkills,
        Integer experienceScore,
        Integer requiredYears,
        Double candidateYears
) {
}