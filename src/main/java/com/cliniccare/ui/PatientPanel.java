package com.cliniccare.ui;

import com.cliniccare.dao.PatientDAO;
import com.cliniccare.exceptions.ClinicCareException;
import com.cliniccare.exceptions.ValidationException;
import com.cliniccare.model.Appointment;
import com.cliniccare.model.Patient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PatientPanel extends JPanel {

    // The panel talks to the database only through the DAO.
    private final PatientDAO patientDAO = new PatientDAO();

    // Form boxes
    private JTextField idField;
    private JTextField nameField;
    private JTextField ageField;
    private JComboBox<String> genderBox;
    private JTextField contactField;
    private JTextField emailField;

    // Search bar
    private JTextField searchField;
    private JButton searchButton;
    private JButton showAllButton;

    // Form buttons
    private JButton addButton;
    private JButton updateButton;
    private JButton clearButton;
    private JButton historyButton;

    // Table
    private DefaultTableModel tableModel;
    private JTable patientTable;

    public PatientPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(buildSearchBar(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildForm(), BorderLayout.EAST);

        connectButtons();
        refresh();
    }

    private JPanel buildSearchBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        searchField = new JTextField(20);
        searchButton = new JButton("Search");
        showAllButton = new JButton("Show All");

        bar.add(new JLabel("Search by ID or name:"));
        bar.add(searchField);
        bar.add(searchButton);
        bar.add(showAllButton);
        return bar;

    }

    private JScrollPane buildTable() {
        String[] columns = { "ID", "Name", "Age", "Gender", "Contact", "Email" };

        tableModel = new DefaultTableModel(columns, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        patientTable = new JTable(tableModel);
        patientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return new JScrollPane(patientTable);
    }

    private JPanel buildForm() {
        JPanel fields = new JPanel(new GridLayout(0, 2, 5, 8));

        idField = new JTextField(15);
        idField.setEditable(false);
        nameField = new JTextField(15);
        ageField = new JTextField(15);
        genderBox = new JComboBox<>(new String[] { "Male", "Female", "Other"});
        contactField = new JTextField(15);
        emailField = new JTextField(15);

        fields.add(new JLabel("Patient ID:"));
        fields.add(idField);
        fields.add(new JLabel("Name:"));
        fields.add(nameField);
        fields.add(new JLabel("Age:"));
        fields.add(ageField);
        fields.add(new JLabel("Gender:"));
        fields.add(genderBox);
        fields.add(new JLabel("Contact:"));
        fields.add(contactField);
        fields.add(new JLabel("Email:"));
        fields.add(emailField);

        addButton = new JButton("Add Patient");
        updateButton = new JButton("Update");
        clearButton = new JButton("Clear");
        historyButton = new JButton("View History");

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(clearButton);
        buttons.add(historyButton);

        JPanel form = new JPanel(new BorderLayout(5, 10));
        form.setBorder(BorderFactory.createTitledBorder("Patient Details"));
        form.add(fields, BorderLayout.NORTH);
        form.add(buttons, BorderLayout.CENTER);
        return form;
    }

    private void connectButtons() {
        clearButton.addActionListener(e -> clearForm());
        addButton.addActionListener(e -> addPatient());
        updateButton.addActionListener(e -> updatePatient());
        historyButton.addActionListener(e -> showHistory());
        searchButton.addActionListener(e -> searchPatients());
        searchField.addActionListener(e -> searchPatients());   // Enter key
        showAllButton.addActionListener(e -> {
            searchField.setText("");
            refresh();
        });

        patientTable.getSelectionModel().addListSelectionListener(e -> fillFormFromSelectedRow());
    }

    public void showPatients(List<Patient> patients) {
        tableModel.setRowCount(0);
        for (Patient p : patients) {
            tableModel.addRow(new Object[]{
                p.getId(), p.getName(), p.getAge(),
                p.getGender(), p.getContact(), p.getEmail()
            });
        }
    }

    private void fillFormFromSelectedRow() {
        int row = patientTable.getSelectedRow();
        if (row == -1) {
            return;
        }
        idField.setText(tableModel.getValueAt(row, 0).toString());
        nameField.setText(tableModel.getValueAt(row, 1).toString());
        ageField.setText(tableModel.getValueAt(row, 2).toString());
        genderBox.setSelectedItem(tableModel.getValueAt(row, 3).toString());
        contactField.setText(tableModel.getValueAt(row, 4).toString());
        emailField.setText(tableModel.getValueAt(row, 5).toString());
    }

    private void clearForm() {
        idField.setText("");
        nameField.setText("");
        ageField.setText("");
        genderBox.setSelectedIndex(0);
        contactField.setText("");
        emailField.setText("");
        patientTable.clearSelection();
    }

    // ---------- button actions ----------

    /** Reloads every patient from the database. Other panels can call this too. */
    public void refresh() {
        try {
            showPatients(patientDAO.findAll());
        } catch (ClinicCareException e) {
            showError(e.getMessage());
        }
    }

    private void addPatient() {
        try {
            Patient p = readForm(0);            // id 0 = not saved yet
            if (patientDAO.save(p)) {
                JOptionPane.showMessageDialog(this, "Patient added with ID " + p.getId() + ".");
                clearForm();
                refresh();
            }
        } catch (ClinicCareException e) {      // also catches ValidationException (subclass)
            showError(e.getMessage());
        }
    }

    private void updatePatient() {
        if (idField.getText().isBlank()) {
            showError("Select a patient in the table first.");
            return;
        }
        try {
            int id = Integer.parseInt(idField.getText());
            Patient p = readForm(id);
            if (patientDAO.update(p)) {
                JOptionPane.showMessageDialog(this, "Patient #" + id + " updated.");
                clearForm();
                refresh();
            } else {
                showError("Patient #" + id + " no longer exists.");
            }
        } catch (ClinicCareException e) {      // also catches ValidationException (subclass)
            showError(e.getMessage());
        }
    }

    /** A number searches by ID; anything else searches by name. */
    private void searchPatients() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            refresh();
            return;
        }
        try {
            if (text.matches("\\d+")) {
                Patient p = patientDAO.findById(Integer.parseInt(text));
                showPatients(p == null ? List.of() : List.of(p));
            } else {
                showPatients(patientDAO.searchByName(text));
            }
            if (tableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No patient matches \"" + text + "\".");
            }
        } catch (NumberFormatException e) {
            showError("That ID is too large.");
        } catch (ClinicCareException e) {
            showError(e.getMessage());
        }
    }

    /** Shows the selected patient's appointments in a pop-up table. */
    private void showHistory() {
        if (idField.getText().isBlank()) {
            showError("Select a patient in the table first.");
            return;
        }
        int id = Integer.parseInt(idField.getText());
        try {
            List<Appointment> history = patientDAO.findAppointmentHistory(id);
            if (history.isEmpty()) {
                JOptionPane.showMessageDialog(this, nameField.getText() + " has no appointments yet.");
                return;
            }
            String[] columns = { "Appt ID", "Doctor ID", "Date", "Time", "Type", "Status" };
            DefaultTableModel model = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            for (Appointment a : history) {
                model.addRow(new Object[]{
                    a.getAppointmentId(), a.getDoctorId(), a.getAppointmentDate(),
                    a.getAppointmentTime(), a.getConsultationType(), a.getStatus()
                });
            }
            JScrollPane scroll = new JScrollPane(new JTable(model));
            scroll.setPreferredSize(new Dimension(550, 200));
            JOptionPane.showMessageDialog(this, scroll,
                    "Appointment history: " + nameField.getText(), JOptionPane.PLAIN_MESSAGE);
        } catch (ClinicCareException e) {
            showError(e.getMessage());
        }
    }

    // ---------- validation ----------

    /** Checks the form and builds a Patient, or throws ValidationException. */
    private Patient readForm(int id) throws ValidationException {
        String name = nameField.getText().trim();
        String ageText = ageField.getText().trim();
        String gender = (String) genderBox.getSelectedItem();
        String contact = contactField.getText().trim();
        String email = emailField.getText().trim();

        if (name.isEmpty()) {
            throw new ValidationException("Name is required.");
        }
        if (!name.matches("[A-Za-z .'-]+")) {
            throw new ValidationException("Name can only contain letters, spaces, . ' and -.");
        }
        int age;
        try {
            age = Integer.parseInt(ageText);
        } catch (NumberFormatException e) {
            throw new ValidationException("Age must be a whole number.");
        }
        if (age < 0 || age > 120) {
            throw new ValidationException("Age must be between 0 and 120.");
        }
        if (!contact.matches("\\+?\\d{7,15}")) {
            throw new ValidationException("Contact must be 7 to 15 digits (a leading + is allowed).");
        }
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new ValidationException("Enter a valid email, like name@example.com.");
        }
        return new Patient(id, name, contact, age, gender, email);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Patient error", JOptionPane.ERROR_MESSAGE);
    }
}
