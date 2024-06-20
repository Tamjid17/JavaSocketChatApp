import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {
    private static List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serversocket = new ServerSocket(4040);
        System.out.println("Server is running waiting for clients");
        while(true){
            Socket clientSocket = serversocket.accept();
            System.out.println("Client connnected: " +clientSocket);
            ClientHandler clientthread = new ClientHandler(clientSocket, clients);
            clients.add(clientthread);
            new Thread(clientthread).start();
        }
    }
}
