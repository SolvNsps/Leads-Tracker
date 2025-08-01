package com.leadstracker.leadstracker.response;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum Statuses {
    PENDING,
    INTERESTED,
    NOT_INTERESTED,
    ONBOARDED,
    AWAITING_DOCUMENTATION;

    public static Statuses fromString(String status) {

        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client status is required.");
        }

        for (Statuses s : values()) {
            if (s.name().equalsIgnoreCase(status)) {
                return s;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
    }
}
