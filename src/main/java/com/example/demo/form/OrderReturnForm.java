package com.example.demo.form;

public class OrderReturnForm {

    private String reason;
    private String imageUrls;

    public OrderReturnForm() {
    }

    public OrderReturnForm(String reason, String imageUrls) {
        this.reason = reason;
        this.imageUrls = imageUrls;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(String imageUrls) {
        this.imageUrls = imageUrls;
    }
}
