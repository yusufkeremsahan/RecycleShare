package model;

public class Waste {
    private int id;
    private String category;
    private String district;
    private String fullLocation;
    private double amount;
    private String unit;
    private String status;
    private String ownerName;
    private String dateInfo;
    private String remainingTime;

    public Waste(int id, String category, String district, String fullLocation, double amount, String unit, String status, String ownerName, String dateInfo, String remainingTime) {
        this.id = id;
        this.category = category;
        this.district = district;
        this.fullLocation = fullLocation;
        this.amount = amount;
        this.unit = unit;
        this.status = status;
        this.ownerName = ownerName;
        this.dateInfo = dateInfo;
        this.remainingTime = remainingTime;
    }

    public int getId() { return id; }
    public String getCategory() { return category; }
    public String getDistrict() { return district; }
    public String getFullLocation() { return fullLocation; }
    public double getAmount() { return amount; }
    public String getUnit() { return unit; }
    public String getStatus() { return status; }
    public String getOwnerName() { return ownerName; }
    public String getDateInfo() { return dateInfo; }
    public String getRemainingTime() { return remainingTime; }
}