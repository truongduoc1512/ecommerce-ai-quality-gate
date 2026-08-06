package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.demo.dao.AccountDAO;
import com.example.demo.entity.Account;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private AccountDAO accountDAO;

    @ModelAttribute("userAvatarUrl")
    public String getUserAvatarUrl() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof OAuth2User) {
            OAuth2User oauthUser = (OAuth2User) principal;
            String picture = (String) oauthUser.getAttribute("picture");
            if (picture != null && !picture.trim().isEmpty()) {
                return picture;
            }
        }

        String username = auth.getName();
        if (username != null && !username.trim().isEmpty()) {
            Account account = accountDAO.findAccount(username);
            if (account == null && principal instanceof OAuth2User) {
                OAuth2User oauthUser = (OAuth2User) principal;
                String email = (String) oauthUser.getAttribute("email");
                if (email != null) {
                    account = accountDAO.findAccountByEmail(email.toLowerCase());
                }
            }
            if (account != null && account.getAvatarUrl() != null && !account.getAvatarUrl().trim().isEmpty()) {
                return account.getAvatarUrl();
            }
        }
        return null;
    }

    @ModelAttribute("userDisplayName")
    public String getUserDisplayName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof OAuth2User) {
            OAuth2User oauthUser = (OAuth2User) principal;
            String name = (String) oauthUser.getAttribute("name");
            if (name != null && !name.trim().isEmpty()) {
                return name;
            }
        }

        String username = auth.getName();
        if (username != null && !username.trim().isEmpty()) {
            Account account = accountDAO.findAccount(username);
            if (account != null && account.getFullName() != null && !account.getFullName().trim().isEmpty()) {
                return account.getFullName();
            }
        }
        return username;
    }
}
