package com.example.demo.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "Voucher_Usages")
public class VoucherUsage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "voucher_code", length = 50, nullable = false)
    private String voucherCode;

    @Column(name = "username", length = 50, nullable = false)
    private String username;

    @Column(name = "order_id", length = 50, nullable = true)
    private String orderId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "used_at", nullable = false)
    private Date usedAt = new Date();

    public VoucherUsage() {
    }

    public VoucherUsage(String voucherCode, String username, String orderId) {
        this.voucherCode = voucherCode != null ? voucherCode.trim().toUpperCase() : null;
        this.username = username;
        this.orderId = orderId;
        this.usedAt = new Date();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode != null ? voucherCode.trim().toUpperCase() : null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Date getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Date usedAt) {
        this.usedAt = usedAt;
    }
}
