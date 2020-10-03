import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

public class MainClient {
    public static void main(String[] args) throws Exception {
        CountDownLatch networkStarter = new CountDownLatch(1);
        new Thread(() -> Network.getInstance().start(networkStarter)).start();
        networkStarter.await();

        FileSender.sendFile(Paths.get("client/file.txt"), Network.getInstance().getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
            }
            if (future.isSuccess()) {
                System.out.println("Файл успешно передан");
                Network.getInstance().stop();
            }
        });
    }
}