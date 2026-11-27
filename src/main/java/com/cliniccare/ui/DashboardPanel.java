package com.cliniccare.ui;

import com.cliniccare.exceptions.ClinicCareException;
import com.cliniccare.model.Appointment;
import java.time.LocalDate;
import java.util.List;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class DashboardPanel extends JPanel {
    private static final Color BACKGROUND = new Color(244, 247, 249);
    private static final Color NAVY = new Color(22, 42, 61);
    private static final Color TEAL = new Color(0, 111, 107);
    private static final Color MUTED = new Color(81, 99, 113);
    private static final Color LINE = new Color(219, 228, 233);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    // Provisional UI-only adapter; replace its wiring with ClinicManager after team agreement.
    @FunctionalInterface
    public interface SummaryLoader {
        Summary load() throws ClinicCareException;
    }

    public record Summary(LocalDate date, Integer totalPatients, Integer activeDoctors,
            int pendingAppointments, int completedAppointments, Integer pendingPayments,
            List<Appointment> todayAppointments) { }

    private final SummaryLoader loader;
    private final JLabel totalPatients = numberLabel();
    private final JLabel activeDoctors = numberLabel();
    private final JLabel todayCount = numberLabel();
    private final JLabel pendingAppointments = numberLabel();
    private final JLabel completedAppointments = numberLabel();
    private final JLabel pendingPayments = numberLabel();
    private final JLabel updateStatus = new JLabel("Not refreshed yet");
    private final JLabel tableTitle = new JLabel("Today's appointments");
    private final JLabel tableStatus = new JLabel(" ");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Appointment ID", "Patient ID", "Doctor ID", "Date", "Time", "Status"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
        @Override public Class<?> getColumnClass(int column) {
            return column < 3 ? Integer.class : String.class;
        }
    };

    public DashboardPanel(SummaryLoader loader) {
        this.loader = loader;
        setLayout(new BorderLayout(0, 18));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(22, 24, 22, 24));

        JPanel header = new JPanel(new BorderLayout(10, 10));
        header.setOpaque(false);
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 4));
        heading.setOpaque(false);
        JLabel title = new JLabel("Dashboard");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        title.setForeground(NAVY);
        JLabel subtitle = new JLabel("Clinic activity overview | Integration in progress");
        subtitle.setForeground(MUTED);
        heading.add(title);
        heading.add(subtitle);
        header.add(heading, BorderLayout.WEST);

        JButton refresh = new JButton("Refresh");
        refresh.setBackground(TEAL);
        refresh.setForeground(Color.WHITE);
        refresh.setFocusPainted(false);
        refresh.setBorder(new EmptyBorder(10, 18, 10, 18));
        refresh.addActionListener(e -> refreshData());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        actions.setOpaque(false);
        actions.add(refresh);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        JPanel cards = new JPanel(new GridLayout(2, 3, 12, 12));
        cards.setOpaque(false);
        cards.add(makeCard("Total Patients", "All registered patients", totalPatients));
        cards.add(makeCard("Active Doctors", "Currently active", activeDoctors));
        cards.add(makeCard("Today's Appointments", "All statuses today", todayCount));
        cards.add(makeCard("Pending Appointments", "Confirmed / in progress", pendingAppointments));
        cards.add(makeCard("Completed Appointments", "All dates", completedAppointments));
        cards.add(makeCard("Pending Payments", "Unpaid bills", pendingPayments));
        body.add(cards, BorderLayout.NORTH);

        JPanel tableCard = new JPanel(new BorderLayout(0, 10));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), new EmptyBorder(14, 14, 14, 14)));
        tableTitle.setForeground(NAVY);
        tableTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        tableCard.add(tableTitle, BorderLayout.NORTH);
        JTable table = new JTable(tableModel);
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(new Color(211, 238, 235));
        table.setSelectionForeground(NAVY);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(new Color(232, 239, 243));
        table.getTableHeader().setForeground(NAVY);
        table.setGridColor(LINE);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(650, 215));
        scroll.setBorder(BorderFactory.createLineBorder(LINE));
        tableCard.add(scroll, BorderLayout.CENTER);
        tableStatus.setForeground(MUTED);
        tableCard.add(tableStatus, BorderLayout.SOUTH);
        body.add(tableCard, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        updateStatus.setForeground(MUTED);
        add(updateStatus, BorderLayout.SOUTH);
    }

    private static JLabel numberLabel() {
        JLabel label = new JLabel("\u2014");
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        label.setForeground(TEAL);
        return label;
    }

    private JPanel makeCard(String title, String detail, JLabel number) {
        JPanel card = new JPanel(new GridLayout(3, 1, 0, 3));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), new EmptyBorder(12, 14, 12, 14)));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        titleLabel.setForeground(NAVY);
        JLabel detailLabel = new JLabel(detail);
        detailLabel.setForeground(MUTED);
        detailLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        card.add(titleLabel);
        card.add(number);
        card.add(detailLabel);
        return card;
    }

    private static String formatCount(Integer count) {
        return count == null ? "—" : count.toString();
    }

    /** Refresh on the EDT, on navigation, after successful writes or by button. */
    public void refreshData() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshData);
            return;
        }
        try {
            Summary data = loader.load();
            totalPatients.setText(formatCount(data.totalPatients()));
            activeDoctors.setText(formatCount(data.activeDoctors()));
            todayCount.setText(String.valueOf(data.todayAppointments().size()));
            pendingAppointments.setText(String.valueOf(data.pendingAppointments()));
            completedAppointments.setText(String.valueOf(data.completedAppointments()));
            pendingPayments.setText(formatCount(data.pendingPayments()));
            tableTitle.setText("Today's appointments  |  " + data.date());
            tableModel.setRowCount(0);
            for (Appointment appointment : data.todayAppointments()) {
                tableModel.addRow(new Object[]{appointment.getAppointmentId(),
                    appointment.getPatientId(), appointment.getDoctorId(),
                    appointment.getAppointmentDate().toString(),
                    appointment.getAppointmentTime().format(TIME), appointment.getStatus().name()});
            }
            tableStatus.setText(data.todayAppointments().size() == 0 ? "No appointments for today."
                    : data.todayAppointments().size() + " appointment(s), sorted by time.");
            updateStatus.setForeground(MUTED);
            updateStatus.setText("Last refreshed at " + LocalTime.now().format(TIME)
                    + " | — means awaiting module integration.");
        } catch (ClinicCareException | RuntimeException ex) {
            // Never show zero or old counts as if a failed database read succeeded.
            for (JLabel label : new JLabel[]{totalPatients, activeDoctors, todayCount,
                    pendingAppointments, completedAppointments, pendingPayments}) {
                label.setText("\u2014");
            }
            tableModel.setRowCount(0);
            tableTitle.setText("Today's appointments");
            tableStatus.setText("Data unavailable. Try refreshing again.");
            updateStatus.setForeground(new Color(160, 35, 35));
            updateStatus.setText("Refresh failed: " + ex.getMessage());
            if (isShowing()) {
                JOptionPane.showMessageDialog(this, "Could not refresh the dashboard.\n" + ex.getMessage(),
                        "Dashboard error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
