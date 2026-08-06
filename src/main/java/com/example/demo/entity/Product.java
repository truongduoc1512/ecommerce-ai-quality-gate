package com.example.demo.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "Products")
public class Product implements Serializable {
 
    private static final long serialVersionUID = -1000119078147252957L;
 
    @Id
    @Column(name = "Code", length = 20, nullable = false)
    private String code;
 
    @Column(name = "Name", length = 255, nullable = false)
    private String name;
 
    @Column(name = "Price", nullable = false)
    private double price;
 
    @Lob
    @Column(name = "Image", length = Integer.MAX_VALUE, nullable = true)
    private byte[] image;
      
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "Create_Date", nullable = false)
    private Date createDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "UPDATE_DATE", nullable = true)
    private Date updateDate;

    @Column(name = "OWNER_USERNAME", length = 20, nullable = false)
    private String ownerUsername = "manager1";

    @Column(name = "DISCOUNT_PERCENT", nullable = false)
    private int discountPercent = 0;

    @Column(name = "SALES_COUNT", nullable = false)
    private int salesCount = 0;

    @Column(name = "LOCATION", length = 100, nullable = false)
    private String location = "Hồ Chí Minh";

    @Column(name = "BRAND", length = 100, nullable = false)
    private String brand = "Originals";

    @Column(name = "RATING", nullable = false)
    private double rating = 5.0;

    @Column(name = "REVIEW_COUNT", nullable = false)
    private int reviewCount = 0;

    @Column(name = "STOCK_QUANTITY", nullable = false)
    private int stockQuantity = 100;

    @Column(name = "CATEGORY", length = 100, nullable = false)
    private String category = "Giày Sneaker";

    @Column(name = "STATUS", length = 20, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "IS_MALL", nullable = false)
    private boolean isMall = false;

    @Column(name = "IS_FAVORED", nullable = false)
    private boolean isFavored = false;
 
    public Product() {
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
 
    public Date getCreateDate() {
        return createDate;
    }
 
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }
 
    public byte[] getImage() {
        return image;
    }
 
    public void setImage(byte[] image) {
        this.image = image;
    }
 
    public String getOwnerUsername() {
        return ownerUsername;
    }
 
    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
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
 
    public void setMall(boolean mall) {
        isMall = mall;
    }
 
    public boolean isFavored() {
        return isFavored;
    }
 
    public void setFavored(boolean favored) {
        isFavored = favored;
    }
 
}
