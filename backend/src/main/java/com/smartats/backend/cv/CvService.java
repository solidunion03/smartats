package com.smartats.backend.cv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CvService {

    private final CvRepository cvRepository;
    private final Path storageDir;

    public CvService(CvRepository cvRepository,
                     @Value("${smartats.storage.dir}") String storageDir) {
        this.cvRepository = cvRepository;
        this.storageDir = Path.of(storageDir);
    }

    public CvUploadResponse receive(MultipartFile file) {
        try {
            UUID id = UUID.randomUUID();
            String ticket = id.toString();

            Files.createDirectories(storageDir);
            Path target = storageDir.resolve(ticket + "-" + file.getOriginalFilename());
            file.transferTo(target);

            CvDocument doc = new CvDocument(
                    id,
                    ticket,
                    file.getOriginalFilename(),
                    file.getSize(),
                    target.toString(),
                    OffsetDateTime.now()
            );
            cvRepository.save(doc);

            return new CvUploadResponse(ticket, file.getOriginalFilename(), file.getSize());
        } catch (IOException e) {
            throw new RuntimeException("Échec du stockage du CV", e);
        }
    }
    public CvUploadResponse getByTicket(String ticket) {
        CvDocument doc = cvRepository.findByTicket(ticket)
                .orElseThrow(() -> new CvNotFoundException(ticket));
        return new CvUploadResponse(doc.getTicket(), doc.getFilename(), doc.getSizeBytes());
    }
}