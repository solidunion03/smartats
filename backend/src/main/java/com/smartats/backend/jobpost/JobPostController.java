package com.smartats.backend.jobpost;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class JobPostController { 

    private final JobPostService jobPostService;

    public JobPostController(JobPostService jobPostService) {
        this.jobPostService = jobPostService;
    }

    @PostMapping("/job-post")
    public JobPostResponse submit(@RequestBody JobPostRequest request) {
        return jobPostService.create(request);
    }
    @GetMapping("/job-post/{ticket}")
    public JobPostResponse get(@PathVariable String ticket) {
        return jobPostService.getByTicket(ticket);
    }
}