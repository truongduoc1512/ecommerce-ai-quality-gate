package com.example.demo.controller.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dao.UserAddressDAO;
import com.example.demo.entity.UserAddress;
import com.example.demo.form.UserAddressForm;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "8. Address Book REST API", description = "APIs quản lý sổ địa chỉ giao hàng của người dùng (JSON output)")
@RestController
@RequestMapping("/api/v1/users/addresses")
public class UserAddressApiController {

    @Autowired
    private UserAddressDAO userAddressDAO;

    private String getCurrentUsername() {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }

    @Operation(summary = "Lấy danh sách sổ địa chỉ giao hàng của người dùng đang đăng nhập")
    @GetMapping
    public ResponseEntity<?> getUserAddresses() {
        String username = getCurrentUsername();
        if (username == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Vui lòng đăng nhập để xem sổ địa chỉ!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        List<UserAddress> addresses = userAddressDAO.getUserAddresses(username);
        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Thêm một địa chỉ giao hàng mới")
    @PostMapping
    public ResponseEntity<?> createAddress(@RequestBody UserAddressForm form) {
        String username = getCurrentUsername();
        if (username == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Vui lòng đăng nhập để thêm địa chỉ!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        if (form.getReceiverName() == null || form.getReceiverName().trim().isEmpty() ||
            form.getPhone() == null || form.getPhone().trim().isEmpty() ||
            form.getStreetAddress() == null || form.getStreetAddress().trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Vui lòng điền đầy đủ Tên người nhận, Số điện thoại và Địa chỉ chi tiết!");
            return ResponseEntity.badRequest().body(error);
        }

        form.setId(null); // Ensure creation
        UserAddress address = userAddressDAO.saveAddress(username, form);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Thêm địa chỉ giao hàng mới thành công!");
        response.put("address", address);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Cập nhật thông tin địa chỉ giao hàng")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAddress(@PathVariable("id") Long id, @RequestBody UserAddressForm form) {
        String username = getCurrentUsername();
        if (username == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Vui lòng đăng nhập!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        UserAddress existing = userAddressDAO.getAddressById(id);
        if (existing == null || !existing.getUsername().equals(username)) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Không tìm thấy địa chỉ hoặc bạn không có quyền chỉnh sửa!");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        form.setId(id);
        UserAddress updated = userAddressDAO.saveAddress(username, form);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cập nhật địa chỉ thành công!");
        response.put("address", updated);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Xóa một địa chỉ giao hàng")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAddress(@PathVariable("id") Long id) {
        String username = getCurrentUsername();
        if (username == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Vui lòng đăng nhập!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        boolean deleted = userAddressDAO.deleteAddress(username, id);
        Map<String, Object> response = new HashMap<>();
        if (!deleted) {
            response.put("success", false);
            response.put("message", "Không tìm thấy địa chỉ hoặc bạn không có quyền xóa!");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        response.put("success", true);
        response.put("message", "Đã xóa địa chỉ giao hàng thành công!");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Đặt một địa chỉ làm địa chỉ mặc định")
    @PutMapping("/{id}/set-default")
    public ResponseEntity<?> setDefaultAddress(@PathVariable("id") Long id) {
        String username = getCurrentUsername();
        if (username == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Vui lòng đăng nhập!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        boolean updated = userAddressDAO.setDefaultAddress(username, id);
        Map<String, Object> response = new HashMap<>();
        if (!updated) {
            response.put("success", false);
            response.put("message", "Không tìm thấy địa chỉ!");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        response.put("success", true);
        response.put("message", "Đã đặt làm địa chỉ mặc định!");
        return ResponseEntity.ok(response);
    }
}
