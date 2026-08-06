package com.example.demo.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "Accounts")
public class Account implements Serializable {
 
    private static final long serialVersionUID = -2054386655979281969L;
 
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";
 
    @Id
    @Column(name = "User_Name", length = 50, nullable = false)
    private String userName;

    @Column(name = "Full_Name", length = 100, nullable = true)
    private String fullName;
 
    @Column(name = "Encryted_Password", length = 255, nullable = true)
    @JsonIgnore
    private String encrytedPassword;
 
    @Column(name = "Email", length = 100, nullable = true)
    private String email;

    @Column(name = "Phone_Number", length = 20, nullable = true)
    private String phoneNumber;

    @Column(name = "Avatar_Url", length = 255, nullable = true)
    private String avatarUrl;

    @Column(name = "Active", nullable = false)
    private boolean active = true;

    @Column(name = "Account_Non_Locked", nullable = false)
    private boolean accountNonLocked = true;

    @Column(name = "Failed_Attempts", nullable = false)
    private int failedAttempts = 0;
 
    @Column(name = "User_Role", length = 20, nullable = false)
    private String userRole = "ROLE_USER";

    @Column(name = "Reset_Token", length = 100, nullable = true)
    @JsonIgnore
    private String resetToken;

    @Column(name = "Provider", length = 20, nullable = true)
    private String provider = "LOCAL";

    @Column(name = "Provider_Id", length = 255, nullable = true)
    private String providerId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "Created_At", updatable = false)
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "Updated_At")
    private Date updatedAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "Last_Login", nullable = true)
    private Date lastLogin;
 
    public String getUserName() {
        return userName;
    }
 
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
 
    public String getEncrytedPassword() {
        return encrytedPassword;
    }
 
    public void setEncrytedPassword(String encrytedPassword) {
        this.encrytedPassword = encrytedPassword;
    }
 
    public boolean isActive() {
        return active;
    }
 
    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }
 
    public String getUserRole() {
        return userRole;
    }
 
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }
 
    @Override
    public String toString() {
        return "[" + this.userName + "," + this.fullName + "," + this.userRole + "]";
    }
}