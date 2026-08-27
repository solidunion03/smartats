package com.smartats.backend.cv;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class CvController {

    private final CvService cvService;

    public CvController(CvService cvService) {
        this.cvService = cvService;
    }

    @PostMapping("/cv")
    public CvUploadResponse upload(@RequestParam("file") MultipartFile file) {
        return cvService.receive(file);
    }
    @GetMapping("/cv/{ticket}")
    public CvUploadResponse get(@PathVariable String ticket) {
        return cvService.getByTicket(ticket);
    }
}