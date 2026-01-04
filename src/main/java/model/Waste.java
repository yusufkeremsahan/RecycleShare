package model;

public class Waste {
    private int id;
    private String category;
    private String district;
    private double amount;
    private String unit;   // YENİ EKLENDİ
    private String status;

    // Constructor güncellendi: 'unit' eklendi
    public Waste(int id, String category, String district, double amount, String unit, String status) {
        this.id = id;
        this.category = category;
        this.district = district;
        this.amount = amount;
        this.unit = unit;
        this.status = status;
    }

    public int getId() { return id; }
    public String getCategory() { return category; }
    public String getDistrict() { return district; }
    public double getAmount() { return amount; }
    public String getUnit() { return unit; } // YENİ
    public String getStatus() { return status; }
}