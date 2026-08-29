package com.smartats.backend.scan;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smartats.backend.cv.CvDocument;
import com.smartats.backend.cv.CvNotFoundException;
import com.smartats.backend.cv.CvRepository;
import com.smartats.backend.jobpost.JobPost;
import com.smartats.backend.jobpost.JobPostNotFoundException;
import com.smartats.backend.jobpost.JobPostRepository;

@Service
public class ScanService {

    private final ScanRepository scanRepository;
    private final CvRepository cvRepository;
    private final JobPostRepository jobPostRepository;

    public ScanService(ScanRepository scanRepository,
                       CvRepository cvRepository,
                       JobPostRepository jobPostRepository) {
        this.scanRepository = scanRepository;
        this.cvRepository = cvRepository;
        this.jobPostRepository = jobPostRepository;
    }

    public ScanResponse create(ScanRequest request) {
        // 1. retrouver le CV par son ticket (ou 404)
        CvDocument cv = cvRepository.findByTicket(request.cvTicket())
                .orElseThrow(() -> new CvNotFoundException(request.cvTicket()));

        // 2. retrouver l'offre par son ticket (ou 404)
        JobPost jobPost = jobPostRepository.findByTicket(request.jobPostTicket())
                .orElseThrow(() -> new JobPostNotFoundException(request.jobPostTicket()));

        // 3. créer le scan qui relie leurs deux id internes
        UUID id = UUID.randomUUID();
        String ticket = id.toString();
        OffsetDateTime now = OffsetDateTime.now();

        Scan scan = new Scan(id, ticket, cv.getId(), jobPost.getId(), now);
        scanRepository.save(scan);

        return new ScanResponse(ticket, now);
    }
    public ScanResponse getByTicket(String ticket) {
        Scan scan = scanRepository.findByTicket(ticket)
                .orElseThrow(() -> new ScanNotFoundException(ticket));
        return new ScanResponse(scan.getTicket(), scan.getCreatedAt());
    }
}