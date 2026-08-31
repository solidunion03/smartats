package com.smartats.backend.cv;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cv_document")
public class CvDocument {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String ticket;

    @Column(nullable = false)
    private String filename;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "extracted_text")
    private String extractedText;

    protected CvDocument() {
    }

    public CvDocument(UUID id, String ticket, String filename, long sizeBytes, String storagePath, OffsetDateTime createdAt, String extractedText) {
        this.id = id;
        this.ticket = ticket;
        this.filename = filename;
        this.sizeBytes = sizeBytes;
        this.storagePath = storagePath;
        this.createdAt = createdAt;
        this.extractedText = extractedText;
    }

    public UUID getId() { return id; }
    public String getTicket() { return ticket; }
    public String getFilename() { return filename; }
    public long getSizeBytes() { return sizeBytes; }
    public String getStoragePath() { return storagePath; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getExtractedText() { return extractedText; }
}