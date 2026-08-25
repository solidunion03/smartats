package com.smartats.backend.cv;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CvService {

    public CvUploadResponse receive(MultipartFile file) {
        String ticket = UUID.randomUUID().toString();
        return new CvUploadResponse(ticket, file.getOriginalFilename(), file.getSize());
    }
}