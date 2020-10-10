import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class MainClient {

    Socket socket;
    private static final String HOST = "localhost";
    private static final int PORT = 8585;
    Scanner scanner = new Scanner(System.in);
    String username;
    DataInputStream in;
    DataOutputStream out;


    private void run() throws Exception {

        connect();

        authorization();

        while (true) {
            System.out.print("Введите команду (1 - отправить файл, 2 - действие с файлом, 3 - выход): ");
            int command = scanner.nextInt();
            scanner.nextLine();
            if (command == 1) {
                sendFile();
                Thread.sleep(500);
            } else if (command == 2) {
                changeFile();
            } else if (command == 3) {
                in.close();
                out.close();
                socket.close();
                break;
            }
            else {
                System.out.println("Неизвестная команда");
            }
        }
    }

    private void connect() {
        try {
            socket = new Socket(HOST, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void authorization() throws InterruptedException, IOException {
        String answer = "";
        do {
            System.out.print("Вы новый пользователь? (1 - Да, 0 - Нет): ");
            byte isNewUser = scanner.nextByte();
            scanner.nextLine();
            System.out.print("Введите имя пользователя: ");
            username = scanner.nextLine();
            System.out.print("Введите пароль: ");
            String password = scanner.nextLine();
            int usernameLength = username.length();
            int passwordLength = password.length();

            out.writeByte(0);
            out.writeByte(isNewUser);
            out.writeInt(usernameLength);
            out.write(username.getBytes());
            out.writeInt(passwordLength);
            out.write(password.getBytes());

            Thread.sleep(500);

            int sizeBuffer = in.available();
            byte[] buffer = new byte[sizeBuffer];
            in.read(buffer);
            answer = new String(buffer);
            System.out.println(answer);

        } while (!answer.startsWith("Успешная авторизация"));

    }

    private void sendFile() {
        System.out.print("Введите путь к файлу: ");
        String pathName = scanner.nextLine();
        Path path = Paths.get(pathName);
        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            byte[] filenameBytes = path.getFileName().toString().getBytes();
            out.writeByte(1);
            out.writeInt(filenameBytes.length);
            out.write(filenameBytes);
            out.writeLong(Files.size(path));
            byte[] buffer = new byte[1024];
            try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(path.toFile()))) {
                int n;
                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                }
            }
        } catch (IOException e) {
            System.out.println("Указанный файл не найден");
        }
    }

    private void changeFile() {
        System.out.println("Пока не работает");
    }

    public static void main(String[] args) throws Exception {
        new MainClient().run();
    }

}
