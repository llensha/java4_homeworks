import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainHandler extends ChannelInboundHandlerAdapter {

    private static final List<Channel> channels = new ArrayList<>();
    private static int clientIndex = 0;
    private String clientName;

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
        if (firstByte == 0) {
            ctx.writeAndFlush(Authorization.run(buf));
        } else if (firstByte == 1) {
            ctx.writeAndFlush(FileReceiver.run(buf));
        } else if (firstByte == 2) {
            ctx.writeAndFlush(CommandReceiver.run(buf));
        } else {
            buf.release();
            System.out.println("Неизвестная команда");
            ctx.writeAndFlush("Неизвестная команда");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
