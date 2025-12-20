package dao;

import helper.DbHelper;
import model.Waste;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WasteDAO {

    // 1. Kategorileri Çek (ComboBox için)
    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT category_name FROM waste_categories";
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                categories.add(rs.getString("category_name"));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return categories;
    }

    // 2. Atık Ekle (INSERT)
    public boolean addWaste(String username, String categoryName, String district, double amount) {
        // Önce username'den ID'yi, categoryName'den category_id'yi bulmamız lazım
        // Pratik olsun diye iç içe sorgu (Subquery) kullanıyoruz.
        String sql = "INSERT INTO wastes (owner_id, category_id, district, amount) VALUES " +
                "((SELECT user_id FROM users WHERE username = ?), " +
                "(SELECT category_id FROM waste_categories WHERE category_name = ?), ?, ?)";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, categoryName);
            pstmt.setString(3, district);
            pstmt.setDouble(4, amount);

            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 3. Kullanıcının Atıklarını Listele (SELECT)
    public List<Waste> getMyWastes(String username) {
        List<Waste> list = new ArrayList<>();
        String sql = "SELECT w.waste_id, c.category_name, w.district, w.amount, w.status " +
                "FROM wastes w " +
                "JOIN waste_categories c ON w.category_id = c.category_id " +
                "JOIN users u ON w.owner_id = u.user_id " +
                "WHERE u.username = ? ORDER BY w.created_at DESC";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            while(rs.next()) {
                list.add(new Waste(
                        rs.getInt("waste_id"),
                        rs.getString("category_name"),
                        rs.getString("district"),
                        rs.getDouble("amount"),
                        rs.getString("status")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // --- BURADAN AŞAĞISINI WasteDAO SINIFININ İÇİNE EKLE ---

    // 4. View Kullanarak Müsait Atıkları Listele
    public List<Waste> getAvailableWastes() {
        List<Waste> list = new ArrayList<>();
        // DİKKAT: Tablo yerine VIEW sorguluyoruz
        String sql = "SELECT * FROM available_wastes_view";

        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while(rs.next()) {
                list.add(new Waste(
                        rs.getInt("waste_id"),
                        rs.getString("category_name"),
                        rs.getString("district"),
                        rs.getDouble("amount"),
                        rs.getString("status")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // 5. Index Kullanarak Arama Yap
    public List<Waste> searchWastesByDistrict(String districtKeyword) {
        List<Waste> list = new ArrayList<>();
        // LIKE operatörü index kullanır
        String sql = "SELECT * FROM available_wastes_view WHERE district ILIKE ?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + districtKeyword + "%");
            ResultSet rs = pstmt.executeQuery();

            while(rs.next()) {
                list.add(new Waste(
                        rs.getInt("waste_id"),
                        rs.getString("category_name"),
                        rs.getString("district"),
                        rs.getDouble("amount"),
                        rs.getString("status")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }
    // 6. Atığı Rezerve Et (Bu işlem Trigger'ı tetikler!)
    public boolean reserveWaste(int wasteId, String collectorUsername) {
        // collections tablosuna kayıt ekliyoruz.
        // waste_id ve collector_id (username'den buluyoruz)
        String sql = "INSERT INTO collections (waste_id, collector_id) VALUES " +
                "(?, (SELECT user_id FROM users WHERE username = ?))";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, wasteId);
            pstmt.setString(2, collectorUsername);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    // 7. Toplayıcının Kendi Rezerve Ettiklerini Getir
    public List<Waste> getMyReservations(String collectorUsername) {
        List<Waste> list = new ArrayList<>();
        // Join işlemiyle collections ve wastes tablolarını birleştiriyoruz
        String sql = "SELECT w.waste_id, c.category_name, w.district, w.amount, w.status " +
                "FROM collections col " +
                "JOIN wastes w ON col.waste_id = w.waste_id " +
                "JOIN waste_categories c ON w.category_id = c.category_id " +
                "JOIN users u ON col.collector_id = u.user_id " +
                "WHERE u.username = ? AND w.status = 'REZERVEYE_ALINDI'"; // Sadece aktif rezervasyonlar

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, collectorUsername);
            ResultSet rs = pstmt.executeQuery();

            while(rs.next()) {
                list.add(new Waste(
                        rs.getInt("waste_id"),
                        rs.getString("category_name"),
                        rs.getString("district"),
                        rs.getDouble("amount"),
                        rs.getString("status")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // 8. Teslim Al ve Puanla (Trigger'ı Tetikler!)
    public boolean completeCollection(int wasteId, int rating) {
        // collections tablosundaki 'rating' alanını güncelliyoruz.
        // Bu UPDATE işlemi, yazdığımız Trigger'ı ateşleyecek.
        String sql = "UPDATE collections SET rating = ?, collection_date = CURRENT_TIMESTAMP " +
                "WHERE waste_id = ?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, rating);
            pstmt.setInt(2, wasteId);

            int rows = pstmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


}
