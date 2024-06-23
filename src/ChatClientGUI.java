import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatClientGUI extends JFrame {
    private JTextPane messagePane;
    private JTextField textField;
    private ChatClient client;
    private JButton exitButton;
    private JButton sendButton;
    private JButton fileButton; // New button for file sending
    private String name;
    private DatabaseManager dbManager;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public ChatClientGUI() {
        super("Chat Application");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        try {
            dbManager = new DatabaseManager();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database connection error", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }

        if (!authenticateUser()) {
            JOptionPane.showMessageDialog(this, "Authentication failed. Exiting application.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        initializeGUI();
    }

    private boolean authenticateUser() {
        while (true) {
            Object[] options = {"Login", "Register", "Exit"};
            int choice = JOptionPane.showOptionDialog(this, "Select an option", "Welcome",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);

            if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
                return false; // User chose to exit
            }

            JPanel panel = new JPanel(new GridLayout(3, 2));
            JLabel userLabel = new JLabel("Username:");
            JTextField userField = new JTextField();
            JLabel passLabel = new JLabel("Password:");
            JPasswordField passField = new JPasswordField();
            panel.add(userLabel);
            panel.add(userField);
            panel.add(passLabel);
            panel.add(passField);

            String title = choice == JOptionPane.YES_OPTION ? "Login" : "Register";
            int result = JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                String username = userField.getText();
                String password = new String(passField.getPassword());

                try {
                    if (choice == JOptionPane.YES_OPTION) {
                        if (dbManager.authenticateUser(username, password)) {
                            name = username;
                            return true;
                        } else {
                            JOptionPane.showMessageDialog(this, "Invalid credentials. Try again.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } else if (choice == JOptionPane.NO_OPTION) {
                        if (dbManager.registerUser(username, password)) {
                            JOptionPane.showMessageDialog(this, "Registration successful. You can now log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this, "Username already taken. Try again.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Database error. Try again.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void initializeGUI() {
        Color backgroundColor = new Color(240, 240, 240);
        Color buttonColor = new Color(75, 75, 75);
        Font textFont = new Font("Arial", Font.PLAIN, 14);
        Font buttonFont = new Font("Arial", Font.BOLD, 12);

        this.setTitle(name + "'s Chat Window");

        messagePane = new JTextPane();
        messagePane.setEditable(false);
        messagePane.setBackground(backgroundColor);
        messagePane.setFont(textFont);

        JScrollPane scrollPane = new JScrollPane(messagePane);
        add(scrollPane, BorderLayout.CENTER);

        exitButton = new JButton("Exit");
        exitButton.setFont(buttonFont);
        exitButton.setBackground(buttonColor);
        exitButton.setForeground(Color.WHITE);
        exitButton.addActionListener(e -> {
            String exitMessage = name + " has left the chat";
            client.sendMessage(exitMessage);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            System.exit(0);
        });

        sendButton = new JButton("Send");
        sendButton.setFont(buttonFont);
        sendButton.setBackground(buttonColor);
        sendButton.setForeground(Color.WHITE);
        sendButton.addActionListener(e -> sendMessage());

        fileButton = new JButton("Send File");
        fileButton.setFont(buttonFont);
        fileButton.setBackground(buttonColor);
        fileButton.setForeground(Color.WHITE);
        fileButton.addActionListener(e -> sendFile());

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(backgroundColor);
        textField = new JTextField();
        textField.setFont(textFont);
        textField.setBackground(backgroundColor);
        textField.addActionListener(e -> sendMessage()); // Also handle send on enter key

        // Adding components to bottom panel
        bottomPanel.add(textField, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3));
        buttonPanel.add(sendButton);
        buttonPanel.add(fileButton);
        buttonPanel.add(exitButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        try {
            this.client = new ChatClient("127.0.0.1", 4040, this::onMessageReceived, this::onFileReceived);
            client.startClient();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error connecting to the server", "Connection error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        loadChatHistory();
    }

    private void loadChatHistory() {
        try {
            ResultSet chatHistory = dbManager.loadChatHistory();
            while (chatHistory.next()) {
                String sender = chatHistory.getString("sender");
                String message = chatHistory.getString("message");
                String timestamp = chatHistory.getTimestamp("timestamp").toString();
                appendMessage("[" + timestamp + "] " + sender + ": " + message,
                        sender.equals(name) ? new Color(153, 255, 51) : Color.PINK,
                        new Color(255, 255, 255)
                        );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void onMessageReceived(String message) {
        SwingUtilities.invokeLater(() -> {
            String senderName = extractSenderName(message);
            Color textColor = Color.WHITE;
            Color backgroundColor = senderName.equals(name) ? new Color(153, 255, 51) : Color.PINK;
            appendMessage(message, backgroundColor, textColor);
        });
    }

    private void onFileReceived(String senderName, String filePath) {
        SwingUtilities.invokeLater(() -> {
            String fileName = new File(filePath).getName();
            appendMessage("[" + dateFormat.format(new Date()) + "] " + senderName + " sent a file: " + fileName,
                    new Color(255, 255, 255), Color.CYAN);

            String userHome = System.getProperty("user.home");
            File saveDir = new File(userHome, "received_files");
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            File destinationFile = new File(saveDir, fileName);

            try (FileInputStream fis = new FileInputStream(filePath);
                 FileOutputStream fos = new FileOutputStream(destinationFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                JOptionPane.showMessageDialog(this, "File saved: " + destinationFile.getAbsolutePath(), "File Received", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error saving file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void sendMessage() {
        String message = "[" + dateFormat.format(new Date()) + "] " + name + ": " + textField.getText();
        try {
            dbManager.saveMessage(name, textField.getText());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        client.sendMessage(message);
        textField.setText("");
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            client.sendFile(name, file);
        }
    }

    private void appendMessage(String message, Color backgroundColor, Color textColor) {
        try {
            StyledDocument doc = messagePane.getStyledDocument();
            Style style = messagePane.addStyle("MessageStyle", null);
            StyleConstants.setBackground(style, backgroundColor);
            StyleConstants.setForeground(style, textColor);
            StyleConstants.setFontFamily(style, "Arial");
            StyleConstants.setFontSize(style, 14);
            doc.insertString(doc.getLength(), message + "\n", style);
            messagePane.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private String extractSenderName(String message) {
        int startIndex = message.indexOf("] ") + 2;
        int endIndex = message.indexOf(": ");
        if (startIndex > 0 && endIndex > startIndex) {
            return message.substring(startIndex, endIndex);
        }
        return "";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ChatClientGUI().setVisible(true);
        });
    }
}
