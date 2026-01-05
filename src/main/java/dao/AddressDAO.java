package dao;

import helper.DbHelper;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AddressDAO {

    // Kullanıcının kayıtlı adreslerinin başlıklarını getirir
    public List<String> getUserAddressTitles(String email) {
        List<String> titles = new ArrayList<>();
        String sql = "SELECT title FROM addresses WHERE user_id = (SELECT user_id FROM users WHERE email = ?)";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) titles.add(rs.getString("title"));
        } catch (Exception e) { e.printStackTrace(); }
        return titles;
    }

    // Seçilen başlığa göre TÜM detayları getirir
    public AddressDetails getAddressDetails(String email, String title) {
        String sql = "SELECT * FROM addresses WHERE user_id = (SELECT user_id FROM users WHERE email = ?) AND title = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, title);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new AddressDetails(
                        rs.getString("city"),
                        rs.getString("district"),
                        rs.getString("neighborhood"),
                        rs.getString("street"),
                        rs.getString("building_no"),
                        rs.getString("floor_no"),
                        rs.getString("door_no"),
                        rs.getString("directions")
                );
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // Yeni DETAYLI adres kaydeder
    public boolean saveAddress(String email, String title, String city, String district, String neigh,
                               String street, String buildNo, String floor, String door, String direct) {

        // Gösterim amaçlı tam metin oluştur
        String fullText = street + " No:" + buildNo + " D:" + door + " " + neigh + " " + district + "/" + city;

        String sql = "INSERT INTO addresses (user_id, title, city, district, neighborhood, street, building_no, floor_no, door_no, directions, full_address_text) " +
                "VALUES ((SELECT user_id FROM users WHERE email = ?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, title);
            pstmt.setString(3, city);
            pstmt.setString(4, district);
            pstmt.setString(5, neigh);
            pstmt.setString(6, street);
            pstmt.setString(7, buildNo);
            pstmt.setString(8, floor);
            pstmt.setString(9, door);
            pstmt.setString(10, direct);
            pstmt.setString(11, fullText);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // Veri taşıyıcı sınıf (DTO)
    public static class AddressDetails {
        public String city, district, neighborhood, street, buildingNo, floorNo, doorNo, directions;
        public AddressDetails(String c, String d, String n, String s, String b, String f, String door, String dir) {
            this.city = c; this.district = d; this.neighborhood = n; this.street = s;
            this.buildingNo = b; this.floorNo = f; this.doorNo = door; this.directions = dir;
        }
    }
}