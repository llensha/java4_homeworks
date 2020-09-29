package java_nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;

public class NioChatClientExample implements Runnable {
    private SocketChannel socketChannel;
    private Selector select;
    private ByteBuffer buffer = ByteBuffer.allocate(256);
    Scanner scanner = new Scanner(System.in);
    boolean isExit = false;

    public NioChatClientExample() throws IOException {
        this.socketChannel = SocketChannel.open(new InetSocketAddress(8189));
        this.socketChannel.configureBlocking(false);
        this.select = Selector.open();
        this.socketChannel.register(select, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    @Override
    public void run() {
        try {
            Iterator<SelectionKey> iter;
            SelectionKey key;
            do {
                select.select();
                iter = this.select.selectedKeys().iterator();
                while (iter.hasNext()) {
                    key = iter.next();
                    iter.remove();
                    if(key.isReadable()) this.handleRead(key);
                    if(key.isWritable()) this.handleWrite(key);
                }
            } while (!isExit);
            Thread.sleep(100);
            socketChannel.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        StringBuilder sb = new StringBuilder();
        buffer.clear();
        int read = 0;
        while ((read = socketChannel.read(buffer)) > 0) {
            buffer.flip();
            byte[] bytes = new byte[buffer.limit()];
            buffer.get(bytes);
            sb.append(new String(bytes));
            buffer.clear();
            System.out.println(sb.toString());
        }
    }

    private void handleWrite(SelectionKey key) throws IOException, InterruptedException {
        buffer.clear();
        System.out.print("Введите сообщение (для выхода введите '/exit'): ");
        String message = scanner.nextLine();
        if (message.equalsIgnoreCase("/exit")) isExit = true;
        buffer.put(message.getBytes());
        buffer.flip();
        socketChannel.write(buffer);
        Thread.sleep(100);
    }

    public static void main(String[] args) throws IOException {
        new Thread(new NioChatClientExample()).start();
    }

}


