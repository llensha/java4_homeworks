import io.netty.buffer.ByteBuf;

public class Authorization {

    public static String run(ByteBuf buf) {
        String username = null;
        String password = null;
        int usernameLength = -1;
        int passwordLength = -1;

        if (buf.readableBytes() >= 4) {
            usernameLength = buf.readInt();
        }

        if (buf.readableBytes() >= usernameLength) {
            byte[] usernameBytes = new byte[usernameLength];
            buf.readBytes(usernameBytes);
            username = new String(usernameBytes);
        }

        if (buf.readableBytes() >= 4) {
            passwordLength = buf.readInt();
        }

        if (buf.readableBytes() >= passwordLength) {
            byte[] passwordBytes = new byte[passwordLength];
            buf.readBytes(passwordBytes);
            password = new String(passwordBytes);
        }

        buf.release();

        System.out.println(username);
        System.out.println(password);

        return "Успешная авторизация";

    }
}
