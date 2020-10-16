import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainHandler extends ChannelInboundHandlerAdapter {

    private static final List<Channel> channels = new ArrayList<>();
    private static int clientIndex = 0;
    private String clientName;
    private String username;
    private static final byte STATE_OK = 0;
    private static final byte STATE_ERROR = 1;
    private static final byte COMMAND_AUTH = 0;
    private static final byte COMMAND_UPLOAD_FILE = 1;
    private static final byte COMMAND_DOWNLOAD_FILE = 2;
    private static final byte COMMAND_DELETE_FILE = 4;
    private static final byte COMMAND_GET_LIST_FILES = 5;

    public static byte getStateOk() {
        return STATE_OK;
    }

    public static byte getStateError() {
        return STATE_ERROR;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        channels.add(ctx.channel());
        clientIndex++;
        clientName = "Клиент #" + clientIndex;
        System.out.println("Подключился " + clientName);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println(clientName + " отключился");
        channels.remove(ctx.channel());
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ByteBuf buf = ((ByteBuf) msg);
        byte command = buf.readByte();
        System.out.println("Код команды: " + command);
        if (command == COMMAND_AUTH) {
            ByteBuf bufAnswer = ByteBufAllocator.DEFAULT.directBuffer(1);
            String answer = AuthorizationService.run(buf);
            if (answer.equals(AuthorizationService.getERROR())) {
                bufAnswer.writeByte(STATE_ERROR);
            } else {
                username = answer;
                bufAnswer.writeByte(STATE_OK);
            }
            ctx.writeAndFlush(bufAnswer);
        } else if (command == COMMAND_UPLOAD_FILE) {
            ctx.writeAndFlush(FileReceiver.run(buf, username));
        } else if (command == COMMAND_DOWNLOAD_FILE) {
            ctx.writeAndFlush(FileSender.run(buf, username));
        } else if (command == COMMAND_DELETE_FILE) {
            ctx.writeAndFlush(deleteFile(buf));
        } else if (command == COMMAND_GET_LIST_FILES) {
            ctx.writeAndFlush(getListFiles(buf));
        } else {
            buf.release();
            System.out.println("Неизвестная команда");
            ctx.writeAndFlush(STATE_ERROR);
        }
    }

    private ByteBuf deleteFile(ByteBuf buf) {
        ByteBuf bufAnswer = ByteBufAllocator.DEFAULT.directBuffer(1);
        System.out.println("Удаление файла");
        int fileNameLength = buf.readInt();
        byte[] fileNameBytes = new byte[fileNameLength];
        buf.readBytes(fileNameBytes);
        String fileName = new String(fileNameBytes);
        System.out.println("Название файла: " + fileName);
        try {
            Path path = Paths.get("server", username, fileName);
            Files.delete(path);
            bufAnswer.writeByte(STATE_OK);
            System.out.println("Файл удален");
        } catch (IOException e) {
            bufAnswer.writeByte(STATE_ERROR);
            System.out.println("Файл не найден");
        }
        buf.release();
        return bufAnswer;
    }

    private ByteBuf getListFiles(ByteBuf buf) {
        ByteBuf bufAnswer = ByteBufAllocator.DEFAULT.directBuffer();
        System.out.println("Формирование списка файлов");
        try {
            int filesCount = (int) Files.list(Paths.get("server", username)).count();
            bufAnswer.writeByte(STATE_OK);
            bufAnswer.writeInt(filesCount);
            List<FileInfo> filesList = Files.list(Paths.get("server", username)).map(FileInfo::new).collect(Collectors.toList());
            for (int i = 0; i < filesCount; i++) {
                bufAnswer.writeInt(filesList.get(i).getFileName().length());
                bufAnswer.writeBytes(filesList.get(i).getFileName().getBytes());
                bufAnswer.writeLong(filesList.get(i).getLength());
            }
        } catch (IOException e) {
            bufAnswer.writeByte(STATE_ERROR);
            System.out.println("Не удалось извлечь информацию о файлах");
        }
        buf.release();
        return bufAnswer;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
