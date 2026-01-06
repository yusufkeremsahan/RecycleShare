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

    // --- LİSTELEME METOTLARI (Tarih formatlama eklendi) ---

    public List<Waste> getMyWastes(String email) {
        // Sakin kendi atıklarına bakarken oluşturma tarihini görsün
        String sql = "SELECT w.waste_id, c.category_name, w.district, w.full_location_text, w.amount, w.unit, w.status, u.full_name, " +
                "to_char(w.created_at, 'DD.MM.YYYY HH24:MI') as display_date " + // Tarih Formatı
                "FROM wastes w " +
                "JOIN waste_categories c ON w.category_id = c.category_id " +
                "JOIN users u ON w.owner_id = u.user_id " +
                "WHERE u.email = ? ORDER BY w.created_at DESC";
        return getWastesByQuery(sql, email);
    }

    public List<Waste> getAvailableWastes() {
        // Toplayıcı müsait atıkları görürken ne zaman eklendiğini görsün
        String sql = "SELECT waste_id, category_name, district, full_location_text, amount, unit, status, owner_name, " +
                "to_char(created_at, 'DD.MM.YYYY HH24:MI') as display_date " +
                "FROM available_wastes_view WHERE created_at > NOW() - INTERVAL '30 DAYS'";
        return getWastesByQuery(sql, null);
    }

    public List<Waste> searchWastesByDistrict(String keyword) {
        String sql = "SELECT waste_id, category_name, district, full_location_text, amount, unit, status, owner_name, " +
                "to_char(created_at, 'DD.MM.YYYY HH24:MI') as display_date " +
                "FROM available_wastes_view WHERE full_location_text ILIKE ?";
        return getWastesByQuery(sql, "%" + keyword + "%");
    }

    public List<Waste> getMyReservations(String email) {
        // BURASI ÖNEMLİ: Geri sayım mantığı (Arkadaşının kodu)
        // 24 saatten ne kadar kaldığını saniye cinsinden hesaplıyoruz
        String sql = "SELECT w.waste_id, c.category_name, w.district, w.full_location_text, w.amount, w.unit, w.status, u_owner.full_name AS owner_name, " +
                "to_char(col.reserved_at, 'DD.MM.YYYY HH24:MI') as display_date, " +
                "EXTRACT(EPOCH FROM (col.reserved_at + INTERVAL '24 hours' - NOW()))::INT as seconds_left " +
                "FROM collections col " +
                "JOIN wastes w ON col.waste_id = w.waste_id " +
                "JOIN waste_categories c ON w.category_id = c.category_id " +
                "JOIN users u_owner ON w.owner_id = u_owner.user_id " +
                "JOIN users u_col ON col.collector_id = u_col.user_id " +
                "WHERE u_col.email = ? AND w.status = 'REZERVEYE_ALINDI'";
        return getWastesByQuery(sql, email);
    }

    // --- İŞLEM METOTLARI ---

    public boolean reserveWaste(int wasteId, String collectorEmail) {
        // Rezervasyon anını (NOW()) kaydediyoruz
        String sql = "INSERT INTO collections (waste_id, collector_id, reserved_at) VALUES (?, (SELECT user_id FROM users WHERE email = ?), NOW())";
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

    // SENİN GÜNCEL KODUN (Complete Function kullanıyor - Dokunmadım)
    public boolean completeCollection(int wasteId, int rClean, int rAcc, int rPunc) {
        String sql = "SELECT complete_waste_process(?, ?, ?, ?)";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, wasteId); pstmt.setInt(2, rClean); pstmt.setInt(3, rAcc); pstmt.setInt(4, rPunc);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getBoolean(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // SENİN GÜNCEL KODUN (Adres kontrolü - Dokunmadım)
    public boolean isReservationAllowed(String collectorEmail, int targetWasteId) {
        String sql = "SELECT COUNT(*) FROM collections c " +
                "JOIN wastes w_active ON c.waste_id = w_active.waste_id " +
                "JOIN users u ON c.collector_id = u.user_id " +
                "WHERE u.email = ? " +
                "AND w_active.status = 'REZERVEYE_ALINDI' " +
                "AND w_active.full_location_text != (SELECT full_location_text FROM wastes WHERE waste_id = ?)";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, collectorEmail);
            pstmt.setInt(2, targetWasteId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) == 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // ORTAK SORGULAMA METODU (Verileri Modele Çevirme)
    private List<Waste> getWastesByQuery(String sql, String param) {
        List<Waste> list = new ArrayList<>();
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (param != null) pstmt.setString(1, param);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
                String ownerName = "";
                try { ownerName = rs.getString("full_name"); } catch(Exception e) {
                    try { ownerName = rs.getString("owner_name"); } catch(Exception ex) {}
                }

                // TARİH OKUMA
                String dateVal = "";
                try { dateVal = rs.getString("display_date"); } catch (Exception e) {}

                // GERİ SAYIM HESAPLAMA (Saniye -> Saat/Dakika)
                String countdown = "";
                try {
                    long seconds = rs.getLong("seconds_left");
                    if (!rs.wasNull()) {
                        if (seconds > 0) {
                            long hours = seconds / 3600;
                            long minutes = (seconds % 3600) / 60;
                            countdown = hours + " sa " + minutes + " dk kaldı";
                        } else { countdown = "SÜRE DOLDU!"; }
                    }
                } catch (Exception e) { countdown = ""; }

                list.add(new Waste(
                        rs.getInt("waste_id"),
                        rs.getString("category_name"),
                        rs.getString("district"),
                        rs.getString("full_location_text"),
                        rs.getDouble("amount"),
                        rs.getString("unit"),
                        rs.getString("status"),
                        ownerName,
                        dateVal,    // YENİ
                        countdown   // YENİ
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }
}