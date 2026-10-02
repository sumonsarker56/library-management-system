import java.awt.*;
import java.io.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class LibrarySystem extends JFrame {

    JTextField idField, titleField, authorField, searchField;
    JComboBox<String> roleBox;
    JTable table;
    DefaultTableModel tableModel;

    File file = new File("books.txt");

    public LibrarySystem() {
        setTitle("Library Management System");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // --- Top Panel (Role Selection) ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        roleBox = new JComboBox<>(new String[]{"ADMIN", "USER"});
        topPanel.add(new JLabel("Role:"));
        topPanel.add(roleBox);
        add(topPanel, BorderLayout.NORTH);

        // --- Center Panel (Table) ---
        tableModel = new DefaultTableModel(new String[]{"ID", "Title", "Author", "Status"}, 0);
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // --- Bottom Panel (Inputs and Buttons) ---
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        
        // Input Fields Panel
        JPanel inputPanel = new JPanel(new GridLayout(2, 4, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        idField = new JTextField();
        titleField = new JTextField();
        authorField = new JTextField();
        
        inputPanel.add(new JLabel("Book ID:"));
        inputPanel.add(idField);
        inputPanel.add(new JLabel("Title:"));
        inputPanel.add(titleField);
        inputPanel.add(new JLabel("Author:"));
        inputPanel.add(authorField);
        inputPanel.add(new JLabel("")); // Empty placeholder
        inputPanel.add(new JLabel("")); // Empty placeholder

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton addBtn = new JButton("Add Book");
        JButton viewBtn = new JButton("View All");
        JButton issueBtn = new JButton("Issue Book");
        JButton returnBtn = new JButton("Return Book");
        JButton deleteBtn = new JButton("Delete Book");

        buttonPanel.add(addBtn);
        buttonPanel.add(viewBtn);
        buttonPanel.add(issueBtn);
        buttonPanel.add(returnBtn);
        buttonPanel.add(deleteBtn);

        // Search Panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        searchField = new JTextField(15);
        JButton searchBtn = new JButton("Search by Title");
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);

        // Combine Bottom Panels
        JPanel combinedBottomPanel = new JPanel(new BorderLayout());
        combinedBottomPanel.add(inputPanel, BorderLayout.NORTH);
        combinedBottomPanel.add(buttonPanel, BorderLayout.CENTER);
        combinedBottomPanel.add(searchPanel, BorderLayout.SOUTH);

        add(combinedBottomPanel, BorderLayout.SOUTH);

        // --- Actions ---
        addBtn.addActionListener(e -> addBook());
        viewBtn.addActionListener(e -> viewBooks());
        searchBtn.addActionListener(e -> searchBook());
        issueBtn.addActionListener(e -> updateStatus("ISSUED"));
        returnBtn.addActionListener(e -> updateStatus("AVAILABLE"));
        deleteBtn.addActionListener(e -> deleteBook());

        // Load initial data
        createFileIfNotExists();
        viewBooks();

        setLocationRelativeTo(null); // Center screen
        setVisible(true);
    }

    private void createFileIfNotExists() {
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ADD BOOK
    void addBook() {
        if (!isAdmin()) {
            JOptionPane.showMessageDialog(this, "Only ADMIN can add books!", "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String id = idField.getText().trim();
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();

        if (id.isEmpty() || title.isEmpty() || author.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Check if ID already exists
        if (isBookExists(id)) {
            JOptionPane.showMessageDialog(this, "Book ID already exists!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (FileWriter fw = new FileWriter(file, true)) {
            fw.write(id + "," + title + "," + author + ",AVAILABLE\n");
            JOptionPane.showMessageDialog(this, "Book Added Successfully!");
            clearFields();
            viewBooks(); // Refresh table
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding book: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isBookExists(String id) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(id + ",")) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // VIEW BOOKS
    void viewBooks() {
        tableModel.setRowCount(0); // Clear table
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    tableModel.addRow(parts);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // SEARCH BOOK
    void searchBook() {
        String key = searchField.getText().toLowerCase().trim();
        if (key.isEmpty()) {
            viewBooks();
            return;
        }

        tableModel.setRowCount(0); // Clear table
        boolean found = false;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4 && parts[1].toLowerCase().contains(key)) {
                    tableModel.addRow(parts);
                    found = true;
                }
            }
            if (!found) {
                JOptionPane.showMessageDialog(this, "Book Not Found!", "Search Result", JOptionPane.INFORMATION_MESSAGE);
                viewBooks(); // Show all again
            }
            searchField.setText("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ISSUE / RETURN
    void updateStatus(String newStatus) {
        String id = idField.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Book ID!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean found = false;
        boolean statusChanged = false;

        try {
            File temp = new File("temp.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            PrintWriter pw = new PrintWriter(new FileWriter(temp));

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                if (parts.length == 4 && parts[0].equals(id)) {
                    found = true;
                    if (parts[3].equals(newStatus)) {
                        JOptionPane.showMessageDialog(this, "Book is already " + newStatus + "!", "Warning", JOptionPane.WARNING_MESSAGE);
                        pw.println(line);
                    } else {
                        pw.println(parts[0] + "," + parts[1] + "," + parts[2] + "," + newStatus);
                        statusChanged = true;
                    }
                } else {
                    pw.println(line);
                }
            }

            br.close();
            pw.close();

            if (found && statusChanged) {
                file.delete();
                temp.renameTo(file);
                JOptionPane.showMessageDialog(this, "Book Status Updated to: " + newStatus);
                clearFields();
                viewBooks();
            } else {
                temp.delete();
                if (!found) {
                    JOptionPane.showMessageDialog(this, "Book ID not found!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // DELETE BOOK
    void deleteBook() {
        if (!isAdmin()) {
            JOptionPane.showMessageDialog(this, "Only ADMIN can delete books!", "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String id = idField.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Book ID to delete!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete Book ID: " + id + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        boolean found = false;

        try {
            File temp = new File("temp.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            PrintWriter pw = new PrintWriter(new FileWriter(temp));

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                if (parts.length == 4 && parts[0].equals(id)) {
                    found = true;
                } else {
                    pw.println(line);
                }
            }

            br.close();
            pw.close();

            if (found) {
                file.delete();
                temp.renameTo(file);
                JOptionPane.showMessageDialog(this, "Book Deleted Successfully!");
                clearFields();
                viewBooks();
            } else {
                temp.delete();
                JOptionPane.showMessageDialog(this, "Book ID not found!", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearFields() {
        idField.setText("");
        titleField.setText("");
        authorField.setText("");
    }

    boolean isAdmin() {
        return roleBox.getSelectedItem().equals("ADMIN");
    }

    public static void main(String[] args) {
        // Set a nice look and feel if available
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        
        SwingUtilities.invokeLater(() -> new LibrarySystem());
    }
}
