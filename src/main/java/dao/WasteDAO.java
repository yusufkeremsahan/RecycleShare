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

    public boolean completeCollection(int wasteId, int rClean, int rAcc, int rPunc) {
        int avg = (rClean + rAcc + rPunc) / 3;

        // Not: Puan artırma işlemini SQL Trigger yapıyor, burada sadece durumu güncelliyoruz.
        String sql = "UPDATE collections SET rating_cleanliness = ?, rating_accuracy = ?, rating_punctuality = ?, rating_avg = ?, collection_date = CURRENT_TIMESTAMP WHERE waste_id = ?";
        String sqlUpdateStatus = "UPDATE wastes SET status = 'TAMAMLANDI' WHERE waste_id = ?";

        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(sql);
                 PreparedStatement ps2 = conn.prepareStatement(sqlUpdateStatus)) {

                ps1.setInt(1, rClean);
                ps1.setInt(2, rAcc);
                ps1.setInt(3, rPunc);
                ps1.setInt(4, avg);
                ps1.setInt(5, wasteId);
                ps1.executeUpdate();

                ps2.setInt(1, wasteId);
                ps2.executeUpdate(); // Bu çalışınca TRIGGER devreye girip puan verecek

                conn.commit();
                return true;
            } catch (SQLException ex) { conn.rollback(); ex.printStackTrace(); return false; }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // --- LİSTELEME METOTLARI ---
    public List<Waste> getMyWastes(String email) {
        return getWastesByQuery("SELECT w.waste_id, c.category_name, w.district, w.amount, w.unit, w.status " +
                "FROM wastes w JOIN waste_categories c ON w.category_id = c.category_id " +
                "JOIN users u ON w.owner_id = u.user_id WHERE u.email = ? ORDER BY w.created_at DESC", email);
    }

    public List<Waste> getAvailableWastes() {
        return getWastesByQuery("SELECT waste_id, category_name, district, amount, unit, status " +
                "FROM available_wastes_view WHERE created_at > NOW() - INTERVAL '30 DAYS'", null);
    }

    public List<Waste> searchWastesByDistrict(String keyword) {
        return getWastesByQuery("SELECT waste_id, category_name, district, amount, unit, status " +
                "FROM available_wastes_view WHERE district ILIKE ?", "%" + keyword + "%");
    }

    public List<Waste> getMyReservations(String email) {
        return getWastesByQuery("SELECT w.waste_id, c.category_name, w.district, w.amount, w.unit, w.status " +
                "FROM collections col JOIN wastes w ON col.waste_id = w.waste_id " +
                "JOIN waste_categories c ON w.category_id = c.category_id " +
                "JOIN users u ON col.collector_id = u.user_id " +
                "WHERE u.email = ? AND w.status = 'REZERVEYE_ALINDI'", email);
    }

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



    private List<Waste> getWastesByQuery(String sql, String param) {
        List<Waste> list = new ArrayList<>();
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (param != null) pstmt.setString(1, param);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
                list.add(new Waste(
                        rs.getInt("waste_id"), rs.getString("category_name"),
                        rs.getString("district"), rs.getDouble("amount"),
                        rs.getString("unit"), rs.getString("status")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // YENİ KONTROL: Toplayıcı rezervasyon yapabilir mi?
    // Kural: Hiç rezervasyonu yoksa EVET.
    // Varsa, sadece mevcut rezervasyonlarıyla AYNI SAHİBE (Owner) aitse EVET.
    public boolean isReservationAllowed(String collectorEmail, int targetWasteId) {
        String sql = "SELECT COUNT(*) FROM collections c " +
                "JOIN wastes w_active ON c.waste_id = w_active.waste_id " + // Mevcut aktif işler
                "JOIN users u ON c.collector_id = u.user_id " +
                "WHERE u.email = ? " +
                "AND w_active.status = 'REZERVEYE_ALINDI' " +
                // KRİTİK KISIM: Mevcut işin sahibi, hedef işin sahibinden FARKLI MI?
                "AND w_active.owner_id != (SELECT owner_id FROM wastes WHERE waste_id = ?)";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, collectorEmail);
            pstmt.setInt(2, targetWasteId);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int conflictCount = rs.getInt(1);
                // Eğer çakışma sayısı 0 ise izin ver (Ya hiç işi yok, ya da aynı kişiyle işi var)
                return conflictCount == 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false; // Hata varsa güvenli tarafı seçip engelle
    }
}