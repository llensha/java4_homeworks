import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSender {
    public static ByteBuf run(ByteBuf buf, String username){

        ByteBuf bufAnswer = ByteBufAllocator.DEFAULT.directBuffer();

        System.out.println("Скачивание файла");
        int fileNameLength = buf.readInt();
        byte[] fileNameBytes = new byte[fileNameLength];
        buf.readBytes(fileNameBytes);
        String fileName = new String(fileNameBytes);
        System.out.println("Название файла: " + fileName);

        try {
            Path path = Paths.get("server", username, fileName);
            long fileLength = Files.size(path);
            bufAnswer.writeByte(MainHandler.getStateOk());
            bufAnswer.writeLong(fileLength);
            bufAnswer.writeBytes(Files.readAllBytes(path));
            System.out.println("Файл отправлен");
        } catch (IOException e) {
            bufAnswer.writeByte(MainHandler.getStateError());
            System.out.println("Файл не найден");
        }

        buf.release();

        return bufAnswer;
    }
}
