package com.example.demo.service;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dao.AccountDAO;
import com.example.demo.entity.Account;

@Service
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private AccountDAO accountDAO;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");
        String providerId = (String) attributes.get("sub");

        if (email != null && !email.trim().isEmpty()) {
            Account account = accountDAO.findAccountByEmail(email.toLowerCase());
            if (account == null) {
                // Tự động tạo tài khoản mới từ Google OAuth2
                account = new Account();
                // Username mặc định lấy từ tiền tố email (an toàn kí tự ASCII)
                String username = email.split("@")[0];
                if (accountDAO.findAccount(username) != null) {
                    username = username + "_" + System.currentTimeMillis() % 10000;
                }
                account.setUserName(username);
                account.setFullName(name != null ? name.trim() : username);
                account.setEmail(email.toLowerCase());
                account.setAvatarUrl(picture);
                account.setProviderId(providerId);
                account.setEncrytedPassword("");
                account.setActive(true);
                account.setAccountNonLocked(true);
                account.setUserRole(Account.ROLE_USER);
                account.setProvider("GOOGLE");
                account.setLastLogin(new Date());
                accountDAO.saveAccount(account);
            } else {
                // Cập nhật thông tin Google mới nhất và mốc thời gian Last Login
                account.setFullName(name != null ? name.trim() : account.getUserName());
                if (picture != null) {
                    account.setAvatarUrl(picture);
                }
                if (providerId != null) {
                    account.setProviderId(providerId);
                }
                account.setLastLogin(new Date());
                accountDAO.saveAccount(account);
            }
        }

        String userNameAttributeName = (attributes.containsKey("name") && attributes.get("name") != null) ? "name" : "email";

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                userNameAttributeName
        );
    }
}
