package com.cliniccare.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class MainFrame extends JFrame {
    private static final Color NAVY = new Color(22, 42, 61);
    private static final Color TEAL = new Color(0, 111, 107);
    private static final Color LIGHT = new Color(244, 247, 249);
    private static final Color SIDEBAR_TEXT = new Color(226, 235, 241);

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    private final Map<String, JButton> navigation = new LinkedHashMap<>();
    private final DashboardPanel dashboard;
    private final PatientPanel patients;

    public MainFrame(DashboardPanel.SummaryLoader loader) {
        super("ClinicCare | Clinic Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(1000, 650));
        setSize(1200, 760);
        setLocationRelativeTo(null);
        content.setBackground(LIGHT);

        dashboard = new DashboardPanel(loader);
        content.add(dashboard, "Dashboard");
        // Temporary pages are clearly marked. No teammate classes are duplicated.
        patients = new PatientPanel();
        content.add(patients, "Patients");
        content.add(waitingPage("Doctors", "Doctor module not integrated yet."), "Doctors");
        content.add(waitingPage("Appointments", "Appointment screen requires patient and doctor modules."), "Appointments");
        content.add(waitingPage("Billing", "Billing module not integrated yet."), "Billing");
        add(createSidebar(), BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        showPage("Dashboard");
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(NAVY);
        sidebar.setPreferredSize(new Dimension(212, 0));
        sidebar.setBorder(new EmptyBorder(25, 14, 20, 14));
        JLabel brand = new JLabel("ClinicCare");
        brand.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        brand.setForeground(Color.WHITE);
        brand.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(brand);
        JLabel subtitle = new JLabel("CLINIC ADMINISTRATION");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        subtitle.setForeground(SIDEBAR_TEXT);
        subtitle.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(subtitle);
        sidebar.add(Box.createVerticalStrut(35));

        for (String page : new String[]{"Dashboard", "Patients", "Doctors", "Appointments", "Billing"}) {
            JButton button = new JButton(page);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setAlignmentX(LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            button.setPreferredSize(new Dimension(184, 44));
            button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            button.setForeground(SIDEBAR_TEXT);
            button.setBackground(NAVY);
            button.setOpaque(true);
            button.setFocusPainted(false);
            button.setBorder(new EmptyBorder(10, 12, 10, 12));
            button.addActionListener(e -> showPage(page));
            navigation.put(page, button);
            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(5));
        }
        sidebar.add(Box.createVerticalGlue());
        JLabel footer = new JLabel("ClinicCare  |  v1.0");
        footer.setAlignmentX(LEFT_ALIGNMENT);
        footer.setForeground(SIDEBAR_TEXT);
        sidebar.add(footer);
        return sidebar;
    }

    private void showPage(String page) {
        cardLayout.show(content, page);
        for (Map.Entry<String, JButton> entry : navigation.entrySet()) {
            boolean selected = entry.getKey().equals(page);
            entry.getValue().setBackground(selected ? TEAL : NAVY);
            entry.getValue().setForeground(selected ? Color.WHITE : SIDEBAR_TEXT);
        }
        if ("Dashboard".equals(page)) {
            dashboard.refreshData();
        } else if ("Patients".equals(page)) {
            patients.refresh();
        }
    }

    private JPanel waitingPage(String title, String message) {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(LIGHT);
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));
        JLabel heading = new JLabel(title);
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        heading.setForeground(NAVY);
        JLabel notice = new JLabel(message, SwingConstants.CENTER);
        notice.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        panel.add(heading, BorderLayout.NORTH);
        panel.add(notice, BorderLayout.CENTER);
        return panel;
    }
}
