package dao;

import helper.DbHelper;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocationDAO {

    //İleride birden çok şehir olursa diye bu şekilde tasarlandı.
    public List<String> getAllCities() {
        return Arrays.asList("İstanbul");
    }

    public List<String> getDistrictsByCity(String cityName) {

        List<String> list = new ArrayList<>();
        String sql = "SELECT district_name FROM tr_districts ORDER BY district_name";

        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
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