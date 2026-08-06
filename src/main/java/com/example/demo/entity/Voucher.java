package com.example.demo.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "Vouchers")
public class Voucher implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_PERCENT = "PERCENT";
    public static final String TYPE_FIXED = "FIXED";

    @Id
    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "discount_type", length = 20, nullable = false)
    private String discountType = TYPE_PERCENT;

    @Column(name = "discount_value", nullable = false)
    private double discountValue = 0.0;

    @Column(name = "max_discount", nullable = true)
    private Double maxDiscount;

    @Column(name = "min_order_value", nullable = false)
    private double minOrderValue = 0.0;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expiry_date", nullable = true)
    private Date expiryDate;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "usage_limit", nullable = false)
    private int usageLimit = 100;

    @Column(name = "used_count", nullable = false)
    private int usedCount = 0;

    @Column(name = "per_user_limit", nullable = false)
    private int perUserLimit = 1;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt = new Date();

    public Voucher() {
    }

    public Voucher(String code, String discountType, double discountValue, Double maxDiscount, double minOrderValue, Date expiryDate, boolean active, int usageLimit, int perUserLimit) {
        this.code = code != null ? code.trim().toUpperCase() : null;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscount = maxDiscount;
        this.minOrderValue = minOrderValue;
        this.expiryDate = expiryDate;
        this.active = active;
        this.usageLimit = usageLimit;
        this.perUserLimit = perUserLimit;
        this.createdAt = new Date();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code != null ? code.trim().toUpperCase() : null;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(double discountValue) {
        this.discountValue = discountValue;
    }

    public Double getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(Double maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    public double getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(double minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(int usageLimit) {
        this.usageLimit = usageLimit;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }

    public int getPerUserLimit() {
        return perUserLimit;
    }

    public void setPerUserLimit(int perUserLimit) {
        this.perUserLimit = perUserLimit;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
