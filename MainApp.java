import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MainApp {
    public static void main(String[] args) {
        try(ServerSocket serverSocket = new ServerSocket(8085)) {
            System.out.println("Server is listening");
            try(Socket socket = serverSocket.accept(); BufferedInputStream in = new BufferedInputStream(socket.getInputStream())) {
                File file = new File("server/file.txt");
                file.createNewFile();
                FileOutputStream out = new FileOutputStream(file);
                byte[] bytes = new byte[256];
                int x;
                while ((x = in.read(bytes)) != -1) {
                    out.write(bytes, 0, x);
                }
                System.out.println("File received from Client");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
