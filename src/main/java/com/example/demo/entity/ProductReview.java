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
@Table(name = "Product_Reviews")
public class ProductReview implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Review_ID")
    private Long reviewId;

    @Column(name = "Product_Code", length = 20, nullable = false)
    private String productCode;

    @Column(name = "Username", length = 50, nullable = false)
    private String username;

    @Column(name = "Rating_Value", nullable = false)
    private int ratingValue;

    @Column(name = "Comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "Image_Url", length = 255)
    private String imageUrl;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "Created_At", nullable = false)
    private Date createdAt = new Date();

    public ProductReview() {
    }

    public ProductReview(String productCode, String username, int ratingValue, String comment) {
        this.productCode = productCode;
        this.username = username;
        this.ratingValue = ratingValue;
        this.comment = comment;
        this.createdAt = new Date();
    }

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getRatingValue() {
        return ratingValue;
    }

    public void setRatingValue(int ratingValue) {
        this.ratingValue = ratingValue;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
