import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
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
    private static final byte OK = 0;
    private static final byte ERROR = 1;
    private byte status = -1;
    private static final byte COMMAND_AUTH = 0;
    private static final byte COMMAND_SEND_FILE = 1;
    private static final byte COMMAND_DOWNLOAD_FILE = 2;
    private static final byte COMMAND_DELETE_CLIENT_FILE = 3;
    private static final byte COMMAND_DELETE_SERVER_FILE = 4;
    private static final byte COMMAND_LIST_SERVER_FILES = 5;
    private static final byte COMMAND_EXIT = 6;

    private void run() throws Exception {

        connect();

        authorization();

        while (true) {
            System.out.print("Введите команду (1 - отправить файл, 2 - скачать файл, 3 - удалить файл с компьютера, 4 - удалить файл из облака, 5 - получить список файлов из облака, 6 - выход): ");
            int command = scanner.nextInt();
            scanner.nextLine();
            if (command == COMMAND_SEND_FILE) {
                sendFile();
            } else if (command == COMMAND_DOWNLOAD_FILE) {
                downloadFile();
            } else if (command == COMMAND_DELETE_CLIENT_FILE) {
                deleteClientFile();
            } else if (command == COMMAND_DELETE_SERVER_FILE) {
                deleteServerFile();
            } else if (command == COMMAND_LIST_SERVER_FILES) {
                getListServerFiles();
            } else if (command == COMMAND_EXIT) {
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

    private void authorization() {
        Byte answerCode = -1;
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

            try {
                ByteBuffer bufAuth = ByteBuffer.allocate(1 + 1 + 4 + usernameLength + 4 + passwordLength);
                bufAuth.put(COMMAND_AUTH);
                bufAuth.put(isNewUser);
                bufAuth.putInt(usernameLength);
                bufAuth.put(username.getBytes());
                bufAuth.putInt(passwordLength);
                bufAuth.put(password.getBytes());
                bufAuth.flip();
                byte[] bytesBufAuth = new byte[bufAuth.remaining()];
                bufAuth.get(bytesBufAuth);
                out.write(bytesBufAuth);
                bufAuth.clear();
                status = OK;
            } catch (IOException e) {
                status = ERROR;
                System.out.println("Не удалось авторизоваться");
            }

            if (status == OK) {
                try {
                    while (in.available() < 1) {
                    }
                    answerCode = in.readByte();
                    if (answerCode == OK) {
                        System.out.println("Успешная авторизация");
                    } else if (answerCode == ERROR) {
                        if (isNewUser == 1) {
                            System.out.println("Такой пользователь уже существует");
                        } else if (isNewUser == 0) {
                            System.out.println("Такой пользователь не найден");
                        }

                    }
                } catch (IOException e) {
                    System.out.println("Не удалось получить ответ");
                }
            }
        } while (answerCode != OK);

    }

    private void sendFile() {
        status = -1;
        System.out.print("Введите путь к файлу: ");
        String pathName = scanner.nextLine();
        try {
            Path path = Paths.get(pathName);
            int fileNameLength = path.getFileName().toString().getBytes().length;
            ByteBuffer bufSendFileInfo = ByteBuffer.allocate(1 + 4 + fileNameLength + 8);
            bufSendFileInfo.put(COMMAND_SEND_FILE);
            bufSendFileInfo.putInt(fileNameLength);
            bufSendFileInfo.put(path.getFileName().toString().getBytes());
            bufSendFileInfo.putLong(Files.size(path));
            bufSendFileInfo.flip();
            byte[] bytesBufSendFileInfo = new byte[bufSendFileInfo.remaining()];
            bufSendFileInfo.get(bytesBufSendFileInfo);
            byte[] bytesBufSendFileContent = Files.readAllBytes(path);
            out.write(bytesBufSendFileInfo);
            out.write(bytesBufSendFileContent);
            bufSendFileInfo.clear();
            status = OK;
        } catch (IOException e) {
            status = ERROR;
            System.out.println("Указанный файл не найден");
        }

        if (status == OK) {
            try {
                while (in.available() < 1) {
                }
                Byte answerCode = in.readByte();
                if (answerCode == OK) {
                    System.out.println("Файл отправлен");
                } else if (answerCode == ERROR) {
                    System.out.println("Не удалось отправить файл");
                }
            } catch (IOException e) {
                status = ERROR;
                System.out.println("Не удалось получить ответ");
            }
        }
    }

    private void downloadFile() {
        status = -1;
        System.out.print("Введите название файла: ");
        String fileName = scanner.nextLine();
        boolean isPathExists;
        String pathName;
        do {
            System.out.print("Введите путь к директории для сохранения файла: ");
            pathName = scanner.nextLine();
            isPathExists = Files.exists(Paths.get(pathName));
            if (!isPathExists) {
                System.out.println("Такой директории не существует");
            }
        } while (!isPathExists);

        try {
            int fileNameLength = fileName.getBytes().length;
            ByteBuffer bufDownloadFileInfo = ByteBuffer.allocate(1 + 4 + fileNameLength);
            bufDownloadFileInfo.put(COMMAND_DOWNLOAD_FILE);
            bufDownloadFileInfo.putInt(fileNameLength);
            bufDownloadFileInfo.put(fileName.getBytes());
            bufDownloadFileInfo.flip();
            byte[] bytesBufDownloadFileInfo = new byte[bufDownloadFileInfo.remaining()];
            bufDownloadFileInfo.get(bytesBufDownloadFileInfo);
            out.write(bytesBufDownloadFileInfo);
            bufDownloadFileInfo.clear();
            status = OK;
        } catch (IOException e) {
            status = ERROR;
            System.out.println("Не удалось отправить информацию на сервер");
        }

        if (status == OK) {
            try {
                while (in.available() < 1) {
                }
                Byte answerCode = in.readByte();
                if (answerCode == OK) {
                    long fileLength = in.readLong();
                    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(pathName + "/" + fileName))) {
                        long receivedFileLength = 0L;
                        while (in.available() > 0) {
                            out.write(in.readByte());
                            receivedFileLength++;
                            if (fileLength == receivedFileLength) {
                                System.out.println("Файл получен");
                            }
                        }
                        System.out.println("Цикл завершен");
                    }
                } else if (answerCode == ERROR) {
                    System.out.println("Не удалось скачать файл");
                }
            } catch (IOException e) {
                status = ERROR;
                System.out.println("Не удалось скачать файл");
            }
        }
    }

    private void deleteClientFile() {
        System.out.print("Введите путь к файлу: ");
        String pathName = scanner.nextLine();
        try {
            Path path = Paths.get(pathName);
            Files.delete(path);
            System.out.println("Файл удален");
        } catch (IOException e) {
            System.out.println("Не удалось удалить указанный файл");
        }
    }

    private void deleteServerFile() {
        status = -1;
        System.out.print("Введите название файла: ");
        String fileName = scanner.nextLine();
        try {
            int fileNameLength = fileName.getBytes().length;
            ByteBuffer bufDeleteFileInfo = ByteBuffer.allocate(1 + 4 + fileNameLength);
            bufDeleteFileInfo.put(COMMAND_DELETE_SERVER_FILE);
            bufDeleteFileInfo.putInt(fileNameLength);
            bufDeleteFileInfo.put(fileName.getBytes());
            bufDeleteFileInfo.flip();
            byte[] bytesBufDeleteFileInfo = new byte[bufDeleteFileInfo.remaining()];
            bufDeleteFileInfo.get(bytesBufDeleteFileInfo);
            out.write(bytesBufDeleteFileInfo);
            bufDeleteFileInfo.clear();
            status = OK;
        } catch (IOException e) {
            status = ERROR;
            System.out.println("Не удалось отправить информацию на сервер");
        }

        if (status == OK) {
            try {
                while (in.available() < 1) {
                }
                Byte answerCode = in.readByte();
                if (answerCode == OK) {
                    System.out.println("Файл удален");
                } else if (answerCode == ERROR) {
                    System.out.println("Не удалось удалить файл");
                }
            } catch (IOException e) {
                status = ERROR;
                System.out.println("Не удалось удалить файл");
            }
        }
    }

    private void getListServerFiles() {
        System.out.println("Пока не работает");
    }

    public static void main(String[] args) throws Exception {
        new MainClient().run();
    }

}
