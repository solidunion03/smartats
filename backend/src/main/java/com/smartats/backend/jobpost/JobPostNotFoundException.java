package com.smartats.backend.jobpost;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class JobPostNotFoundException extends RuntimeException {
    public JobPostNotFoundException(String ticket) {
        super("Aucune offre trouvée pour le ticket : " + ticket);
    }
}