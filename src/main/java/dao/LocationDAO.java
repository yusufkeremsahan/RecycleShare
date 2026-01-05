package dao;

import helper.DbHelper;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocationDAO {

    // Tüm Şehirleri Getir (Artık Sadece İstanbul)
    public List<String> getAllCities() {
        // Veritabanı tablosu silindiği için sabit liste dönüyoruz.
        // Bu sayede arayüz kodunu bozmadan İstanbul'u tek seçenek yapıyoruz.
        return Arrays.asList("İstanbul");
    }

    // Seçilen Şehrin İlçelerini Getir
    public List<String> getDistrictsByCity(String cityName) {
        // cityName parametresi "İstanbul" değilse boş dönebilir veya yine de ilçeleri getirebiliriz.
        // Ancak biz sadece İstanbul ilçelerini veritabanından çekeceğiz.

        List<String> list = new ArrayList<>();
        // Artık City tablosuna JOIN yapmıyoruz, doğrudan tüm ilçeleri çekiyoruz.
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