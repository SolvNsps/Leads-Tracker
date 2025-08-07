package com.leadstracker.leadstracker.response;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum Statuses {
    PENDING("Pending"),
    INTERESTED("Interested"),
    NOT_INTERESTED("Not Interested"),
    ONBOARDED("Onboarded"),
    AWAITING_DOCUMENTATION("Awaiting Documentation");

    private final String displayName;

    Statuses(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Statuses fromString(String status) {

        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client status is required.");
        }

        for (Statuses s : values()) {
            if (s.name().equalsIgnoreCase(status) || s.getDisplayName().equalsIgnoreCase(status)) {
                return s;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
    }
}
