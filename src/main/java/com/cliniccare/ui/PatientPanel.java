package com.cliniccare.ui;

import com.cliniccare.model.Patient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PatientPanel extends JPanel {

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

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(clearButton);

        JPanel form = new JPanel(new BorderLayout(5, 10));
        form.setBorder(BorderFactory.createTitledBorder("Patient Details"));
        form.add(fields, BorderLayout.NORTH);
        form.add(buttons, BorderLayout.CENTER);
        return form;
    }

    private void connectButtons() {
        clearButton.addActionListener(e -> clearForm());


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
    
}