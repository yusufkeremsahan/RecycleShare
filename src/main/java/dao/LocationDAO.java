package dao;

import helper.DbHelper;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LocationDAO {

    // Tüm Şehirleri Getir
    public List<String> getAllCities() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT city_name FROM tr_cities ORDER BY city_name";
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(rs.getString("city_name"));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // Seçilen Şehrin İlçelerini Getir
    public List<String> getDistrictsByCity(String cityName) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT district_name FROM tr_districts d " +
                "JOIN tr_cities c ON d.city_id = c.city_id " +
                "WHERE c.city_name = ? ORDER BY district_name";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cityName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(rs.getString("district_name"));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // Seçilen İlçenin Mahallelerini Getir
    public List<String> getNeighborhoodsByDistrict(String districtName) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT neighborhood_name FROM tr_neighborhoods n " +
                "JOIN tr_districts d ON n.district_id = d.district_id " +
                "WHERE d.district_name = ? ORDER BY neighborhood_name";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, districtName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(rs.getString("neighborhood_name"));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }
}