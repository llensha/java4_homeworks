import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

public class FileReceiver {

    public static ByteBuf run(ByteBuf buf, String username) {

        int fileNameLength = -1;
        long fileLength = -1L;
        long receivedFileLength = 0L;
        ByteBuf bufAnswer = ByteBufAllocator.DEFAULT.directBuffer();

        System.out.println("Получение файла");

        try {
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

            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("server/" + username + "/" + new String(fileName)))) {
                while (buf.readableBytes() > 0) {
                    out.write(buf.readByte());
                    receivedFileLength++;
                    if (fileLength == receivedFileLength) {
                        System.out.println("Файл получен");
                    }
                }
            }
            bufAnswer.writeByte(MainHandler.getStateOk());
        } catch (Exception e) {
            bufAnswer.writeByte(MainHandler.getStateError());
            System.out.println("Не удалось получить файл");
        }

        buf.release();

        return bufAnswer;
    }
    
}
