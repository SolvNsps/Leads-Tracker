package com.leadstracker.leadstracker.response;

public enum Statuses {
    PENDING,
    INTERESTED,
    NOT_INTERESTED,
    ONBOARDED,
    AWAITING_DOCUMENTATION;

    public static Statuses fromString(String status) {
        for (Statuses s : values()) {
            if (s.name().equalsIgnoreCase(status)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Invalid status: " + status);
    }
}
