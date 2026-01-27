package com.medibridge.sos_ambulance_service_medibridge.domain.enums;

/**
 * Assignment Status Enum
 * Represents the lifecycle states of a driver assignment
 */
public enum AssignmentStatus {
    ASSIGNED("Driver assigned, awaiting acceptance"),
    ACCEPTED("Driver accepted the assignment"),
    EN_ROUTE("Driver en route to patient"),
    ARRIVED("Driver arrived at patient location"),
    PICKED_UP("Patient picked up"),
    COMPLETED("Assignment completed"),
    CANCELLED("Assignment cancelled"),
    FAILED("Assignment failed");

    private final String description;

    AssignmentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
