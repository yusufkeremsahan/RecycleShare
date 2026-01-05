package dao;

import helper.DbHelper;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // GİRİŞ İŞLEMİ (Email ile)
    public String login(String email, String password) {
        String sql = "SELECT role FROM users WHERE email = ? AND password = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
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

    // YENİ KULLANICI KAYDI (Email ile)
    public boolean register(String email, String password, String fullName, String role) {
        String sql = "INSERT INTO users (email, password, full_name, role, score) VALUES (?, ?, ?, ?, 0)";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
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

    // LİDERLİK TABLOSU
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

    // RAPOR FONKSİYONU
    public String getImpactReport(String email) {
        String report = "Henüz rapor verisi yok.";
        String sql = "SELECT get_personal_impact_report((SELECT user_id FROM users WHERE email = ?))";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                report = rs.getString(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return report;
    }
}