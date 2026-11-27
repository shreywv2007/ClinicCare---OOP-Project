package com.cliniccare.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Opens a fresh SQLite connection. The caller must close it. */
public final class DatabaseConnection {
    private DatabaseConnection() { }

    public static Connection getConnection() throws SQLException {
        // Run from the project root. The override is useful for isolated tests.
        Path file = Path.of(System.getProperty("cliniccare.db.path",
                "database/cliniccare.db")).toAbsolutePath().normalize();
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException e) {
            throw new SQLException("Could not create database folder", e);
        }

        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
        // SQLite requires foreign keys to be enabled on every connection.
        try (PreparedStatement statement = connection.prepareStatement("PRAGMA foreign_keys = ON")) {
            statement.execute();
        } catch (SQLException e) {
            try {
                connection.close();
            } catch (SQLException closeError) {
                e.addSuppressed(closeError);
            }
            throw e;
        }
        return connection;
    }
}
