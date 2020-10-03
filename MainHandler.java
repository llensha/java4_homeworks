import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainHandler extends ChannelInboundHandlerAdapter {

    private enum DataType {
        EMPTY((byte)-1), AUTH((byte)0), FILE((byte)1), COMMAND((byte)2);

        byte firstMessageByte;

        DataType(byte firstMessageByte) {
            this.firstMessageByte = firstMessageByte;
        }

        static DataType getDataTypeFromByte(byte b) {
            if (b == AUTH.firstMessageByte) {
                return AUTH;
            }
            if (b == FILE.firstMessageByte) {
                return FILE;
            }
            if (b == COMMAND.firstMessageByte) {
                return COMMAND;
            }
            return EMPTY;
        }
    }

    private DataType type = DataType.EMPTY;

    private static final List<Channel> channels = new ArrayList<>();
    private static int clientIndex = 0;
    private static String clientName;

    public static int getClientIndex() {
        return clientIndex;
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
        byte firstByte = buf.readByte();
        type = DataType.getDataTypeFromByte(firstByte);
        System.out.println(type);
        if(type == DataType.AUTH) {
            ctx.channel().pipeline().addLast(new AuthHandler());
        } else if (type == DataType.FILE) {
            ctx.channel().pipeline().addLast(new FileHandler());
        } else if (type == DataType.COMMAND) {
            ctx.channel().pipeline().addLast(new CommandHandler());
        } else {
            ctx.writeAndFlush("Неизвестная команда");
        }
        ctx.fireChannelRead(buf);
        //buf.release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
