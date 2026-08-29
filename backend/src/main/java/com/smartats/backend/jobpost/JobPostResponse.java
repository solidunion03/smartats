package com.smartats.backend.jobpost;

import java.time.OffsetDateTime;

public record JobPostResponse(String ticket, String title, OffsetDateTime createdAt) {
}