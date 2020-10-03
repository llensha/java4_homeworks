import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

public class FileHandler extends ChannelInboundHandlerAdapter {
    public enum State {
        IDLE, NAME_LENGTH, NAME, FILE_LENGTH, FILE
    }

    private State currentState = State.IDLE;
    private int fileNameLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("Получение файла");
        ByteBuf buf = ((ByteBuf) msg);
        if (currentState == State.IDLE) {
            currentState = State.NAME_LENGTH;
            System.out.println("STATE: Start file receiving");
            }

        if (currentState == State.NAME_LENGTH) {
            if (buf.readableBytes() >= 4) {
                System.out.println("STATE: Get filename length");
                fileNameLength = buf.readInt();
                currentState = State.NAME;
            }
        }

        if (currentState == State.NAME) {
            if (buf.readableBytes() >= fileNameLength) {
                byte[] fileName = new byte[fileNameLength];
                buf.readBytes(fileName);
                System.out.println("STATE: Filename received - _" + new String(fileName) + MainHandler.getClientIndex());
                out = new BufferedOutputStream(new FileOutputStream("server/" + new String(fileName) + MainHandler.getClientIndex()));
                currentState = State.FILE_LENGTH;
            }
        }

        if (currentState == State.FILE_LENGTH) {
            if (buf.readableBytes() >= 8) {
                fileLength = buf.readLong();
                System.out.println("STATE: File length received - " + fileLength);
                currentState = State.FILE;
            }
        }

        if (currentState == State.FILE) {
            while (buf.readableBytes() > 0) {
                out.write(buf.readByte());
                receivedFileLength++;
                if (fileLength == receivedFileLength) {
                    currentState = State.IDLE;
                    System.out.println("Файл получен");
                    out.close();
                }
            }
        }

        buf.release();
        ctx.writeAndFlush("Получение файла завершено");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
