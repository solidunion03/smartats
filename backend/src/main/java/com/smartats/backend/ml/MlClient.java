package com.smartats.backend.ml;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MlClient {

    private final RestClient restClient;

    public MlClient(@Value("${smartats.ml.base-url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public String ping() {
        return restClient.get()
                .uri("/ping")
                .retrieve()
                .body(String.class);
    }
    public MlScoreResponse score(String cvText, String jobText) {
        return restClient.post()
                .uri("/score")
                .body(new MlScoreRequest(cvText, jobText))
                .retrieve()
                .body(MlScoreResponse.class);
    }
}