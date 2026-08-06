package com.example.demo.controller.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dao.OrderDAO;
import com.example.demo.model.OrderDetailInfo;
import com.example.demo.model.OrderInfo;
import com.example.demo.pagination.PaginationResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "2. Order REST API", description = "RESTful APIs dành cho tra cứu và quản lý đơn hàng (JSON output)")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderApiController {

    @Autowired
    private OrderDAO orderDAO;

    @Operation(summary = "Lấy danh sách tất cả các đơn hàng có phân trang")
    @GetMapping
    public ResponseEntity<PaginationResult<OrderInfo>> getOrders(
            @RequestParam(value = "page", defaultValue = "1") int page) {
        int maxResult = 10;
        int maxNavigationPage = 10;
        PaginationResult<OrderInfo> result = orderDAO.listOrderInfo(page, maxResult, maxNavigationPage, "", "");
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Lấy chi tiết đơn hàng theo ID")
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable("orderId") String orderId) {
        OrderInfo orderInfo = orderDAO.getOrderInfo(orderId);
        if (orderInfo == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Không tìm thấy đơn hàng với ID: " + orderId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        List<OrderDetailInfo> details = orderDAO.listOrderDetailInfos(orderId);
        orderInfo.setDetails(details);
        return ResponseEntity.ok(orderInfo);
    }

    @Operation(summary = "Cập nhật trạng thái đơn hàng (PENDING, APPROVED, SHIPPED, COMPLETED, CANCELLED)")
    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable("orderId") String orderId,
            @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        if (status == null || status.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Trạng thái mới 'status' không được để trống!");
            return ResponseEntity.badRequest().body(error);
        }
        OrderInfo orderInfo = orderDAO.getOrderInfo(orderId);
        if (orderInfo == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Không tìm thấy đơn hàng để cập nhật với ID: " + orderId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        try {
            orderDAO.updateOrderStatus(orderId, status.trim().toUpperCase());
            OrderInfo updatedOrder = orderDAO.getOrderInfo(orderId);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Cập nhật trạng thái thất bại: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
