# 🏦 Secure Banking System

A robust desktop banking application built with **Java Swing** and **MySQL**. This project simulates core banking operations with a strong focus on security, including password encryption and OTP verification.

## 🚀 Key Features

### 🔐 Security First
* **SHA-256 Hashing:** User passwords are encrypted before storage; never saved as plain text.
* **OTP Verification:** Simulates a 2-Factor Authentication (OTP) check for transfers over ₹10,000.
* **Session Management:** Securely tracks the active user session.

### 💸 Core Banking Operations
* **Account Management:** Users can register and automatically get a unique Account ID.
* **Deposits:** Add funds securely to the account.
* **Smart Transfers:**
    * **Search Directory:** Find recipients by name using the built-in search tool.
    * **Direct Transfer:** Send money instantly using Account IDs.
    * **Atomic Transactions:** Ensures money is deducted and added simultaneously to prevent data errors.

### 📊 Real-Time Dashboard
* **Live Balance:** Displays current balance in Indian Rupees (₹).
* **Transaction History:** View a detailed log of all deposits, withdrawals, and transfers with timestamps.

## 🛠️ Tech Stack

* **Language:** Java (JDK 21)
* **GUI:** Java Swing (CardLayout, JFrame, JTable)
* **Database:** MySQL
* **Connectivity:** JDBC (MySQL Connector/J)

## ⚙️ Setup Instructions

1.  **Database Setup:**
    Import the `banking_db.sql` file included in this repository into your MySQL server to create the `users`, `accounts`, and `transactions` tables.

2.  **Configuration:**
    Open `BankingApp.java` and update the database credentials on line 20:
    ```java
    private static final String PASS = "YOUR_MYSQL_PASSWORD";
    ```

3.  **Run the App:**
    Run `BankingApp.java` as a Java Application in Eclipse or your preferred IDE.

## 📸 Screenshots
*(You can upload screenshots of your Login screen and Dashboard here)*

## 📜 License
This project is for educational purposes as part of a Major Project submission.
