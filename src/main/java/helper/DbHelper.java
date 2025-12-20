package helper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbHelper {
    // Veritabanı adı 'recycleshare' değilse düzelt
    private static final String URL = "jdbc:postgresql://localhost:5432/recycleshare";
    private static final String USER = "postgres";
    private static final String PASSWORD = "Eses2626.";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
