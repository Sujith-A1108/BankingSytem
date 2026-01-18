package banking;

import java.sql.*;
import java.security.MessageDigest;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Random;

public class BankingApp extends JFrame {

    // ==========================================
    // 1. CONFIGURATION & VARIABLES
    // ==========================================
    private static final String URL = "jdbc:mysql://localhost:3306/banking_db";
    private static final String USER = "root";
    private static final String PASS = "Sujith@123"; // <--- CHECK YOUR PASSWORD HERE

    // Session Data
    private static int currentUserId = -1;
    private static int currentAccountId = -1;
    private static String currentUserFullName = "";

    // GUI Components
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    
    // Dashboard Components (Global so we can refresh them)
    // Updated to use Rupee Symbol (\u20B9)
    private JLabel balanceLabel = new JLabel("Balance: \u20B9 0.00");
    private DefaultTableModel historyModel = new DefaultTableModel(new String[]{"ID", "Type", "Amount", "Date", "Desc"}, 0);

    public BankingApp() {
        setTitle("Secure Banking Application");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Add Screens
        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createRegisterPanel(), "REGISTER");
        mainPanel.add(createDashboardPanel(), "DASHBOARD");

        add(mainPanel);
        setVisible(true);
    }

    // ==========================================
    // 2. SECURITY METHODS
    // ==========================================
    
    // Hash passwords (SHA-256)
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) hexString.append(String.format("%02x", b));
            return hexString.toString();
        } catch (Exception e) { return null; }
    }

    // Simulate OTP (Triggered for transfers > 10,000)
    private boolean verifyOTP() {
        int otp = new Random().nextInt(9000) + 1000; 
        String input = JOptionPane.showInputDialog(null, 
            "SECURITY CHECK:\nHigh value transaction detected.\nOTP sent to mobile.\n\n[SIMULATION] Your OTP is: " + otp + "\n\nEnter OTP:");
        
        return input != null && input.equals(String.valueOf(otp));
    }

    // ==========================================
    // 3. DATABASE METHODS
    // ==========================================
    
    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    private boolean loginUser(String username, String password) {
        try (Connection conn = connect()) {
            String sql = "SELECT u.user_id, u.full_name, a.account_number FROM users u " +
                         "JOIN accounts a ON u.user_id = a.user_id " +
                         "WHERE u.username = ? AND u.password_hash = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, hashPassword(password)); // Check hashed password
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                currentUserId = rs.getInt("user_id");
                currentUserFullName = rs.getString("full_name");
                currentAccountId = rs.getInt("account_number");
                return true;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    private void registerUser(String name, String user, String pass, String phone) {
        try (Connection conn = connect()) {
            // 1. Insert User
            String sqlUser = "INSERT INTO users (full_name, username, password_hash, phone) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, name); pstmt.setString(2, user);
            pstmt.setString(3, hashPassword(pass)); 
            pstmt.setString(4, phone);
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int newUserId = rs.getInt(1);
                // 2. Create Account
                PreparedStatement pstmtAcc = conn.prepareStatement("INSERT INTO accounts (user_id, balance) VALUES (?, 0.00)");
                pstmtAcc.setInt(1, newUserId);
                pstmtAcc.executeUpdate();
                JOptionPane.showMessageDialog(null, "Registration Successful! Please Login.");
                cardLayout.show(mainPanel, "LOGIN");
            }
        } catch (Exception e) { JOptionPane.showMessageDialog(null, "Error: Username might be taken."); }
    }

    private void performTransaction(String type, double amount, int targetAccount) {
        try (Connection conn = connect()) {
            // Security Check
            if (amount > 10000 && !verifyOTP()) {
                JOptionPane.showMessageDialog(null, "OTP Verification Failed!");
                return;
            }

            if (type.equals("DEPOSIT")) {
                conn.prepareStatement("UPDATE accounts SET balance = balance + " + amount + " WHERE account_number = " + currentAccountId).executeUpdate();
                logTransaction(conn, currentAccountId, "DEPOSIT", amount, "Cash Deposit");
            
            } else if (type.equals("TRANSFER")) {
                // Check balance
                ResultSet rs = conn.prepareStatement("SELECT balance FROM accounts WHERE account_number = " + currentAccountId).executeQuery();
                if (rs.next() && rs.getDouble("balance") >= amount) {
                    // Deduct from Sender
                    conn.prepareStatement("UPDATE accounts SET balance = balance - " + amount + " WHERE account_number = " + currentAccountId).executeUpdate();
                    // Add to Receiver
                    conn.prepareStatement("UPDATE accounts SET balance = balance + " + amount + " WHERE account_number = " + targetAccount).executeUpdate();
                    
                    logTransaction(conn, currentAccountId, "TRANSFER_SENT", amount, "To Acc: " + targetAccount);
                    logTransaction(conn, targetAccount, "TRANSFER_RCVD", amount, "From Acc: " + currentAccountId);
                } else {
                    JOptionPane.showMessageDialog(null, "Insufficient Funds!");
                    return;
                }
            }
            JOptionPane.showMessageDialog(null, "Transaction Successful!");
        } catch (Exception e) { JOptionPane.showMessageDialog(null, "Transaction Failed: " + e.getMessage()); }
    }

    private void logTransaction(Connection conn, int accId, String type, double amount, String desc) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO transactions (account_id, type, amount, description) VALUES (?, ?, ?, ?)");
        pstmt.setInt(1, accId); pstmt.setString(2, type); pstmt.setDouble(3, amount); pstmt.setString(4, desc);
        pstmt.executeUpdate();
    }

    // ==========================================
    // 4. GUI SCREENS
    // ==========================================

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(50, 250, 50, 250));
        
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JButton loginBtn = new JButton("Secure Login");
        JButton regBtn = new JButton("Register New Account");

        panel.add(new JLabel("Username:")); panel.add(userField);
        panel.add(new JLabel("Password:")); panel.add(passField);
        panel.add(loginBtn); panel.add(regBtn);

        loginBtn.addActionListener(e -> {
            if (loginUser(userField.getText(), new String(passField.getPassword()))) {
                cardLayout.show(mainPanel, "DASHBOARD");
                refreshDashboard();
            } else {
                JOptionPane.showMessageDialog(null, "Invalid Credentials!");
            }
        });

        regBtn.addActionListener(e -> cardLayout.show(mainPanel, "REGISTER"));
        return panel;
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(50, 200, 50, 200));

        JTextField nameF = new JTextField(), userF = new JTextField(), phoneF = new JTextField();
        JPasswordField passF = new JPasswordField();
        JButton submitBtn = new JButton("Create Account");
        JButton backBtn = new JButton("Back to Login");

        panel.add(new JLabel("Full Name:")); panel.add(nameF);
        panel.add(new JLabel("Username:")); panel.add(userF);
        panel.add(new JLabel("Password:")); panel.add(passF);
        panel.add(new JLabel("Phone:")); panel.add(phoneF);
        panel.add(backBtn); panel.add(submitBtn);

        submitBtn.addActionListener(e -> registerUser(nameF.getText(), userF.getText(), new String(passF.getPassword()), phoneF.getText()));
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));
        return panel;
    }

    // --- DASHBOARD ---
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Top Bar (Fixed Layout Error)
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); 
        topPanel.add(new JLabel("<html><h1>Banking Dashboard</h1></html>"));
        JButton logoutBtn = new JButton("Logout");
        topPanel.add(logoutBtn);
        panel.add(topPanel, BorderLayout.NORTH);

        // Center Info
        JPanel centerPanel = new JPanel(new GridLayout(2, 1));
        
        // Balance & Actions
        JPanel actionPanel = new JPanel(new GridLayout(3, 2, 10, 10)); 
        actionPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
        actionPanel.add(balanceLabel);
        actionPanel.add(new JLabel("")); // Spacer

        JButton depositBtn = new JButton("Deposit Money");
        JButton transferBtn = new JButton("Transfer Funds");
        JButton btnFindUser = new JButton("Find Users (Get IDs)"); 
        
        actionPanel.add(depositBtn);
        actionPanel.add(transferBtn);
        actionPanel.add(btnFindUser); 
        actionPanel.add(new JLabel("")); 
        
        centerPanel.add(actionPanel);
        centerPanel.add(new JScrollPane(new JTable(historyModel)));
        panel.add(centerPanel, BorderLayout.CENTER);

        // --- BUTTON ACTIONS ---
        logoutBtn.addActionListener(e -> {
            currentUserId = -1; 
            cardLayout.show(mainPanel, "LOGIN");
        });

        depositBtn.addActionListener(e -> {
            String amt = JOptionPane.showInputDialog("Enter Amount to Deposit:");
            if (amt != null && !amt.trim().isEmpty()) {
                try {
                    performTransaction("DEPOSIT", Double.parseDouble(amt), 0);
                    refreshDashboard();
                } catch (Exception ex) { JOptionPane.showMessageDialog(null, "Invalid Amount"); }
            }
        });

        // FIXED TRANSFER LOGIC (Search + Cancel Safety)
        transferBtn.addActionListener(e -> {
            String[] options = {"Search by Name", "Enter ID Directly"};
            int choice = JOptionPane.showOptionDialog(null, 
                    "Do you know the Recipient's Account ID?", 
                    "Transfer Funds", 
                    JOptionPane.DEFAULT_OPTION, 
                    JOptionPane.QUESTION_MESSAGE, 
                    null, options, options[0]);

            if (choice == JOptionPane.CLOSED_OPTION) return; // Stop if closed

            String targetId = null;

            if (choice == 0) { // Search by Name
                String searchName = JOptionPane.showInputDialog("Enter Name to Search:");
                if (searchName == null || searchName.trim().isEmpty()) return; // Cancelled

                try (Connection conn = connect()) {
                    String sql = "SELECT u.full_name, a.account_number FROM users u " +
                                 "JOIN accounts a ON u.user_id = a.user_id " +
                                 "WHERE u.full_name LIKE ? OR u.username LIKE ?";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, "%" + searchName + "%");
                    pstmt.setString(2, "%" + searchName + "%");
                    ResultSet rs = pstmt.executeQuery();

                    DefaultTableModel model = new DefaultTableModel(new String[]{"Name", "Account ID"}, 0);
                    boolean found = false;
                    while (rs.next()) {
                        model.addRow(new Object[]{rs.getString("full_name"), rs.getInt("account_number")});
                        found = true;
                    }

                    if (found) {
                        JTable table = new JTable(model);
                        JOptionPane.showMessageDialog(null, new JScrollPane(table), "Search Results", JOptionPane.INFORMATION_MESSAGE);
                        targetId = JOptionPane.showInputDialog("Enter the Account ID from the list above:");
                    } else {
                        JOptionPane.showMessageDialog(null, "No user found with that name.");
                        return;
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            } 
            else if (choice == 1) { // Enter ID Directly
                targetId = JOptionPane.showInputDialog("Enter Recipient Account ID:");
            }

            // CHECK: If ID is missing or Cancelled, STOP.
            if (targetId == null || targetId.trim().isEmpty()) return;

            String amt = JOptionPane.showInputDialog("Enter Amount to Transfer:");
            if (amt == null || amt.trim().isEmpty()) return; // Cancelled

            try {
                performTransaction("TRANSFER", Double.parseDouble(amt), Integer.parseInt(targetId));
                refreshDashboard();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Invalid Amount or ID!");
            }
        });
        
        // FIND USER BUTTON
        btnFindUser.addActionListener(e -> {
            String searchName = JOptionPane.showInputDialog("Enter Name to Search:");
            if (searchName != null && !searchName.trim().isEmpty()) {
                try (Connection conn = connect()) {
                    String sql = "SELECT u.full_name, a.account_number FROM users u " +
                                 "JOIN accounts a ON u.user_id = a.user_id " +
                                 "WHERE u.full_name LIKE ? OR u.username LIKE ?";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, "%" + searchName + "%");
                    pstmt.setString(2, "%" + searchName + "%");
                    ResultSet rs = pstmt.executeQuery();
                    
                    DefaultTableModel model = new DefaultTableModel(new String[]{"Name", "Account ID"}, 0);
                    boolean found = false;
                    while (rs.next()) {
                        model.addRow(new Object[]{rs.getString("full_name"), rs.getInt("account_number")});
                        found = true;
                    }
                    
                    if (found) {
                        JTable table = new JTable(model);
                        JOptionPane.showMessageDialog(null, new JScrollPane(table), "Search Results", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(null, "No user found.");
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        return panel;
    }

    private void refreshDashboard() {
        try (Connection conn = connect()) {
            // Update Balance (Showing Rupee Symbol)
            ResultSet rs = conn.prepareStatement("SELECT balance FROM accounts WHERE account_number = " + currentAccountId).executeQuery();
            if (rs.next()) {
                balanceLabel.setText("<html><h2>Account #" + currentAccountId + " | Balance: \u20B9 " + rs.getDouble("balance") + "</h2></html>");
            }

            // Update History
            historyModel.setRowCount(0);
            ResultSet rsHist = conn.prepareStatement("SELECT * FROM transactions WHERE account_id = " + currentAccountId + " ORDER BY timestamp DESC").executeQuery();
            while (rsHist.next()) {
                historyModel.addRow(new Object[]{
                    rsHist.getInt("trans_id"), rsHist.getString("type"), 
                    rsHist.getDouble("amount"), rsHist.getString("timestamp"), rsHist.getString("description")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BankingApp());
    }
}