package com.cliniccare.dao;

import com.cliniccare.database.DatabaseConnection;
import com.cliniccare.exceptions.ClinicCareException;
import com.cliniccare.interfaces.Repository;
import com.cliniccare.model.Appointment;
import com.cliniccare.model.AppointmentStatus;
import com.cliniccare.model.ConsultationType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the appointments table lives here (and nowhere else in Member 4's code).
 * Dates are stored as "YYYY-MM-DD", times as "HH:mm".
 */
public class AppointmentDAO implements Repository<Appointment> {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String SELECT_COLUMNS =
            "SELECT appointment_id, patient_id, doctor_id, appointment_date, appointment_time, "
          + "consultation_type, status FROM appointments ";

    @Override
    public boolean save(Appointment a) throws ClinicCareException {
        String sql = "INSERT INTO appointments (patient_id, doctor_id, appointment_date, "
                   + "appointment_time, consultation_type, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getPatientId());
            ps.setInt(2, a.getDoctorId());
            ps.setString(3, a.getAppointmentDate().toString());
            ps.setString(4, a.getAppointmentTime().format(TIME_FORMAT));
            ps.setString(5, a.getConsultationType().name());
            ps.setString(6, a.getStatus().name());
            boolean saved = ps.executeUpdate() > 0;
            if (saved) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        a.setAppointmentId(keys.getInt(1));
                    }
                }
            }
            return saved;
        } catch (SQLException e) {
            throw new ClinicCareException("Could not save appointment: " + e.getMessage(), e);
        }
    }

    @Override
    public Appointment findById(int id) throws ClinicCareException {
        String sql = SELECT_COLUMNS + "WHERE appointment_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;   // null when not found
            }
        } catch (SQLException e) {
            throw new ClinicCareException("Could not find appointment: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Appointment> findAll() throws ClinicCareException {
        return runListQuery(SELECT_COLUMNS + "ORDER BY appointment_date, appointment_time", null);
    }

    @Override
    public boolean update(Appointment a) throws ClinicCareException {
        String sql = "UPDATE appointments SET patient_id = ?, doctor_id = ?, appointment_date = ?, "
                   + "appointment_time = ?, consultation_type = ?, status = ? WHERE appointment_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, a.getPatientId());
            ps.setInt(2, a.getDoctorId());
            ps.setString(3, a.getAppointmentDate().toString());
            ps.setString(4, a.getAppointmentTime().format(TIME_FORMAT));
            ps.setString(5, a.getConsultationType().name());
            ps.setString(6, a.getStatus().name());
            ps.setInt(7, a.getAppointmentId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ClinicCareException("Could not update appointment: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int id) throws ClinicCareException {
        String sql = "DELETE FROM appointments WHERE appointment_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ClinicCareException("Could not delete appointment: " + e.getMessage(), e);
        }
    }

    // ---------- extra queries for the appointment module ----------

    /** All appointments on a date, earliest first. */
    public List<Appointment> findByDate(LocalDate date) throws ClinicCareException {
        String sql = SELECT_COLUMNS + "WHERE appointment_date = ? ORDER BY appointment_time";
        return runListQuery(sql, date.toString());
    }

    /** All appointments of one patient, newest first. */
    public List<Appointment> findByPatientId(int patientId) throws ClinicCareException {
        String sql = SELECT_COLUMNS + "WHERE patient_id = ? ORDER BY appointment_date DESC, appointment_time DESC";
        return runListQuery(sql, patientId);
    }

    /** Changes only the status column. */
    public boolean updateStatus(int appointmentId, AppointmentStatus status) throws ClinicCareException {
        String sql = "UPDATE appointments SET status = ? WHERE appointment_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, appointmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ClinicCareException("Could not update status: " + e.getMessage(), e);
        }
    }

    /**
     * True if the doctor already has a non-cancelled appointment at the exact same date and time.
     * Pass excludedAppointmentId = 0 when booking a new appointment, or the appointment's own ID
     * when rescheduling (so it does not conflict with itself).
     */
    public boolean hasDoctorConflict(int doctorId, LocalDate date, LocalTime time,
                                     int excludedAppointmentId) throws ClinicCareException {
        String sql = "SELECT COUNT(*) FROM appointments WHERE doctor_id = ? AND appointment_date = ? "
                   + "AND appointment_time = ? AND status <> 'CANCELLED' AND appointment_id <> ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setString(2, date.toString());
            ps.setString(3, time.format(TIME_FORMAT));
            ps.setInt(4, excludedAppointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new ClinicCareException("Could not check doctor availability: " + e.getMessage(), e);
        }
    }

    // ---------- helpers ----------

    /** Runs a SELECT that has at most one parameter (a String date OR an Integer patient ID). */
    private List<Appointment> runListQuery(String sql, Object param) throws ClinicCareException {
        List<Appointment> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (param != null) {
                ps.setObject(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new ClinicCareException("Could not load appointments: " + e.getMessage(), e);
        }
        return list;
    }

    private Appointment mapRow(ResultSet rs) throws SQLException {
        return new Appointment(
                rs.getInt("appointment_id"),
                rs.getInt("patient_id"),
                rs.getInt("doctor_id"),
                LocalDate.parse(rs.getString("appointment_date")),
                LocalTime.parse(rs.getString("appointment_time")),
                ConsultationType.valueOf(rs.getString("consultation_type")),
                AppointmentStatus.valueOf(rs.getString("status")));
    }
}
