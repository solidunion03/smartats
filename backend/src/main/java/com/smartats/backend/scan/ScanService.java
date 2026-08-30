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
import com.smartats.backend.ml.MlClient;
import com.smartats.backend.ml.MlScoreResponse;


@Service
public class ScanService {

    private final ScanRepository scanRepository;
    private final CvRepository cvRepository;
    private final JobPostRepository jobPostRepository;
    private final MlClient mlClient;

    public ScanService(ScanRepository scanRepository,
                       CvRepository cvRepository,
                       JobPostRepository jobPostRepository, MlClient mlClient) {
        this.scanRepository = scanRepository;
        this.cvRepository = cvRepository;
        this.jobPostRepository = jobPostRepository;
        this.mlClient = mlClient;
    }
    public ScanResponse getByTicket(String ticket) {
        Scan scan = scanRepository.findByTicket(ticket)
                .orElseThrow(() -> new ScanNotFoundException(ticket));
        return new ScanResponse(scan.getTicket(), scan.getCreatedAt());
    }
    public ScanResponse create(ScanRequest request) {
        CvDocument cv = cvRepository.findByTicket(request.cvTicket())
                .orElseThrow(() -> new CvNotFoundException(request.cvTicket()));

        JobPost jobPost = jobPostRepository.findByTicket(request.jobPostTicket())
                .orElseThrow(() -> new JobPostNotFoundException(request.jobPostTicket()));

        // NOUVEAU : demander un score au service Python
        // (texte CV provisoire — le parsing du PDF viendra après)
        String cvText = "cv text placeholder";
        String jobText = jobPost.getRawText();
        MlScoreResponse mlResponse = mlClient.score(cvText, jobText);

        UUID id = UUID.randomUUID();
        String ticket = id.toString();
        OffsetDateTime now = OffsetDateTime.now();

        Scan scan = new Scan(id, ticket, cv.getId(), jobPost.getId(), now, mlResponse.score());
        scanRepository.save(scan);

        return new ScanResponse(ticket, now);
    }
}