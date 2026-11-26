package com.cliniccare.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Run manually with Maven exec:java; no test framework needed. */
public final class DatabaseSmokeTest {
    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("cliniccare-test-");
        Path file = temp.resolve("test.db");
        String previous = System.getProperty("cliniccare.db.path");
        System.setProperty("cliniccare.db.path", file.toString());
        try {
            DatabaseInitializer.initialize();
            try (Connection c = DatabaseConnection.getConnection()) {
                check(scalar(c, "PRAGMA foreign_keys") == 1, "Foreign keys enabled");
                for (String table : new String[]{"patients", "doctors", "appointments", "bills"}) {
                    check(scalar(c, "SELECT COUNT(*) FROM " + table) == 0, table + " exists and is empty");
                }
                write(c, "INSERT INTO patients(name,age,gender,contact,email) VALUES ('Test Patient',25,'Other','0500000000','test@example.com')");
                write(c, "INSERT INTO doctors(name,specialization,contact,consultation_fee) VALUES ('Test Doctor','General','0500000001',150)");
                String booking = "INSERT INTO appointments(patient_id,doctor_id,appointment_date,appointment_time,consultation_type) VALUES (1,1,'2030-01-01','09:00','GENERAL')";
                write(c, booking);
                write(c, "INSERT INTO bills(appointment_id,amount) VALUES (1,150)");
                check(scalar(c, "SELECT COUNT(*) FROM bills WHERE payment_status='UNPAID'") == 1, "Unpaid bill stored");
                reject(c, booking, "Duplicate doctor slot rejected");
                reject(c, "INSERT INTO bills(appointment_id,amount) VALUES (999,150)", "Invalid foreign key rejected");
                reject(c, "INSERT INTO bills(appointment_id,amount) VALUES (1,-1)", "Negative bill rejected");
                reject(c, "UPDATE appointments SET status='INVALID'", "Invalid appointment status rejected");
                write(c, "UPDATE appointments SET status='CANCELLED' WHERE appointment_id=1");
                write(c, booking);
                check(scalar(c, "SELECT COUNT(*) FROM appointments") == 2, "Cancelled slot can be reused");
            }
            DatabaseInitializer.initialize();
            try (Connection c = DatabaseConnection.getConnection()) {
                check(scalar(c, "SELECT COUNT(*) FROM patients") == 1, "Data survives reopening and initialization");
                check(scalar(c, "PRAGMA foreign_keys") == 1, "Foreign keys enabled on reopened connection");
            }
            System.out.println("MEMBER 1 DATABASE SMOKE TEST PASSED");
        } finally {
            if (previous == null) System.clearProperty("cliniccare.db.path");
            else System.setProperty("cliniccare.db.path", previous);
            Files.deleteIfExists(file);
            Files.deleteIfExists(temp);
        }
    }

    private static int scalar(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("No result");
            return rs.getInt(1);
        }
    }

    private static void write(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) { ps.executeUpdate(); }
    }

    private static void reject(Connection c, String sql, String message) throws SQLException {
        try { write(c, sql); }
        catch (SQLException expected) {
            // SQLITE_CONSTRAINT has primary error code 19.
            if ((expected.getErrorCode() & 255) != 19) throw expected;
            check(true, message);
            return;
        }
        throw new AssertionError("Expected constraint rejection: " + message);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("PASS: " + message);
    }
}
