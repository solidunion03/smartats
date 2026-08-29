package com.smartats.backend.jobpost;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class JobPostService {

    private final JobPostRepository jobPostRepository;

    public JobPostService(JobPostRepository jobPostRepository) {
        this.jobPostRepository = jobPostRepository;
    }

    public JobPostResponse create(JobPostRequest request) {
        UUID id = UUID.randomUUID();
        String ticket = id.toString();
        OffsetDateTime now = OffsetDateTime.now();   // calculée UNE fois

        JobPost jobPost = new JobPost(
                id,
                ticket,
                request.title(),
                request.rawText(),
                null,          // language : rempli plus tard
                now
        );

        jobPostRepository.save(jobPost);

        return new JobPostResponse(ticket, request.title(), now);
    }
    public JobPostResponse getByTicket(String ticket) {
        JobPost jobPost = jobPostRepository.findByTicket(ticket)
                .orElseThrow(() -> new JobPostNotFoundException(ticket));
        return new JobPostResponse(jobPost.getTicket(), jobPost.getTitle(), jobPost.getCreatedAt());
    }
}