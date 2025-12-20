package dao;

import helper.DbHelper;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // GİRİŞ İŞLEMİ
    public String login(String username, String password) {
        String sql = "SELECT role FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 1. YENİ KULLANICI KAYDI (Register)
    public boolean register(String username, String password, String fullName, String role) {
        String sql = "INSERT INTO users (username, password, full_name, role, score) VALUES (?, ?, ?, ?, 0)";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, fullName);
            pstmt.setString(4, role);

            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 2. LİDERLİK TABLOSU (Top 5 Score)
    public static class UserScore {
        private String name;
        private int score;
        public UserScore(String name, int score) { this.name = name; this.score = score; }
        public String getName() { return name; }
        public int getScore() { return score; }
    }

    public List<UserScore> getTopUsers() {
        List<UserScore> list = new ArrayList<>();
        String sql = "SELECT full_name, score FROM users WHERE role = 'SAKIN' ORDER BY score DESC LIMIT 5";

        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while(rs.next()) {
                list.add(new UserScore(rs.getString("full_name"), rs.getInt("score")));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // 3. RAPOR FONKSİYONU (EKSİK OLAN KISIM BURASIYDI)
    public String getImpactReport(String username) {
        String report = "Henüz rapor verisi yok.";
        // SQL tarafında yazdığımız fonksiyonu çağırıyoruz
        String sql = "SELECT get_personal_impact_report((SELECT user_id FROM users WHERE username = ?))";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                report = rs.getString(1); // PostgreSQL fonksiyonunun döndürdüğü metin
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return report;
    }
}