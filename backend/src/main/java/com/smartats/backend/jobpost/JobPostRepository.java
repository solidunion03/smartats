package com.smartats.backend.jobpost;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostRepository extends JpaRepository<JobPost, UUID> {
    Optional<JobPost> findByTicket(String ticket);
}