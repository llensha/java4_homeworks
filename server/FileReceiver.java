import io.netty.buffer.ByteBuf;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

public class FileReceiver {

    public static String run(ByteBuf buf) throws Exception {
        int fileNameLength = -1;
        long fileLength = -1L;
        long receivedFileLength = 0L;
        BufferedOutputStream out = null;

        System.out.println("Получение файла");

        if (buf.readableBytes() >= 4) {
            fileNameLength = buf.readInt();
            System.out.println("Получили длину имени файла: " + fileNameLength);
        }

        if (buf.readableBytes() >= fileNameLength) {
            byte[] fileName = new byte[fileNameLength];
            buf.readBytes(fileName);
            System.out.println("Получили имя файла: " + new String(fileName));
            out = new BufferedOutputStream(new FileOutputStream("server/" + new String(fileName)));
        }

        if (buf.readableBytes() >= 8) {
            fileLength = buf.readLong();
            System.out.println("Получили длину файла: " + fileLength);
        }

        while (buf.readableBytes() > 0) {
            out.write(buf.readByte());
            receivedFileLength++;
            if (fileLength == receivedFileLength) {
                System.out.println("Файл получен");
                out.close();
            }
        }

        buf.release();
        return "Получение файла завершено";
    }
    
}
