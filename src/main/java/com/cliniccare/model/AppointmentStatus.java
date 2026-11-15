package com.cliniccare.model;

/**
 * Allowed appointment statuses (fixed by the shared contract).
 */
public enum AppointmentStatus {
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    /**
     * Status workflow rule:
     * CONFIRMED -> IN_PROGRESS or CANCELLED
     * IN_PROGRESS -> COMPLETED or CANCELLED
     * COMPLETED and CANCELLED are final.
     */
    public boolean canMoveTo(AppointmentStatus next) {
        switch (this) {
            case CONFIRMED:
                return next == IN_PROGRESS || next == CANCELLED;
            case IN_PROGRESS:
                return next == COMPLETED || next == CANCELLED;
            default:
                return false;
        }
    }
}
