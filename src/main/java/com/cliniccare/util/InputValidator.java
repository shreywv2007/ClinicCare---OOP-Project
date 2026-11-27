package com.cliniccare.util;

import com.cliniccare.exceptions.ValidationException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/** Shared input-format checks. Appointment business rules belong in AppointmentService. */
public final class InputValidator {
    private InputValidator() { }

    public static void validateRequired(String value, String fieldName)
            throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " is required.");
        }
    }

    public static void validateAge(int age) throws ValidationException {
        if (age < 0 || age > 120) {
            throw new ValidationException("Age must be between 0 and 120.");
        }
    }

    public static int validateAge(String value) throws ValidationException {
        validateRequired(value, "Age");
        int age;
        try {
            age = Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new ValidationException("Age must be a whole number.");
        }
        validateAge(age);
        return age;
    }

    public static void validatePhone(String value) throws ValidationException {
        validateRequired(value, "Phone number");
        String phone = value.trim();
        String digits = phone.replaceAll("[+ -]", "");
        if (!phone.matches("\\+?[0-9]+(?:[ -][0-9]+)*")
                || digits.length() < 7 || digits.length() > 15) {
            throw new ValidationException(
                    "Enter a phone number containing 7 to 15 digits (optional +, spaces or hyphens).");
        }
    }

    public static void validateEmail(String value) throws ValidationException {
        validateRequired(value, "Email");
        String pattern = "[A-Za-z0-9_+%'-]+(?:\\.[A-Za-z0-9_+%'-]+)*@"
                + "[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?"
                + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)*\\.[A-Za-z]{2,}";
        if (!value.trim().matches(pattern)) {
            throw new ValidationException("Enter a valid email address, such as user@example.com.");
        }
    }

    public static void validateFee(double fee) throws ValidationException {
        if (!Double.isFinite(fee) || fee < 0) {
            throw new ValidationException("Consultation fee must be a finite, non-negative number.");
        }
    }

    public static double validateFee(String value) throws ValidationException {
        validateRequired(value, "Consultation fee");
        double fee;
        try {
            // Accept ordinary decimal amounts only, not currency symbols or Java numeric literals.
            if (!value.trim().matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)")) {
                throw new NumberFormatException();
            }
            fee = Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            throw new ValidationException("Enter a numeric consultation fee, such as 150.00.");
        }
        validateFee(fee);
        return fee;
    }

    public static LocalDate validateDate(String value) throws ValidationException {
        validateRequired(value, "Date");
        try {
            if (!value.trim().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
                throw new DateTimeParseException("Wrong format", value, 0);
            }
            LocalDate date = LocalDate.parse(value.trim());
            if (date.getYear() < 1) {
                throw new DateTimeParseException("Invalid year", value, 0);
            }
            return date;
        } catch (DateTimeParseException ex) {
            throw new ValidationException("Enter a real date in YYYY-MM-DD format.");
        }
    }

    public static LocalTime validateTime(String value) throws ValidationException {
        validateRequired(value, "Time");
        try {
            if (!value.trim().matches("[0-9]{2}:[0-9]{2}")) {
                throw new DateTimeParseException("Wrong format", value, 0);
            }
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new ValidationException("Enter a valid 24-hour time in HH:MM format (00:00 to 23:59).");
        }
    }
}
