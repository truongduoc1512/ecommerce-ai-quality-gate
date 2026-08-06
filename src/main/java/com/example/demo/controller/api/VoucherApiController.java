package com.example.demo.controller.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dao.VoucherDAO;
import com.example.demo.entity.Voucher;
import com.example.demo.form.VoucherForm;
import com.example.demo.model.CartInfo;
import com.example.demo.model.VoucherApplyResult;
import com.example.demo.utils.Utils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "7. Voucher REST API", description = "APIs quản lý và áp dụng Mã giảm giá (JSON output)")
@RestController
public class VoucherApiController {

    @Autowired
    private VoucherDAO voucherDAO;

    private String getCurrentUsername() {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }

    // ==========================================
    // USER APIs
    // ==========================================

    @Operation(summary = "Lấy danh sách các mã giảm giá hợp lệ đang có hiệu lực")
    @GetMapping("/api/v1/vouchers")
    public ResponseEntity<List<Voucher>> getActiveVouchers() {
        List<Voucher> vouchers = voucherDAO.listActiveVouchers();
        return ResponseEntity.ok(vouchers);
    }

    @Operation(summary = "Áp dụng mã giảm giá cho đơn hàng")
    @PostMapping("/api/v1/vouchers/apply")
    public ResponseEntity<VoucherApplyResult> applyVoucher(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {

        String voucherCode = payload.get("voucherCode") != null ? payload.get("voucherCode").toString() : null;
        CartInfo myCart = Utils.getCartInSession(request);

        double orderAmount = 0.0;
        if (payload.containsKey("orderAmount") && payload.get("orderAmount") != null) {
            try {
                orderAmount = Double.parseDouble(payload.get("orderAmount").toString());
            } catch (Exception e) {
                orderAmount = myCart != null ? myCart.getAmountTotal() : 0.0;
            }
        } else if (myCart != null) {
            orderAmount = myCart.getAmountTotal();
        }

        String username = getCurrentUsername();
        VoucherApplyResult result = voucherDAO.validateAndApplyVoucher(voucherCode, orderAmount, username);

        if (result.isSuccess() && myCart != null) {
            myCart.setVoucherCode(result.getVoucherCode());
            myCart.setDiscountAmount(result.getDiscountAmount());
        }

        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }

        return ResponseEntity.ok(result);
    }

    // ==========================================
    // ADMIN APIs
    // ==========================================

    @Operation(summary = "Lấy toàn bộ danh sách voucher (Admin)")
    @GetMapping("/api/v1/admin/vouchers")
    public ResponseEntity<List<Voucher>> getAllVouchersAdmin() {
        List<Voucher> list = voucherDAO.listAllVouchers();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Tạo mới hoặc cập nhật mã giảm giá (Admin)")
    @PostMapping("/api/v1/admin/vouchers")
    public ResponseEntity<?> createVoucherAdmin(@RequestBody VoucherForm form) {
        if (form.getCode() == null || form.getCode().trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Mã giảm giá không được để trống!");
            return ResponseEntity.badRequest().body(error);
        }

        if (form.getDiscountValue() <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Giá trị giảm giá phải lớn hơn 0!");
            return ResponseEntity.badRequest().body(error);
        }

        voucherDAO.saveVoucher(form);
        Voucher voucher = voucherDAO.findVoucher(form.getCode());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Tạo / Cập nhật mã giảm giá '" + form.getCode().toUpperCase() + "' thành công!");
        response.put("voucher", voucher);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Vô hiệu hóa hoặc xóa mã giảm giá (Admin)")
    @DeleteMapping("/api/v1/admin/vouchers/{code}")
    public ResponseEntity<?> deleteVoucherAdmin(@PathVariable("code") String code) {
        boolean deleted = voucherDAO.deleteVoucher(code);
        Map<String, Object> response = new HashMap<>();
        if (!deleted) {
            response.put("success", false);
            response.put("message", "Không tìm thấy mã giảm giá: " + code);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        response.put("success", true);
        response.put("message", "Đã vô hiệu hóa mã giảm giá: " + code);
        return ResponseEntity.ok(response);
    }
}
