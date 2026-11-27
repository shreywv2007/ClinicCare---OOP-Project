package com.cliniccare.model;

/**
 * Consultation types and their fee multiplier.
 *
 * PROPOSED fee rule (must be agreed with the team / Member 5):
 *   charge = doctor's consultation_fee x feeMultiplier
 * If the team decides on a fixed fee only, keep GENERAL (1.0) and delete the others.
 */
public enum ConsultationType {
    GENERAL(1.0),
    FOLLOW_UP(0.5),
    EMERGENCY(1.5);

    private final double feeMultiplier;

    ConsultationType(double feeMultiplier) {
        this.feeMultiplier = feeMultiplier;
    }

    public double getFeeMultiplier() {
        return feeMultiplier;
    }

    public double calculateFee(double doctorFee) {
        return doctorFee * feeMultiplier;
    }
}
