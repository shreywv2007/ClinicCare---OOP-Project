package com.cliniccare.dao;

import com.cliniccare.database.DatabaseConnection;
import com.cliniccare.exceptions.ClinicCareException;
import com.cliniccare.interfaces.Repository;
import com.cliniccare.model.Appointment;
import com.cliniccare.model.Patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** All SQL for the patients table lives here, never in the Swing panel. */
public class PatientDAO implements Repository<Patient> {

    private static final String SELECT_COLUMNS =
            "SELECT patient_id, name, age, gender, contact, email FROM patients ";

    // Used only for appointment history.
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();

    @Override
    public boolean save(Patient p) throws ClinicCareException {
        String sql = "INSERT INTO patients (name, age, gender, contact, email) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setInt(2, p.getAge());
            ps.setString(3, p.getGender());
            ps.setString(4, p.getContact());
            ps.setString(5, p.getEmail());
            boolean saved = ps.executeUpdate() > 0;
            if (saved) {
                // SQLite made the new patient_id; copy it back into the object.
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        p.setId(keys.getInt(1));
                    }
                }
            }
            return saved;
        } catch (SQLException e) {
            throw new ClinicCareException("Could not save patient: " + e.getMessage(), e);
        }
    }

    @Override
    public Patient findById(int id) throws ClinicCareException {
        String sql = SELECT_COLUMNS + "WHERE patient_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;   // null when not found
            }
        } catch (SQLException e) {
            throw new ClinicCareException("Could not find patient: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Patient> findAll() throws ClinicCareException {
        String sql = SELECT_COLUMNS + "ORDER BY patient_id";
        List<Patient> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new ClinicCareException("Could not load patients: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean update(Patient p) throws ClinicCareException {
        String sql = "UPDATE patients SET name = ?, age = ?, gender = ?, contact = ?, email = ? "
                   + "WHERE patient_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setInt(2, p.getAge());
            ps.setString(3, p.getGender());
            ps.setString(4, p.getContact());
            ps.setString(5, p.getEmail());
            ps.setInt(6, p.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ClinicCareException("Could not update patient: " + e.getMessage(), e);
        }
    }

    /** Fails if the patient has appointments: the foreign key protects them. */
    @Override
    public boolean delete(int id) throws ClinicCareException {
        String sql = "DELETE FROM patients WHERE patient_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ClinicCareException("Could not delete patient: " + e.getMessage(), e);
        }
    }

    // ---------- extra query for the patient module ----------

    /** Patients whose name contains the text, ignoring case. */
    public List<Patient> searchByName(String name) throws ClinicCareException {
        String sql = SELECT_COLUMNS + "WHERE name LIKE ? ORDER BY name";
        List<Patient> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + name.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new ClinicCareException("Could not search patients: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Every appointment this patient has had, newest first.
     * The appointments table belongs to AppointmentDAO, so we reuse its query
     * instead of writing the same SQL twice.
     */
    public List<Appointment> findAppointmentHistory(int patientId) throws ClinicCareException {
        return appointmentDAO.findByPatientId(patientId);
    }

    // ---------- helper ----------

    /** Turns the current database row into a Patient object. */
    private Patient mapRow(ResultSet rs) throws SQLException {
        return new Patient(
                rs.getInt("patient_id"),
                rs.getString("name"),
                rs.getString("contact"),
                rs.getInt("age"),
                rs.getString("gender"),
                rs.getString("email"));
    }
}
