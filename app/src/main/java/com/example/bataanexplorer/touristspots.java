package com.example.bataanexplorer;

public class touristspots {
    private long id;
    private long municipality_id;
    private String name;
    private String category;
    private String location;
    private boolean is_open;
    private double rating;
    private long review_count;
    private String description;
    private String entrance_fee;
    private String operating_hours;
    private String image_url;

    // Required empty constructor
    public touristspots() {}

    // Getters
    public long getId() { return id; }
    public long getMunicipalityId() { return municipality_id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getLocation() { return location; }
    public boolean isOpen() { return is_open; }
    public double getRating() { return rating; }
    public long getReviewCount() { return review_count; }
    public String getDescription() { return description; }
    public String getEntranceFee() { return entrance_fee; }
    public String getOperatingHours() { return operating_hours; }
    public String getImageUrl() { return image_url; }
}