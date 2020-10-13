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


    public static byte getStateOk() {
        return STATE_OK;
    }

    public static byte getStateError() {
        return STATE_ERROR;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channels.add(ctx.channel());
        clientIndex++;
        clientName = "Клиент #" + clientIndex;
        System.out.println("Подключился " + clientName);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
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
        } else {
            buf.release();
            System.out.println("Неизвестная команда");
            ctx.writeAndFlush(1);
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
