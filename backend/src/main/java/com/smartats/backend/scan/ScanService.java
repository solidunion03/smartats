package com.smartats.backend.scan;

import java.time.OffsetDateTime;
import java.util.List;
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
                       JobPostRepository jobPostRepository,
                       MlClient mlClient) {
        this.scanRepository = scanRepository;
        this.cvRepository = cvRepository;
        this.jobPostRepository = jobPostRepository;
        this.mlClient = mlClient;
    }

    public ScanResponse create(ScanRequest request) {
        CvDocument cv = cvRepository.findByTicket(request.cvTicket())
                .orElseThrow(() -> new CvNotFoundException(request.cvTicket()));

        JobPost jobPost = jobPostRepository.findByTicket(request.jobPostTicket())
                .orElseThrow(() -> new JobPostNotFoundException(request.jobPostTicket()));

        MlScoreResponse ml = mlClient.score(cv.getExtractedText(), jobPost.getRawText());

        Scan scan = new Scan(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                cv.getId(),
                jobPost.getId(),
                OffsetDateTime.now(),
                ml.score(),
                ml.skillScore(),
                ml.semanticScore(),
                String.join(",", ml.matchedSkills()),
                String.join(",", ml.missingSkills()),
                ml.experienceScore(),
                ml.requiredYears(),
                ml.candidateYears()
        );
        scanRepository.save(scan);

        return toResponse(scan);
    }

    public ScanResponse getByTicket(String ticket) {
        Scan scan = scanRepository.findByTicket(ticket)
                .orElseThrow(() -> new ScanNotFoundException(ticket));
        return toResponse(scan);
    }

    // Une seule façon de transformer un Scan en réponse : POST et GET renvoient la même chose
    private ScanResponse toResponse(Scan scan) {
        return new ScanResponse(
                scan.getTicket(),
                scan.getCreatedAt(),
                scan.getScore(),
                scan.getSkillScore(),
                scan.getSemanticScore(),
                splitCsv(scan.getMatchedSkills()),
                splitCsv(scan.getMissingSkills()),
                scan.getExperienceScore(),
                scan.getRequiredYears(),
                scan.getCandidateYears()
        );
    }

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return List.of(csv.split(","));
    }
}