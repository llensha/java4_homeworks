import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSender {

    public static void sendFile(Path path, Channel channel, ChannelFutureListener finishListener) throws IOException {

        ByteBuf buf;

        byte[] filenameBytes = path.getFileName().toString().getBytes();

        buf = ByteBufAllocator.DEFAULT.directBuffer();
        buf.writeByte((byte) 1);
        buf.writeInt(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        buf.writeLong(Files.size(path));
        byte[] buffer = new byte[1024];
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(path.toFile()))) {
            int n;
            while ((n = in.read(buffer)) != -1) {
                buf.writeBytes(buffer, 0, n);
            }
        }

        ChannelFuture transferOperationFuture = channel.writeAndFlush(buf);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }

    }
}