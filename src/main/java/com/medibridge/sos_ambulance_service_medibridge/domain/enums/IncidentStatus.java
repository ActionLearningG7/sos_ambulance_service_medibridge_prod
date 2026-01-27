package com.medibridge.sos_ambulance_service_medibridge.domain.enums;

/**
 * Incident Status Enum
 * Represents the lifecycle states of an SOS incident
 */
public enum IncidentStatus {
    CREATED("Incident created"),
    SEARCHING("Searching for available ambulance"),
    ASSIGNED("Ambulance assigned to incident"),
    EN_ROUTE("Ambulance en route to patient"),
    ARRIVED("Ambulance arrived at pickup location"),
    PICKUP("Patient picked up"),
    COMPLETED("Incident completed"),
    CANCELLED("Incident cancelled");

    private final String description;

    IncidentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
