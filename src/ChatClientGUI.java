import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    private String name;
    private DatabaseManager dbManager;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public ChatClientGUI() {
        super("Chat Application");
        setSize(400, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);


        try {
            dbManager = new DatabaseManager();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database connection error", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return; // Exit if unable to connect to the database
        }

        while (true) {
            name = JOptionPane.showInputDialog(this, "Enter your username: ", "Login", JOptionPane.PLAIN_MESSAGE);
            String password = JOptionPane.showInputDialog(this, "Enter your password: ", "Login", JOptionPane.PLAIN_MESSAGE);
            try {
                if (dbManager.authenticateUser(name, password)) {
                    break;
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid username or password", "Login failed", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        this.setTitle(name + "'s Chat Window");

//        try {
//            ResultSet chatHistory = dbManager.loadChatHistory();
//            while (chatHistory.next()) {
//                String sender = chatHistory.getString("sender");
//                String message = chatHistory.getString("message");
//                String timestamp = chatHistory.getTimestamp("timestamp").toString();
//                appendMessage("[" + timestamp + "] " + sender + ": " + message,
//                        sender.equals(name) ? new Color(255, 255, 204) : new Color(204, 229, 255),
//                        sender.equals(name) ? Color.YELLOW : Color.BLUE);
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }

        Color backgroundColor = new Color(240, 240, 240);
        Color buttonColor = new Color(75, 75, 75);
        Color textColor = new Color(50, 50, 50);
        Font textFont = new Font("Arial", Font.PLAIN, 14);
        Font buttonFont = new Font("Arial", Font.BOLD, 12);

        //name = JOptionPane.showInputDialog(this, "Enter your name: ", "User Name", JOptionPane.PLAIN_MESSAGE);

        messagePane = new JTextPane();
        messagePane.setEditable(false);
        messagePane.setBackground(backgroundColor);
        messagePane.setForeground(textColor);
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
        sendButton.addActionListener(e -> sendMessage(name));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(backgroundColor);
        textField = new JTextField();
        textField.setFont(textFont);
        textField.setForeground(textColor);
        textField.setBackground(backgroundColor);
        textField.addActionListener(e -> sendMessage(name)); // Also handle send on enter key

        // Adding components to bottom panel
        bottomPanel.add(textField, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(sendButton);
        buttonPanel.add(exitButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        try {
            this.client = new ChatClient("127.0.0.1", 4040, this::onMessageReceived);
            client.startClient();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error connecting to the server", "Connection error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        try {
            ResultSet chatHistory = dbManager.loadChatHistory();
            while (chatHistory.next()) {
                String sender = chatHistory.getString("sender");
                String message = chatHistory.getString("message");
                String timestamp = chatHistory.getTimestamp("timestamp").toString();
                appendMessage("[" + timestamp + "] " + sender + ": " + message,
                        sender.equals(name) ? new Color(255, 255, 204) : new Color(204, 229, 255),
                        sender.equals(name) ? Color.YELLOW : Color.BLUE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void onMessageReceived(String message) {
        SwingUtilities.invokeLater(() -> {
            String senderName = extractSenderName(message);
            Color textcolor = Color.WHITE;
            Color backgroundColor = senderName.equals(name) ? new Color(153, 255, 51) : Color.PINK;
            appendMessage(message, backgroundColor, textcolor);
        });
    }
    private void sendMessage(String name) {
        String message = "[" + dateFormat.format(new Date()) + "] " + name + ": " + textField.getText();
        try {
            dbManager.saveMessage(name, textField.getText());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        client.sendMessage(message);
        textField.setText("");
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
            messagePane.setCaretPosition(doc.getLength()); // Scroll to the end
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