import java.io.*;
import java.net.Socket;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ChatClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Consumer<String> onMessageReceived;
    private BiConsumer<String, String> onFileReceived;

    public ChatClient(String address, int port, Consumer<String> onMessageReceived, BiConsumer<String, String> onFileReceived) throws IOException {
        this.socket = new Socket(address, port);
        System.out.println("Connected to the chat server");
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.onMessageReceived = onMessageReceived;
        this.onFileReceived = onFileReceived;
    }

    public void sendMessage(String msg) {
        out.println("MSG:" + msg);
    }

    public void sendFile(String senderName, File file) {
        try {
            long fileSize = file.length();
            out.println("FILE:" + senderName + ":" + file.getName() + ":" + fileSize);
            FileInputStream fis = new FileInputStream(file);
            OutputStream os = socket.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startClient() {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("MSG:")) {
                        onMessageReceived.accept(line.substring(4));
                    } else if (line.startsWith("FILE:")) {
                        String[] parts = line.split(":", 4);
                        String senderName = parts[1];
                        String fileName = parts[2];
                        int fileSize = Integer.parseInt(parts[3]);

                        File receivedFile = new File("received_" + fileName);
                        try (FileOutputStream fos = new FileOutputStream(receivedFile)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            int totalBytesRead = 0;
                            InputStream is = socket.getInputStream();
                            while (totalBytesRead < fileSize) {
                                bytesRead = is.read(buffer, 0, Math.min(buffer.length, fileSize - totalBytesRead));
                                if (bytesRead == -1) break;
                                fos.write(buffer, 0, bytesRead);
                                totalBytesRead += bytesRead;
                            }
                        }
                        onFileReceived.accept(senderName, receivedFile.getAbsolutePath());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void close() throws IOException {
        socket.close();
    }
}
