package com.smartats.backend.scan;

import java.time.OffsetDateTime;

public record ScanResponse(String ticket, OffsetDateTime createdAt) {
}