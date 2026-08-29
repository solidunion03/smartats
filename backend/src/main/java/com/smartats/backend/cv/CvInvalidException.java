package com.smartats.backend.cv;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CvInvalidException extends RuntimeException {
    public CvInvalidException(String message) {
        super(message);
    }
}