package com.cliniccare.exceptions;

public class AppointmentConflictException extends ClinicCareException {
    public AppointmentConflictException(String message) {
        super(message);
    }

    public AppointmentConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
