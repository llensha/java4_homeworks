import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class Network {
    private static final String HOST = "localhost";
    private static final int PORT = 8585;
    private static Network ourInstance = new Network();
    private SocketChannel currentChannel;

    private Network() {
    }

    public static Network getInstance() {
        return ourInstance;
    }

    public SocketChannel getCurrentChannel() {
        return currentChannel;
    }

    public void start(CountDownLatch countDownLatch) {
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap clientBootstrap = new Bootstrap();
                clientBootstrap.group(group);
                clientBootstrap.channel(NioSocketChannel.class);
                clientBootstrap.remoteAddress(new InetSocketAddress(HOST, PORT));
                clientBootstrap.handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new StringDecoder(),
                                new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
                                        System.out.println(s);
                                    }
                                }
                                );
                        currentChannel = socketChannel;
                    }
                });
                ChannelFuture channelFuture = clientBootstrap.connect().sync();
                countDownLatch.countDown();
                channelFuture.channel().closeFuture().sync();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    group.shutdownGracefully().sync();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
    }

    public void stop() {
        currentChannel.close();
    }

}
