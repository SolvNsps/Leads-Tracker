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

        // Normalize: uppercase, replace spaces with underscores
//        String normalized = status.trim().replace(" ", "_").toUpperCase();
        String normalized = status.trim().replaceAll("\\s+", "_").toUpperCase();

        for (Statuses s : values()) {
            // Match enum name
            if (s.name().equalsIgnoreCase(normalized)) {
                return s;
            }
            // Match display name ignoring spaces/underscores
//            String displayNormalized = s.getDisplayName().replace(" ", "_").toUpperCase();
            String displayNormalized = s.getDisplayName().trim().replaceAll("\\s+", "_").toUpperCase();
            if (displayNormalized.equals(normalized)) {
                return s;
            }
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
    }


//    public static Statuses fromString(String status) {
//
//        if (status == null || status.isBlank()) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client status is required.");
//        }
//
//        for (Statuses s : values()) {
//            if (s.name().equalsIgnoreCase(status) || s.getDisplayName().equalsIgnoreCase(status)) {
//                return s;
//            }
//        }
//        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
//    }
}
