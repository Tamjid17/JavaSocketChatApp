import java.sql.*;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/ChatApp";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private Connection connection;

    public DatabaseManager() throws SQLException {
        connection = DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public boolean authenticateUser(String username, String password) throws SQLException {
        String query = "SELECT * FROM Users WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    public void saveMessage(String sender, String message) throws SQLException {
        String query = "INSERT INTO Messages (sender, message) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, sender);
            stmt.setString(2, message);
            stmt.executeUpdate();
        }
    }

    public ResultSet loadChatHistory() throws SQLException {
        String query = "SELECT * FROM Messages ORDER BY timestamp";
        return connection.createStatement().executeQuery(query);
    }
}
