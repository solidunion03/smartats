package com.smartats.backend.cv;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CvRepository extends JpaRepository<CvDocument, UUID> {
    Optional<CvDocument> findByTicket(String ticket);
}