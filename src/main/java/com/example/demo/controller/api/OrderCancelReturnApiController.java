package com.example.demo.controller.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dao.OrderReturnDAO;
import com.example.demo.entity.OrderReturn;
import com.example.demo.form.OrderReturnForm;
import com.example.demo.form.ReturnStatusUpdateForm;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "9. Order Cancellation & Returns REST API", description = "APIs xử lý hủy đơn hàng và yêu cầu trả hàng / hoàn tiền (JSON output)")
@RestController
@RequestMapping("/api/v1")
public class OrderCancelReturnApiController {

    @Autowired
    private OrderReturnDAO orderReturnDAO;

    private String getCurrentUsername() {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }

    private boolean isAdmin() {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        }
        return false;
    }

    @Operation(summary = "Hủy đơn hàng đang chờ xử lý (User)")
    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable("orderId") String orderId) {
        String username = getCurrentUsername();
        if (username == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Vui lòng đăng nhập để thực hiện hủy đơn!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        try {
            boolean success = orderReturnDAO.cancelOrder(username, orderId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", "Hủy đơn hàng #" + orderId + " thành công! Số lượng sản phẩm đã được hoàn lại kho.");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Lỗi xử lý hủy đơn: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Operation(summary = "Gửi yêu cầu trả hàng / hoàn tiền cho đơn hàng (User)")
    @PostMapping("/orders/{orderId}/return")
    public ResponseEntity<?> createReturnRequest(@PathVariable("orderId") String orderId, @RequestBody OrderReturnForm form) {
        String username = getCurrentUsername();
        if (username == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Vui lòng đăng nhập để gửi yêu cầu trả hàng!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        if (form.getReason() == null || form.getReason().trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Vui lòng nhập lý do trả hàng!");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            OrderReturn orderReturn = orderReturnDAO.createReturnRequest(username, orderId, form);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Gửi yêu cầu trả hàng thành công! Đang chờ Admin duyệt.");
            response.put("data", orderReturn);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @Operation(summary = "Xem thông tin chi tiết yêu cầu trả hàng của một đơn (User/Admin)")
    @GetMapping("/orders/{orderId}/return")
    public ResponseEntity<?> getReturnRequest(@PathVariable("orderId") String orderId) {
        OrderReturn orderReturn = orderReturnDAO.findReturnByOrderId(orderId);
        if (orderReturn == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Đơn hàng này chưa có yêu cầu trả hàng nào.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        return ResponseEntity.ok(orderReturn);
    }

    @Operation(summary = "Duyệt hoặc từ chối yêu cầu trả hàng (Admin)")
    @PutMapping("/admin/orders/{orderId}/return-status")
    public ResponseEntity<?> updateReturnStatus(@PathVariable("orderId") String orderId, @RequestBody ReturnStatusUpdateForm form) {
        if (!isAdmin()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Bạn không có quyền thực hiện chức năng này!");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        try {
            OrderReturn updated = orderReturnDAO.updateReturnStatus(getCurrentUsername(), orderId, form.getAction(), form.getAdminNote());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật trạng thái trả hàng thành công: " + updated.getStatus());
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
