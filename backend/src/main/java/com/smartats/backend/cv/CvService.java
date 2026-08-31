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
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 Mo

    public CvService(CvRepository cvRepository,
                     @Value("${smartats.storage.dir}") String storageDir) {
        this.cvRepository = cvRepository;
        this.storageDir = Path.of(storageDir);
    }

        public CvUploadResponse receive(MultipartFile file) {
        validate(file);
        try {
            UUID id = UUID.randomUUID();
            String ticket = id.toString();

            Files.createDirectories(storageDir);
            Path target = storageDir.resolve(ticket + "-" + file.getOriginalFilename());
            file.transferTo(target);

            String extractedText = extractText(target);   

            CvDocument doc = new CvDocument(
                    id,
                    ticket,
                    file.getOriginalFilename(),
                    file.getSize(),
                    target.toString(),
                    OffsetDateTime.now(),
                    extractedText                          
            );
            cvRepository.save(doc);

            return new CvUploadResponse(ticket, file.getOriginalFilename(), file.getSize());
        } catch (IOException e) {
            throw new RuntimeException("Échec du stockage du CV", e);
        }
    }
    private String extractText(Path pdfPath) throws IOException {
        try (var document = org.apache.pdfbox.Loader.loadPDF(pdfPath.toFile())) {
            var stripper = new org.apache.pdfbox.text.PDFTextStripper();
            return stripper.getText(document);
        }
    }

    public CvUploadResponse getByTicket(String ticket) {
        CvDocument doc = cvRepository.findByTicket(ticket)
                .orElseThrow(() -> new CvNotFoundException(ticket));
        return new CvUploadResponse(doc.getTicket(), doc.getFilename(), doc.getSizeBytes());
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new CvInvalidException("Le fichier est vide.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new CvInvalidException("Le fichier dépasse la taille maximale de 5 Mo.");
        }
        String name = file.getOriginalFilename();
        if (name == null
                || !(name.toLowerCase().endsWith(".pdf") || name.toLowerCase().endsWith(".docx"))) {
            throw new CvInvalidException("Seuls les fichiers PDF ou DOCX sont acceptés.");
        }
        try {
            if (!hasValidSignature(file)) {
                throw new CvInvalidException("Le contenu du fichier n'est pas un PDF ou un DOCX valide.");
            }
        } catch (IOException e) {
            throw new CvInvalidException("Impossible de lire le contenu du fichier.");
        }
    }

    private boolean hasValidSignature(MultipartFile file) throws IOException {
        byte[] header = new byte[4];
        try (var in = file.getInputStream()) {
            int read = in.read(header);   // lit jusqu'à 4 octets, au tout début
            if (read < 4) {
                return false;             // trop court pour être un vrai PDF/DOCX
            }
        }

        // PDF : commence par les octets %PDF
        boolean isPdf = header[0] == '%' && header[1] == 'P'
                     && header[2] == 'D' && header[3] == 'F';

        // DOCX (archive ZIP) : commence par P K puis 0x03 0x04
        boolean isDocx = header[0] == 'P' && header[1] == 'K'
                      && header[2] == 0x03 && header[3] == 0x04;

        return isPdf || isDocx;
    }
}