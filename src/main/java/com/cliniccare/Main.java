package com.cliniccare;

import com.cliniccare.dao.AppointmentDAO;
import com.cliniccare.database.DatabaseInitializer;
import com.cliniccare.exceptions.ClinicCareException;
import com.cliniccare.model.Appointment;
import com.cliniccare.model.AppointmentStatus;
import com.cliniccare.ui.DashboardPanel;
import com.cliniccare.ui.MainFrame;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        boolean checkOnly = args.length == 1 && "--check-dashboard".equals(args[0]);
        try {
            DatabaseInitializer.initialize();
            AppointmentDAO appointmentDAO = new AppointmentDAO();
            DashboardPanel.SummaryLoader loader = () -> loadSummary(appointmentDAO);
            // Headless verification mode for Codespaces (no desktop is required).
            if (checkOnly) {
                DashboardPanel.Summary data = loader.load();
                System.out.println("Today's appointments: " + data.todayAppointments().size());
                System.out.println("Pending appointments: " + data.pendingAppointments());
                System.out.println("Completed appointments: " + data.completedAppointments());
                System.out.println("Patient, doctor and billing totals: awaiting integration");
                System.out.println("MEMBER 6 STAGE 2 DATABASE READ PASSED");
                return;
            }
            SwingUtilities.invokeLater(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                    new MainFrame(loader).setVisible(true);
                } catch (Exception ex) {
                    showStartupError(ex);
                }
            });
        } catch (Exception ex) {
            if (checkOnly) {
                ex.printStackTrace();
                System.exit(1);
            } else {
                SwingUtilities.invokeLater(() -> showStartupError(ex));
            }
        }
    }

    // Temporary read-only adapter in the entry point, until ClinicManager is agreed.
    // SQL stays in the existing AppointmentDAO. No patient/doctor/billing DAO is duplicated.
    private static DashboardPanel.Summary loadSummary(AppointmentDAO dao)
            throws ClinicCareException {
        LocalDate today = LocalDate.now();
        List<Appointment> todayAppointments = new ArrayList<>();
        int pending = 0;
        int completed = 0;
        for (Appointment appointment : dao.findAll()) {
            if (today.equals(appointment.getAppointmentDate())) {
                todayAppointments.add(appointment);
            }
            AppointmentStatus status = appointment.getStatus();
            if (status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.IN_PROGRESS) {
                pending++;
            } else if (status == AppointmentStatus.COMPLETED) {
                completed++;
            }
        }
        todayAppointments.sort(Comparator.comparing(Appointment::getAppointmentTime)
                .thenComparingInt(Appointment::getAppointmentId));
        // null represents unavailable metrics, never a fabricated zero.
        return new DashboardPanel.Summary(today, null, null, pending, completed, null,
                List.copyOf(todayAppointments));
    }

    private static void showStartupError(Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "ClinicCare could not start.\n" + ex.getMessage(),
                "Startup error", JOptionPane.ERROR_MESSAGE);
    }
}
