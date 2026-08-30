package com.smartats.backend.ml;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MlPingController {

    private final MlClient mlClient;

    public MlPingController(MlClient mlClient) {
        this.mlClient = mlClient;
    }

    @GetMapping("/ml-ping")
    public String mlPing() {
        return mlClient.ping();
    }
    
}