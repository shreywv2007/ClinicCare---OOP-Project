package com.cliniccare.service;

import com.cliniccare.dao.AppointmentDAO;
import com.cliniccare.dao.DoctorDAO;
import com.cliniccare.dao.PatientDAO;
import com.cliniccare.exceptions.AppointmentConflictException;
import com.cliniccare.exceptions.ClinicCareException;
import com.cliniccare.exceptions.ValidationException;
import com.cliniccare.model.Appointment;
import com.cliniccare.model.AppointmentStatus;
import com.cliniccare.model.ConsultationType;
import com.cliniccare.model.Doctor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Business rules for appointments. The UI calls this class; this class calls the DAOs.
 * Conflict rule (first version): same doctor + same date + same time, ignoring CANCELLED.
 */
public class AppointmentService {

    private final AppointmentDAO appointmentDAO;
    private final PatientDAO patientDAO;
    private final DoctorDAO doctorDAO;

    public AppointmentService() {
        this(new AppointmentDAO(), new PatientDAO(), new DoctorDAO());
    }

    public AppointmentService(AppointmentDAO appointmentDAO, PatientDAO patientDAO, DoctorDAO doctorDAO) {
        this.appointmentDAO = appointmentDAO;
        this.patientDAO = patientDAO;
        this.doctorDAO = doctorDAO;
    }

    /** Books a new CONFIRMED appointment after checking all rules. */
    public Appointment bookAppointment(int patientId, int doctorId, LocalDate date,
                                       LocalTime time, ConsultationType type)
            throws ClinicCareException {
        if (date == null || time == null || type == null) {
            throw new ValidationException("Date, time and consultation type are required.");
        }
        checkNotInPast(date, time);

        if (patientDAO.findById(patientId) == null) {
            throw new ValidationException("Patient ID " + patientId + " does not exist.");
        }
        Doctor doctor = doctorDAO.findById(doctorId);
        if (doctor == null) {
            throw new ValidationException("Doctor ID " + doctorId + " does not exist.");
        }
        if (!doctor.isActive()) {
            throw new ValidationException("Dr. " + doctor.getName() + " is inactive and cannot be booked.");
        }
        if (appointmentDAO.hasDoctorConflict(doctorId, date, time, 0)) {
            throw new AppointmentConflictException(
                    "Dr. " + doctor.getName() + " already has an appointment on " + date + " at " + time + ".");
        }

        Appointment appointment = new Appointment(patientId, doctorId, date, time, type);
        appointmentDAO.save(appointment);
        return appointment;
    }

    /** Moves a CONFIRMED appointment to a new date/time, re-checking the conflict rule. */
    public Appointment rescheduleAppointment(int appointmentId, LocalDate newDate, LocalTime newTime)
            throws ClinicCareException {
        if (newDate == null || newTime == null) {
            throw new ValidationException("New date and time are required.");
        }
        checkNotInPast(newDate, newTime);

        Appointment appointment = getExisting(appointmentId);
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ValidationException("Only CONFIRMED appointments can be rescheduled (current status: "
                    + appointment.getStatus() + ").");
        }
        if (appointmentDAO.hasDoctorConflict(appointment.getDoctorId(), newDate, newTime, appointmentId)) {
            throw new AppointmentConflictException(
                    "The doctor already has an appointment on " + newDate + " at " + newTime + ".");
        }

        appointment.setAppointmentDate(newDate);
        appointment.setAppointmentTime(newTime);
        appointmentDAO.update(appointment);
        return appointment;
    }

    /** Cancels an appointment (the record is kept, status becomes CANCELLED). */
    public void cancelAppointment(int appointmentId) throws ClinicCareException {
        updateStatus(appointmentId, AppointmentStatus.CANCELLED);
    }

    /** Changes status following the workflow in AppointmentStatus.canMoveTo(). */
    public void updateStatus(int appointmentId, AppointmentStatus newStatus) throws ClinicCareException {
        if (newStatus == null) {
            throw new ValidationException("A status is required.");
        }
        Appointment appointment = getExisting(appointmentId);
        if (!appointment.getStatus().canMoveTo(newStatus)) {
            throw new ValidationException("Cannot change status from " + appointment.getStatus()
                    + " to " + newStatus + ".");
        }
        appointmentDAO.updateStatus(appointmentId, newStatus);
    }

    /** Daily queue: that day's non-cancelled appointments, earliest time first. */
    public List<Appointment> getDailyQueue(LocalDate date) throws ClinicCareException {
        if (date == null) {
            throw new ValidationException("A date is required.");
        }
        List<Appointment> queue = new ArrayList<>();
        for (Appointment a : appointmentDAO.findByDate(date)) {   // DAO already sorts by time
            if (a.getStatus() != AppointmentStatus.CANCELLED) {
                queue.add(a);
            }
        }
        return queue;
    }

    public List<Appointment> getAllAppointments() throws ClinicCareException {
        return appointmentDAO.findAll();
    }

    public List<Appointment> getAppointmentsForPatient(int patientId) throws ClinicCareException {
        return appointmentDAO.findByPatientId(patientId);
    }

    /** Active doctors for the booking drop-down (inactive doctors never appear). */
    public List<Doctor> getActiveDoctors() throws ClinicCareException {
        return doctorDAO.findActiveDoctors();
    }

    // ---------- helpers ----------

    private Appointment getExisting(int appointmentId) throws ClinicCareException {
        Appointment appointment = appointmentDAO.findById(appointmentId);
        if (appointment == null) {
            throw new ValidationException("Appointment ID " + appointmentId + " does not exist.");
        }
        return appointment;
    }

    private void checkNotInPast(LocalDate date, LocalTime time) throws ValidationException {
        if (LocalDateTime.of(date, time).isBefore(LocalDateTime.now())) {
            throw new ValidationException("The appointment date/time cannot be in the past.");
        }
    }
}
