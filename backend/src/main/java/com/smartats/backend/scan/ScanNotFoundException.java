package com.smartats.backend.scan;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ScanNotFoundException extends RuntimeException {
    public ScanNotFoundException(String ticket) {
        super("Aucun scan trouvé pour le ticket : " + ticket);
    }
}