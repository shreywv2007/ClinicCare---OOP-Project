package com.cliniccare.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Appointment model: plain data holder with private fields (encapsulation).
 * appointmentId == 0 means "not saved yet"; SQLite generates the real ID.
 */
public class Appointment {
    private int appointmentId;
    private int patientId;
    private int doctorId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private ConsultationType consultationType;
    private AppointmentStatus status;

    /** Constructor for a NEW appointment (ID not known yet, status CONFIRMED). */
    public Appointment(int patientId, int doctorId, LocalDate appointmentDate,
                       LocalTime appointmentTime, ConsultationType consultationType) {
        this(0, patientId, doctorId, appointmentDate, appointmentTime,
                consultationType, AppointmentStatus.CONFIRMED);
    }

    /** Constructor for an appointment loaded from the database. */
    public Appointment(int appointmentId, int patientId, int doctorId, LocalDate appointmentDate,
                       LocalTime appointmentTime, ConsultationType consultationType,
                       AppointmentStatus status) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.consultationType = consultationType;
        this.status = status;
    }

    public int getAppointmentId() { return appointmentId; }
    public void setAppointmentId(int appointmentId) { this.appointmentId = appointmentId; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }

    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }

    public LocalTime getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(LocalTime appointmentTime) { this.appointmentTime = appointmentTime; }

    public ConsultationType getConsultationType() { return consultationType; }
    public void setConsultationType(ConsultationType consultationType) { this.consultationType = consultationType; }

    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }

    @Override
    public String toString() {
        return "Appointment #" + appointmentId + " [patient " + patientId + ", doctor " + doctorId
                + ", " + appointmentDate + " " + appointmentTime + ", " + consultationType
                + ", " + status + "]";
    }
}
