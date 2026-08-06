package com.example.demo.controller.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dao.AccountDAO;
import com.example.demo.entity.Account;
import com.example.demo.form.RegisterForm;
import com.example.demo.form.UserProfileForm;
import com.example.demo.pagination.PaginationResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "4. User REST API", description = "RESTful APIs dành cho quản lý người dùng, đăng ký và thông tin tài khoản (JSON output)")
@RestController
@RequestMapping("/api/v1/users")
public class UserApiController {

    @Autowired
    private AccountDAO accountDAO;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Operation(summary = "Lấy danh sách người dùng có phân trang (Admin)")
    @GetMapping
    public ResponseEntity<PaginationResult<Account>> getUsers(
            @RequestParam(value = "page", defaultValue = "1") int page) {
        int maxResult = 10;
        int maxNavigationPage = 10;
        PaginationResult<Account> result = accountDAO.listAccounts(page, maxResult, maxNavigationPage);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Lấy thông tin tài khoản người dùng theo username")
    @GetMapping("/{userName}")
    public ResponseEntity<?> getUserByUsername(@PathVariable("userName") String userName) {
        Account account = accountDAO.findAccount(userName);
        if (account == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Không tìm thấy tài khoản với username: " + userName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        return ResponseEntity.ok(account);
    }

    @Operation(summary = "Đăng ký tài khoản mới (JSON Payload)")
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterForm registerForm) {
        if (registerForm.getUserName() == null || registerForm.getUserName().trim().isEmpty() ||
            registerForm.getEmail() == null || registerForm.getEmail().trim().isEmpty() ||
            registerForm.getPassword() == null || registerForm.getPassword().trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Vui lòng nhập đầy đủ userName, email và password!");
            return ResponseEntity.badRequest().body(error);
        }

        Account existingAccount = accountDAO.findAccount(registerForm.getUserName().trim());
        if (existingAccount != null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Tên đăng nhập đã tồn tại trong hệ thống!");
            return ResponseEntity.badRequest().body(error);
        }

        Account existingEmail = accountDAO.findAccountByEmail(registerForm.getEmail().trim().toLowerCase());
        if (existingEmail != null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Email đã được sử dụng bởi tài khoản khác!");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            Account account = new Account();
            account.setUserName(registerForm.getUserName().trim());
            account.setEmail(registerForm.getEmail().trim().toLowerCase());
            account.setEncrytedPassword(passwordEncoder.encode(registerForm.getPassword()));
            account.setActive(true);
            account.setUserRole(Account.ROLE_USER);
            account.setProvider("LOCAL");

            accountDAO.saveAccount(account);
            Account savedAccount = accountDAO.findAccount(account.getUserName());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedAccount);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Lỗi tạo tài khoản: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Operation(summary = "Lấy hồ sơ cá nhân của tài khoản đang đăng nhập")
    @GetMapping("/profile")
    public ResponseEntity<?> getCurrentUserProfile() {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Vui lòng đăng nhập để xem thông tin cá nhân!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        String username = auth.getName();
        Account account = accountDAO.findAccount(username);
        if (account == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Không tìm thấy thông tin tài khoản trong hệ thống!");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        return ResponseEntity.ok(account);
    }

    @Operation(summary = "Cập nhật hồ sơ cá nhân (FullName, Email, Phone, AvatarUrl)")
    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@RequestBody UserProfileForm profileForm) {
        if (profileForm.getUserName() == null || profileForm.getUserName().trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Tên đăng nhập 'userName' không được để trống!");
            return ResponseEntity.badRequest().body(error);
        }

        Account account = accountDAO.findAccount(profileForm.getUserName().trim());
        if (account == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Không tìm thấy tài khoản cần cập nhật!");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        try {
            if (profileForm.getFullName() != null) account.setFullName(profileForm.getFullName().trim());
            if (profileForm.getEmail() != null) account.setEmail(profileForm.getEmail().trim().toLowerCase());
            if (profileForm.getPhoneNumber() != null) account.setPhoneNumber(profileForm.getPhoneNumber().trim());
            if (profileForm.getAvatarUrl() != null && !profileForm.getAvatarUrl().trim().isEmpty()) {
                account.setAvatarUrl(profileForm.getAvatarUrl().trim());
            }

            accountDAO.saveAccount(account);
            Account updatedAccount = accountDAO.findAccount(account.getUserName());
            return ResponseEntity.ok(updatedAccount);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Cập nhật hồ sơ thất bại: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
