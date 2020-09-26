import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class MainClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 8085)) {
            try (FileInputStream in = new FileInputStream("client/file.txt")) {
                byte[] bytes = new byte[256];
                int x;
                while ((x = in.read(bytes)) != -1) {
                    socket.getOutputStream().write(bytes);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
