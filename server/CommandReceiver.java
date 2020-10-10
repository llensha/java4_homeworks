import io.netty.buffer.ByteBuf;

public class CommandReceiver {
    public static String run(ByteBuf buf){
        buf.release();
        return "Обработка команды завершена";
    }
}
