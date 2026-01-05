package dao;

import helper.DbHelper;
import model.Waste;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WasteDAO {

    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT category_name FROM waste_categories";
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) categories.add(rs.getString("category_name"));
        } catch (Exception e) { e.printStackTrace(); }
        return categories;
    }

    public boolean addWaste(String email, String categoryName, String city, String district, String fullLocation, double amount, String unit) {
        String sql = "INSERT INTO wastes (owner_id, category_id, city, district, full_location_text, amount, unit) VALUES " +
                "((SELECT user_id FROM users WHERE email = ?), " +
                "(SELECT category_id FROM waste_categories WHERE category_name = ?), ?, ?, ?, ?, ?)";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            pstmt.setString(2, categoryName);
            pstmt.setString(3, city);
            pstmt.setString(4, district);
            pstmt.setString(5, fullLocation);
            pstmt.setDouble(6, amount);
            pstmt.setString(7, unit);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // --- LİSTELEME METOTLARI (GÜNCELLENDİ) ---

    // Sakinin Kendi Atıkları
    public List<Waste> getMyWastes(String email) {
        // Sakin kendi adını görmesine gerek yok, o yüzden ownerName yerine "Ben" yazdırabiliriz veya adını çekebiliriz.
        String sql = "SELECT w.waste_id, c.category_name, w.district, w.full_location_text, w.amount, w.unit, w.status, u.full_name " +
                "FROM wastes w " +
                "JOIN waste_categories c ON w.category_id = c.category_id " +
                "JOIN users u ON w.owner_id = u.user_id " +
                "WHERE u.email = ? ORDER BY w.created_at DESC";
        return getWastesByQuery(sql, email);
    }

    // Toplayıcı İçin Müsait Atıklar (View Kullanıyor - owner_name ve full_location View'da var)
    public List<Waste> getAvailableWastes() {
        return getWastesByQuery("SELECT waste_id, category_name, district, full_location_text, amount, unit, status, owner_name " +
                "FROM available_wastes_view WHERE created_at > NOW() - INTERVAL '30 DAYS'", null);
    }

    // Toplayıcı Arama (View Kullanıyor)
    public List<Waste> searchWastesByDistrict(String keyword) {
        return getWastesByQuery("SELECT waste_id, category_name, district, full_location_text, amount, unit, status, owner_name " +
                "FROM available_wastes_view WHERE district ILIKE ?", "%" + keyword + "%");
    }

    // Toplayıcının Rezerve Ettikleri (Join Gerekli)
    public List<Waste> getMyReservations(String email) {
        String sql = "SELECT w.waste_id, c.category_name, w.district, w.full_location_text, w.amount, w.unit, w.status, u_owner.full_name AS owner_name " +
                "FROM collections col " +
                "JOIN wastes w ON col.waste_id = w.waste_id " +
                "JOIN waste_categories c ON w.category_id = c.category_id " +
                "JOIN users u_owner ON w.owner_id = u_owner.user_id " + // Atık Sahibinin Adı
                "JOIN users u_col ON col.collector_id = u_col.user_id " + // Toplayıcı
                "WHERE u_col.email = ? AND w.status = 'REZERVEYE_ALINDI'";
        return getWastesByQuery(sql, email);
    }

    // --- YARDIMCI VE DİĞER METOTLAR ---

    public boolean reserveWaste(int wasteId, String collectorEmail) {
        String sql = "INSERT INTO collections (waste_id, collector_id) VALUES (?, (SELECT user_id FROM users WHERE email = ?))";
        String sqlUpd = "UPDATE wastes SET status = 'REZERVEYE_ALINDI' WHERE waste_id = ?";
        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(sql);
                 PreparedStatement ps2 = conn.prepareStatement(sqlUpd)) {
                ps1.setInt(1, wasteId);
                ps1.setString(2, collectorEmail);
                ps1.executeUpdate();
                ps2.setInt(1, wasteId);
                ps2.executeUpdate();
                conn.commit();
                return true;
            } catch(Exception ex) { conn.rollback(); return false; }
        } catch (SQLException e) { return false; }
    }

    public boolean completeCollection(int wasteId, int rClean, int rAcc, int rPunc) {
        // Artık tüm işlemi Veritabanındaki 'complete_waste_process' fonksiyonu yapıyor.
        // Bu sayede Transaction hatası, Trigger çakışması vs. olmuyor.
        String sql = "SELECT complete_waste_process(?, ?, ?, ?)";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, wasteId);
            pstmt.setInt(2, rClean);
            pstmt.setInt(3, rAcc);
            pstmt.setInt(4, rPunc);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                // Fonksiyon true dönerse işlem başarılıdır
                return rs.getBoolean(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isReservationAllowed(String collectorEmail, int targetWasteId) {
        String sql = "SELECT COUNT(*) FROM collections c " +
                "JOIN wastes w_active ON c.waste_id = w_active.waste_id " +
                "JOIN users u ON c.collector_id = u.user_id " +
                "WHERE u.email = ? " +
                "AND w_active.status = 'REZERVEYE_ALINDI' " +
                "AND w_active.owner_id != (SELECT owner_id FROM wastes WHERE waste_id = ?)";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, collectorEmail);
            pstmt.setInt(2, targetWasteId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) == 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private List<Waste> getWastesByQuery(String sql, String param) {
        List<Waste> list = new ArrayList<>();
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (param != null) pstmt.setString(1, param);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
                // ResultSet sütun adlarına dikkat! View veya Join sorgularındaki adlarla aynı olmalı.
                // Eğer alias kullandıysak (u.full_name AS owner_name gibi) alias'ı kullanmalıyız.
                // Basitlik olsun diye index ile veya standart isimlerle alıyoruz.

                String ownerName = "";
                try { ownerName = rs.getString("full_name"); } catch(Exception e) {
                    try { ownerName = rs.getString("owner_name"); } catch(Exception ex) {}
                }

                list.add(new Waste(
                        rs.getInt("waste_id"),
                        rs.getString("category_name"),
                        rs.getString("district"),
                        rs.getString("full_location_text"), // YENİ
                        rs.getDouble("amount"),
                        rs.getString("unit"),
                        rs.getString("status"),
                        ownerName // YENİ
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }
}