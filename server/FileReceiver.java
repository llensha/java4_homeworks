import io.netty.buffer.ByteBuf;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

public class FileReceiver {

    public static String run(ByteBuf buf) throws Exception {
        int fileNameLength = -1;
        long fileLength = -1L;
        long receivedFileLength = 0L;

        System.out.println("Получение файла");
        Thread.sleep(500);

        if (buf.readableBytes() >= 4) {
            fileNameLength = buf.readInt();
            System.out.println("Получили длину имени файла: " + fileNameLength);
        }

        byte[] fileName = new byte[fileNameLength];

        if (buf.readableBytes() >= fileNameLength) {
            buf.readBytes(fileName);
            System.out.println("Получили имя файла: " + new String(fileName));
        }

        if (buf.readableBytes() >= 8) {
            fileLength = buf.readLong();
            System.out.println("Получили длину файла: " + fileLength);
        }

        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("server/" + new String(fileName)));) {
            while (buf.readableBytes() > 0) {
                out.write(buf.readByte());
                receivedFileLength++;
                if (fileLength == receivedFileLength) {
                    System.out.println("Файл получен");
                }
            }
            System.out.println("Цикл завершен");
        }

        buf.release();
        return "Получение файла завершено";
    }
    
}
