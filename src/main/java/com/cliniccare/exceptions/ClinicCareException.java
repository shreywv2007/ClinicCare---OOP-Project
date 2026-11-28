package com.cliniccare.exceptions;

/** Base checked exception for application errors. */
public class ClinicCareException extends Exception {
    public ClinicCareException(String message) {
        super(message);
    }

    public ClinicCareException(String message, Throwable cause) {
        super(message, cause);
    }
}
