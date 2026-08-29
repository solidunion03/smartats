package com.smartats.backend.scan;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScanController {

    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    @PostMapping("/scan")
    public ScanResponse create(@RequestBody ScanRequest request) {
        return scanService.create(request);
    }

    @GetMapping("/scan/{ticket}")
    public ScanResponse get(@PathVariable String ticket) {
        return scanService.getByTicket(ticket);
    }
}