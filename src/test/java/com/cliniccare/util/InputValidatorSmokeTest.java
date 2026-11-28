package com.cliniccare.util;

import com.cliniccare.exceptions.ValidationException;
import java.time.LocalDate;
import java.time.LocalTime;

/** Standalone test: run using Maven exec with test classpath. */
public final class InputValidatorSmokeTest {
    @FunctionalInterface
    private interface Check { void run() throws ValidationException; }

    private static int passed;

    private static void valid(String label, Check check) throws ValidationException {
        check.run();
        passed++;
        System.out.println("PASS: " + label);
    }

    private static void invalid(String label, Check check) {
        try {
            check.run();
        } catch (ValidationException expected) {
            passed++;
            System.out.println("PASS: " + label + " rejected");
            return;
        }
        throw new AssertionError("Expected ValidationException: " + label);
    }

    public static void main(String[] args) throws ValidationException {
        valid("Required text", () -> InputValidator.validateRequired("Test Patient", "Name"));
        valid("Age", () -> {
            if (InputValidator.validateAge("25") != 25) throw new AssertionError("Age mismatch");
        });
        valid("Phone", () -> InputValidator.validatePhone("+971 50 123 4567"));
        valid("Email", () -> InputValidator.validateEmail("test@example.com"));
        valid("Fee", () -> {
            if (InputValidator.validateFee("150.50") != 150.50) throw new AssertionError("Fee mismatch");
        });
        valid("Leap-day date", () -> {
            if (!InputValidator.validateDate("2028-02-29").equals(LocalDate.of(2028, 2, 29)))
                throw new AssertionError("Date mismatch");
        });
        valid("24-hour time", () -> {
            if (!InputValidator.validateTime("09:30").equals(LocalTime.of(9, 30)))
                throw new AssertionError("Time mismatch");
        });
        invalid("Blank name", () -> InputValidator.validateRequired("  ", "Name"));
        invalid("Negative age", () -> InputValidator.validateAge(-1));
        invalid("Age above 120", () -> InputValidator.validateAge(121));
        invalid("Non-numeric age", () -> InputValidator.validateAge("abc"));
        invalid("Invalid phone", () -> InputValidator.validatePhone("abc123"));
        invalid("Invalid email", () -> InputValidator.validateEmail("test@"));
        invalid("Negative fee", () -> InputValidator.validateFee(-100));
        invalid("Non-finite fee", () -> InputValidator.validateFee(Double.NaN));
        invalid("Impossible date", () -> InputValidator.validateDate("2027-02-29"));
        invalid("Impossible time", () -> InputValidator.validateTime("24:00"));
        System.out.println("MEMBER 6 INPUT VALIDATOR SMOKE TEST PASSED: " + passed + " checks");
    }
}
