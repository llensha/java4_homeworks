import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class MainClient {

    Scanner scanner = new Scanner(System.in);
    String username;

    private void run() throws Exception {
        CountDownLatch networkStarter = new CountDownLatch(1);
        new Thread(() -> Network.getInstance().start(networkStarter)).start();
        networkStarter.await();

        CountDownLatch authProcess = new CountDownLatch(1);
        authorization(future -> {
            if (!future.isSuccess()) {
                System.out.println("Ошибка авторизации");
                future.cause().printStackTrace();
            }
            if (future.isSuccess()) {
                System.out.println("Авторизация завершена");
            }
            authProcess.countDown();
        });
        authProcess.await();
        Thread.sleep(500);

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
                Network.getInstance().stop();
                break;
            }
            else {
                System.out.println("Неизвестная команда");
            }
        }
    }

    private void authorization(ChannelFutureListener finishListener) {
        System.out.print("Введите имя пользователя: ");
        username = scanner.nextLine();
        System.out.print("Введите пароль: ");
        String password = scanner.nextLine();
        int usernameLength = username.length();
        int passwordLength = password.length();
        ByteBuf bufAuth = null;
        bufAuth = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + usernameLength + 4 + passwordLength);
        bufAuth.writeByte((byte)0);
        bufAuth.writeInt(usernameLength);
        bufAuth.writeBytes(username.getBytes());
        bufAuth.writeInt(passwordLength);
        bufAuth.writeBytes(password.getBytes());
        ChannelFuture transferOperationFuture = Network.getInstance().getCurrentChannel().writeAndFlush(bufAuth);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }

    private void sendFile() {
        System.out.print("Введите путь к файлу: ");
        String pathName = scanner.nextLine();
        CountDownLatch sendFileProcess = new CountDownLatch(1);
        try {
            FileSender.sendFile(Paths.get(pathName), Network.getInstance().getCurrentChannel(), future -> {
                if (!future.isSuccess()) {
                    System.out.println("Ошибка передачи файла");
                    future.cause().printStackTrace();
                }
                if (future.isSuccess()) {
                    System.out.println("Передача файла завершена");
                }
                sendFileProcess.countDown();
            });
            sendFileProcess.await();
        } catch (IOException e) {
            throw new RuntimeException("Указанный файл не найден");
        } catch (InterruptedException e) {
            throw new RuntimeException("Ошибка ожидания окончания передачи файла");
        }
    }

    private void changeFile() {
        System.out.println("Пока не работает");
    }

    public static void main(String[] args) throws Exception {
        new MainClient().run();
    }

}
