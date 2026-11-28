package com.cliniccare.database;

import com.cliniccare.exceptions.ClinicCareException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Creates the shared schema without deleting existing records. */
public final class DatabaseInitializer {
    private DatabaseInitializer() { }

    private static final String[] SCHEMA = {
        """
        CREATE TABLE IF NOT EXISTS patients (
            patient_id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            age INTEGER NOT NULL CHECK (typeof(age) = 'integer' AND age >= 0),
            gender TEXT NOT NULL,
            contact TEXT NOT NULL,
            email TEXT NOT NULL
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS doctors (
            doctor_id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            specialization TEXT NOT NULL,
            contact TEXT NOT NULL,
            consultation_fee REAL NOT NULL CHECK (typeof(consultation_fee) IN ('real', 'integer') AND consultation_fee >= 0),
            is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1))
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS appointments (
            appointment_id INTEGER PRIMARY KEY AUTOINCREMENT,
            patient_id INTEGER NOT NULL REFERENCES patients(patient_id),
            doctor_id INTEGER NOT NULL REFERENCES doctors(doctor_id),
            appointment_date TEXT NOT NULL,
            appointment_time TEXT NOT NULL,
            consultation_type TEXT NOT NULL,
            status TEXT NOT NULL DEFAULT 'CONFIRMED'
                CHECK (status IN ('CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS bills (
            bill_id INTEGER PRIMARY KEY AUTOINCREMENT,
            appointment_id INTEGER NOT NULL REFERENCES appointments(appointment_id),
            amount REAL NOT NULL CHECK (typeof(amount) IN ('real', 'integer') AND amount >= 0),
            payment_status TEXT NOT NULL DEFAULT 'UNPAID'
                CHECK (payment_status IN ('PAID', 'UNPAID')),
            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
        )
        """,
        """
        CREATE UNIQUE INDEX IF NOT EXISTS uq_doctor_slot
        ON appointments(doctor_id, appointment_date, appointment_time)
        WHERE status <> 'CANCELLED'
        """,
        "CREATE INDEX IF NOT EXISTS idx_appointment_patient ON appointments(patient_id)",
        "CREATE INDEX IF NOT EXISTS idx_appointment_date ON appointments(appointment_date, appointment_time)",
        "CREATE INDEX IF NOT EXISTS idx_bill_appointment ON bills(appointment_id)"
    };

    public static void initialize() throws ClinicCareException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                for (String sql : SCHEMA) {
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.execute();
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new ClinicCareException("Could not initialize ClinicCare database", e);
        }
    }
}
