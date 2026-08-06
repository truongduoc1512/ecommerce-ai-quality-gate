package com.example.demo.form;

public class ReturnStatusUpdateForm {

    private String action; // APPROVE or REJECT
    private String adminNote;

    public ReturnStatusUpdateForm() {
    }

    public ReturnStatusUpdateForm(String action, String adminNote) {
        this.action = action;
        this.adminNote = adminNote;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }
}
