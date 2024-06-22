import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatClientGUI extends JFrame {
    private JTextPane messagePane;
    private JTextField textField;
    private ChatClient client;
    private JButton exitButton;
    private JButton sendButton;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    public ChatClientGUI() {
        super("Chat Application");
        setSize(400, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Color backgroundColor = new Color(240, 240, 240);
        Color buttonColor = new Color(75, 75, 75);
        Color textColor = new Color(50, 50, 50);
        Font textFont = new Font("Arial", Font.PLAIN, 14);
        Font buttonFont = new Font("Arial", Font.BOLD, 12);

        String name = JOptionPane.showInputDialog(this, "Enter your name: ", "User Name", JOptionPane.PLAIN_MESSAGE);
        this.setTitle(name + "'s Chat Window");

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
    }
    private void onMessageReceived(String message) {
        SwingUtilities.invokeLater(() -> appendMessage(message, Color.LIGHT_GRAY));
    }
    private void sendMessage(String name) {
        String message = "[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + name + ": " + textField.getText();
        client.sendMessage(message);
        textField.setText("");
    }

    private void appendMessage(String message, Color backgroundColor) {
        try {
            StyledDocument doc = messagePane.getStyledDocument();
            Style style = messagePane.addStyle("MessageStyle", null);
            StyleConstants.setBackground(style, backgroundColor);
            StyleConstants.setForeground(style, Color.BLACK); // Message text color
            StyleConstants.setFontFamily(style, "Arial");
            StyleConstants.setFontSize(style, 14);
            doc.insertString(doc.getLength(), message + "\n", style);
            messagePane.setCaretPosition(doc.getLength()); // Scroll to the end
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ChatClientGUI().setVisible(true);
        });
    }
}