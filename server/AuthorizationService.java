import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;

public class AuthorizationService {

    private static Connection connection;
    private static Statement statement;

    public static void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:cloud_db.db");
            statement = connection.createStatement();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static String run(ByteBuf buf) throws SQLException, IOException {

        String username = null;
        String password = null;
        int usernameLength = -1;
        int passwordLength = -1;

        byte isNewUser = buf.readByte();
        System.out.println("Признак нового пользователя: " + isNewUser);

        if (buf.readableBytes() >= 4) {
            usernameLength = buf.readInt();
        }
        System.out.println("Длина логина: " + usernameLength);

        if (buf.readableBytes() >= usernameLength) {
            byte[] usernameBytes = new byte[usernameLength];
            buf.readBytes(usernameBytes);
            username = new String(usernameBytes);
        }
        System.out.println("Логин: " + username);

        if (buf.readableBytes() >= 4) {
            passwordLength = buf.readInt();
        }
        System.out.println("Длина пароля: " + passwordLength);

        if (buf.readableBytes() >= passwordLength) {
            byte[] passwordBytes = new byte[passwordLength];
            buf.readBytes(passwordBytes);
            password = new String(passwordBytes);
        }
        System.out.println("Пароль: " + password);

        buf.release();

        if (isNewUser == 1) {
            String sql = String.format("SELECT user_id FROM user WHERE username = '%s';", username);
            ResultSet rs = statement.executeQuery(sql);
            if (rs.next()) {
                return "Такой пользователь уже существует";
            } else {
                PreparedStatement pstmt = connection.prepareStatement("INSERT INTO user (username, password) VALUES (?, ?);");
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                pstmt.executeUpdate();
                Files.createDirectory(Paths.get("server/" + username));
                return "Успешная авторизация. Новый пользователь добавлен";
            }
        } else {
            String sql = String.format("SELECT user_id FROM user WHERE username = '%s' AND password = '%s';", username, password);
            ResultSet rs = statement.executeQuery(sql);
            if (rs.next()) {
                return "Успешная авторизация";
            } else {
                return "Ошибка авторизации";
            }
        }

    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
