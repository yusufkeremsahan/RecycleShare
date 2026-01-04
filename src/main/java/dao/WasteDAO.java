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

    // GÜNCELLENDİ: unit parametresi eklendi
    public boolean addWaste(String username, String categoryName, String district, double amount, String unit) {
        // SQL'e 'unit' alanı eklendi
        String sql = "INSERT INTO wastes (owner_id, category_id, district, amount, unit) VALUES " +
                "((SELECT user_id FROM users WHERE username = ?), " +
                "(SELECT category_id FROM waste_categories WHERE category_name = ?), ?, ?, ?)";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, categoryName);
            pstmt.setString(3, district);
            pstmt.setDouble(4, amount);
            pstmt.setString(5, unit); // YENİ: Birim kaydediliyor

            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // GÜNCELLENDİ: unit verisi çekiliyor
    public List<Waste> getMyWastes(String username) {
        List<Waste> list = new ArrayList<>();
        // SQL'e 'w.unit' eklendi
        String sql = "SELECT w.waste_id, c.category_name, w.district, w.amount, w.unit, w.status " +
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
                        rs.getString("unit"),
                        rs.getString("status")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public List<Waste> getAvailableWastes() {
        List<Waste> list = new ArrayList<>();
        // VIEW henüz güncellenmediği için manuel sorgu yazalım ya da View'i güncellemek gerekir.
        // Pratik çözüm: View yerine doğrudan sorgu atalım veya View'i de güncellemelisin.
        // Şimdilik View'in de unit döndürdüğünü varsayalım (veya View'i güncelleme adımını aşağıya ekleyeceğim).
        String sql = "SELECT w.waste_id, c.category_name, w.district, w.amount, w.unit, w.status " +
                "FROM wastes w JOIN waste_categories c ON w.category_id = c.category_id " +
                "WHERE w.status = 'MUSAIT'";

        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while(rs.next()) {
                list.add(new Waste(rs.getInt("waste_id"), rs.getString("category_name"),
                        rs.getString("district"), rs.getDouble("amount"),
                        rs.getString("unit"), rs.getString("status")));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public List<Waste> searchWastesByDistrict(String districtKeyword) {
        List<Waste> list = new ArrayList<>();
        String sql = "SELECT w.waste_id, c.category_name, w.district, w.amount, w.unit, w.status " +
                "FROM wastes w JOIN waste_categories c ON w.category_id = c.category_id " +
                "WHERE w.status = 'MUSAIT' AND w.district ILIKE ?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + districtKeyword + "%");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
                list.add(new Waste(rs.getInt("waste_id"), rs.getString("category_name"),
                        rs.getString("district"), rs.getDouble("amount"),
                        rs.getString("unit"), rs.getString("status")));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public boolean reserveWaste(int wasteId, String collectorUsername) {
        String sql = "INSERT INTO collections (waste_id, collector_id) VALUES (?, (SELECT user_id FROM users WHERE username = ?))";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, wasteId);
            pstmt.setString(2, collectorUsername);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean hasActiveReservation(String username) {
        String sql = "SELECT COUNT(*) FROM collections c JOIN wastes w ON c.waste_id = w.waste_id " +
                "WHERE c.collector_id = (SELECT user_id FROM users WHERE username = ?) AND w.status = 'REZERVEYE_ALINDI'";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public List<Waste> getMyReservations(String collectorUsername) {
        List<Waste> list = new ArrayList<>();
        String sql = "SELECT w.waste_id, c.category_name, w.district, w.amount, w.unit, w.status " +
                "FROM collections col JOIN wastes w ON col.waste_id = w.waste_id " +
                "JOIN waste_categories c ON w.category_id = c.category_id " +
                "JOIN users u ON col.collector_id = u.user_id " +
                "WHERE u.username = ? AND w.status = 'REZERVEYE_ALINDI'";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, collectorUsername);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
                list.add(new Waste(rs.getInt("waste_id"), rs.getString("category_name"),
                        rs.getString("district"), rs.getDouble("amount"),
                        rs.getString("unit"), rs.getString("status")));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public boolean completeCollection(int wasteId, int rating) {
        String sql = "UPDATE collections SET rating = ?, collection_date = CURRENT_TIMESTAMP WHERE waste_id = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, rating);
            pstmt.setInt(2, wasteId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
}