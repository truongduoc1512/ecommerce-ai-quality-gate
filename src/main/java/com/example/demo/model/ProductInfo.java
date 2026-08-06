package com.example.demo.model;

import com.example.demo.entity.Product;

public class ProductInfo {
    private String code;
    private String name;
    private double price;
    private double originalPrice;
    private int discountPercent;
    private int salesCount;
    private String location;
    private String brand;
    private double rating;
    private int reviewCount;
    private int stockQuantity;
    private String category;
    private String status;
    private boolean isMall;
    private boolean isFavored;
 
    public ProductInfo() {
    }
 
    public ProductInfo(Product product) {
        this.code = product.getCode();
        this.name = product.getName();
        this.originalPrice = product.getPrice();
        this.discountPercent = product.getDiscountPercent();
        this.price = product.getPrice() * (100 - product.getDiscountPercent()) / 100.0;
        this.salesCount = product.getSalesCount();
        this.location = product.getLocation();
        this.brand = product.getBrand();
        this.rating = product.getRating();
        this.reviewCount = product.getReviewCount();
        this.stockQuantity = product.getStockQuantity();
        this.category = product.getCategory();
        this.status = product.getStatus();
        this.isMall = product.isMall();
        this.isFavored = product.isFavored();
    }
 
    // Using in JPA/Hibernate query
    public ProductInfo(String code, String name, double price) {
        this.code = code;
        this.name = name;
        this.originalPrice = price;
        this.price = price;
    }
 
    public ProductInfo(String code, String name, double price, int discountPercent, int salesCount, 
                       String location, String brand, double rating, boolean isMall, boolean isFavored) {
        this.code = code;
        this.name = name;
        this.originalPrice = price;
        this.discountPercent = discountPercent;
        this.price = price * (100 - discountPercent) / 100.0;
        this.salesCount = salesCount;
        this.location = location;
        this.brand = brand;
        this.rating = rating;
        this.isMall = isMall;
        this.isFavored = isFavored;
    }

    public ProductInfo(String code, String name, double price, int discountPercent, int salesCount, 
                       String location, String brand, double rating, boolean isMall, boolean isFavored,
                       int reviewCount, int stockQuantity) {
        this(code, name, price, discountPercent, salesCount, location, brand, rating, isMall, isFavored);
        this.reviewCount = reviewCount;
        this.stockQuantity = stockQuantity;
    }
 
    public String getCode() {
        return code;
    }
 
    public void setCode(String code) {
        this.code = code;
    }
 
    public String getName() {
        return name;
    }
 
    public void setName(String name) {
        this.name = name;
    }
 
    public double getPrice() {
        return price;
    }
 
    public void setPrice(double price) {
        this.price = price;
    }
 
    public int getDiscountPercent() {
        return discountPercent;
    }
 
    public void setDiscountPercent(int discountPercent) {
        this.discountPercent = discountPercent;
    }
 
    public int getSalesCount() {
        return salesCount;
    }
 
    public void setSalesCount(int salesCount) {
        this.salesCount = salesCount;
    }
 
    public String getLocation() {
        return location;
    }
 
    public void setLocation(String location) {
        this.location = location;
    }
 
    public String getBrand() {
        return brand;
    }
 
    public void setBrand(String brand) {
        this.brand = brand;
    }
 
    public double getRating() {
        return rating;
    }
 
    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
 
    public boolean isMall() {
        return isMall;
    }
 
    public boolean getIsMall() {
        return isMall;
    }
 
    public void setMall(boolean mall) {
        isMall = mall;
    }
 
    public boolean isFavored() {
        return isFavored;
    }
 
    public boolean getIsFavored() {
        return isFavored;
    }
 
    public void setFavored(boolean favored) {
        isFavored = favored;
    }
 
    public double getOriginalPrice() {
        return originalPrice;
    }
 
    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }
 
}
