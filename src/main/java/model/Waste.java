package model;

public class Waste {
    private int id;
    private String category;
    private String district;
    private double amount;
    private String status;

    public Waste(int id, String category, String district, double amount, String status) {
        this.id = id;
        this.category = category;
        this.district = district;
        this.amount = amount;
        this.status = status;
    }

    // Getter metodları (JavaFX Tablosu veriyi buradan okur)
    public int getId() { return id; }
    public String getCategory() { return category; }
    public String getDistrict() { return district; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
}
