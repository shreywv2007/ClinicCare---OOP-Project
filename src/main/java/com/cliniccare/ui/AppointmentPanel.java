package com.cliniccare.ui;

import com.cliniccare.exceptions.ClinicCareException;
import com.cliniccare.model.Appointment;
import com.cliniccare.model.AppointmentStatus;
import com.cliniccare.model.ConsultationType;
import com.cliniccare.model.Doctor;
import com.cliniccare.service.AppointmentService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Appointment screen. Talks ONLY to AppointmentService (no SQL here).
 * Member 6 can call refreshData() whenever this tab is shown.
 */
public class AppointmentPanel extends JPanel {

    private static final Color NAVY = new Color(18, 38, 66);
    private static final Color TEAL = new Color(0, 128, 128);

    private final AppointmentService service = new AppointmentService();

    private final JTextField patientIdField = new JTextField(8);
    private final JComboBox<Doctor> doctorCombo = new JComboBox<>();
    private final JTextField dateField = new JTextField(LocalDate.now().toString(), 10);
    private final JTextField timeField = new JTextField("09:00", 6);
    private final JComboBox<ConsultationType> typeCombo = new JComboBox<>(ConsultationType.values());
    private final JComboBox<AppointmentStatus> statusCombo = new JComboBox<>(AppointmentStatus.values());
    private final JTextField queueDateField = new JTextField(LocalDate.now().toString(), 10);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Patient ID", "Doctor ID", "Date", "Time", "Type", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    private boolean showingQueue = false;   // which list is currently displayed

    public AppointmentPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(buildFormPanel(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildQueueBar(), BorderLayout.SOUTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.getTableHeader().setBackground(NAVY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillFormFromSelectedRow();
            }
        });

        doctorCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Doctor) {
                    Doctor d = (Doctor) value;
                    setText(d.getName() + " (" + d.getSpecialization() + ", ID " + d.getId() + ")");
                }
                return this;
            }
        });

        refreshData();
    }

    // ---------- layout ----------

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Appointment details"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        addRow(form, c, 0, "Patient ID:", patientIdField, "Doctor (active only):", doctorCombo);
        addRow(form, c, 1, "Date (YYYY-MM-DD):", dateField, "Time (HH:MM, 24h):", timeField);
        addRow(form, c, 2, "Consultation type:", typeCombo, "Change status to:", statusCombo);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttons.setBackground(Color.WHITE);
        buttons.add(makeButton("Book", e -> onBook()));
        buttons.add(makeButton("Reschedule", e -> onReschedule()));
        buttons.add(makeButton("Cancel Appointment", e -> onCancel()));
        buttons.add(makeButton("Update Status", e -> onUpdateStatus()));
        buttons.add(makeButton("Clear", e -> clearForm()));

        c.gridx = 0; c.gridy = 3; c.gridwidth = 4;
        form.add(buttons, c);
        return form;
    }

    private void addRow(JPanel p, GridBagConstraints c, int row,
                        String label1, JComponent field1, String label2, JComponent field2) {
        c.gridwidth = 1;
        c.gridy = row;
        c.gridx = 0; p.add(new JLabel(label1), c);
        c.gridx = 1; p.add(field1, c);
        c.gridx = 2; p.add(new JLabel(label2), c);
        c.gridx = 3; p.add(field2, c);
    }

    private JPanel buildQueueBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createTitledBorder("View"));
        bar.add(new JLabel("Queue date:"));
        bar.add(queueDateField);
        bar.add(makeButton("Show Daily Queue", e -> onShowQueue()));
        bar.add(makeButton("Show All Appointments", e -> onShowAll()));
        bar.add(makeButton("Refresh", e -> refreshData()));
        return bar;
    }

    private JButton makeButton(String text, java.awt.event.ActionListener listener) {
        JButton b = new JButton(text);
        b.setBackground(TEAL);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.addActionListener(listener);
        return b;
    }

    // ---------- button handlers ----------

    private void onBook() {
        try {
            int patientId = parseId(patientIdField.getText(), "Patient ID");
            Doctor doctor = (Doctor) doctorCombo.getSelectedItem();
            if (doctor == null) {
                throw new IllegalArgumentException("No active doctor is available to select.");
            }
            Appointment a = service.bookAppointment(patientId, doctor.getId(), parseDate(dateField.getText()),
                    parseTime(timeField.getText()), (ConsultationType) typeCombo.getSelectedItem());
            info("Appointment #" + a.getAppointmentId() + " booked successfully.");
            clearForm();
            reloadTable();
        } catch (ClinicCareException | IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    private void onReschedule() {
        Integer id = selectedAppointmentId();
        if (id == null) return;
        try {
            service.rescheduleAppointment(id, parseDate(dateField.getText()), parseTime(timeField.getText()));
            info("Appointment #" + id + " rescheduled.");
            reloadTable();
        } catch (ClinicCareException | IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    private void onCancel() {
        Integer id = selectedAppointmentId();
        if (id == null) return;
        int choice = JOptionPane.showConfirmDialog(this, "Cancel appointment #" + id + "?",
                "Confirm cancellation", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            service.cancelAppointment(id);
            info("Appointment #" + id + " cancelled.");
            reloadTable();
        } catch (ClinicCareException ex) {
            error(ex.getMessage());
        }
    }

    private void onUpdateStatus() {
        Integer id = selectedAppointmentId();
        if (id == null) return;
        try {
            AppointmentStatus status = (AppointmentStatus) statusCombo.getSelectedItem();
            service.updateStatus(id, status);
            info("Appointment #" + id + " is now " + status + ".");
            reloadTable();
        } catch (ClinicCareException ex) {
            error(ex.getMessage());
        }
    }

    private void onShowQueue() {
        try {
            parseDate(queueDateField.getText());   // validate first
            showingQueue = true;
            reloadTable();
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    private void onShowAll() {
        showingQueue = false;
        reloadTable();
    }

    // ---------- public / helper methods ----------

    /** Reloads the doctor drop-down and the table. Safe to call from MainFrame. */
    public void refreshData() {
        try {
            doctorCombo.removeAllItems();
            for (Doctor d : service.getActiveDoctors()) {
                doctorCombo.addItem(d);
            }
        } catch (ClinicCareException ex) {
            error(ex.getMessage());
        }
        reloadTable();
    }

    private void reloadTable() {
        try {
            List<Appointment> rows = showingQueue
                    ? service.getDailyQueue(parseDate(queueDateField.getText()))
                    : service.getAllAppointments();
            tableModel.setRowCount(0);
            for (Appointment a : rows) {
                tableModel.addRow(new Object[]{a.getAppointmentId(), a.getPatientId(), a.getDoctorId(),
                        a.getAppointmentDate(), a.getAppointmentTime(), a.getConsultationType(), a.getStatus()});
            }
        } catch (ClinicCareException | IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    private void fillFormFromSelectedRow() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        patientIdField.setText(String.valueOf(tableModel.getValueAt(row, 1)));
        dateField.setText(String.valueOf(tableModel.getValueAt(row, 3)));
        timeField.setText(String.valueOf(tableModel.getValueAt(row, 4)));
        typeCombo.setSelectedItem(tableModel.getValueAt(row, 5));
    }

    private Integer selectedAppointmentId() {
        int row = table.getSelectedRow();
        if (row < 0) {
            error("Please select an appointment in the table first.");
            return null;
        }
        return (Integer) tableModel.getValueAt(row, 0);
    }

    private void clearForm() {
        patientIdField.setText("");
        dateField.setText(LocalDate.now().toString());
        timeField.setText("09:00");
        typeCombo.setSelectedIndex(0);
        table.clearSelection();
    }

    private int parseId(String text, String fieldName) {
        try {
            int id = Integer.parseInt(text.trim());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a positive whole number.");
        }
    }

    private LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date must be in YYYY-MM-DD format (e.g. 2026-11-20).");
        }
    }

    private LocalTime parseTime(String text) {
        try {
            return LocalTime.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Time must be in 24-hour HH:MM format (e.g. 14:30).");
        }
    }

    private void info(String message) {
        JOptionPane.showMessageDialog(this, message, "ClinicCare", JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
