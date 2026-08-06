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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dao.ProductReviewDAO;
import com.example.demo.entity.ProductReview;
import com.example.demo.form.ProductReviewForm;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "5. Review REST API", description = "RESTful APIs dành cho gửi và quản lý nhận xét/đánh giá sản phẩm (JSON output)")
@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewApiController {

    @Autowired
    private ProductReviewDAO productReviewDAO;

    @Operation(summary = "Lấy danh sách đánh giá của sản phẩm theo mã Code")
    @GetMapping("/product/{productCode}")
    public ResponseEntity<List<ProductReview>> getReviewsByProductCode(@PathVariable("productCode") String productCode) {
        List<ProductReview> reviews = productReviewDAO.getReviewsByProductCode(productCode);
        return ResponseEntity.ok(reviews);
    }

    @Operation(summary = "Gửi bài đánh giá mới cho sản phẩm (JSON Payload)")
    @PostMapping
    public ResponseEntity<?> saveReview(@RequestBody ProductReviewForm reviewForm) {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) ? auth.getName() : "customer_qa";

        if (reviewForm.getProductCode() == null || reviewForm.getProductCode().trim().isEmpty() ||
            reviewForm.getComment() == null || reviewForm.getComment().trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Vui lòng nhập đầy đủ productCode và nội dung comment!");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            ProductReview review = new ProductReview(reviewForm.getProductCode().trim(), username, reviewForm.getRatingValue(), reviewForm.getComment().trim());
            productReviewDAO.saveReview(review);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã gửi đánh giá sản phẩm thành công!");
            response.put("review", review);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Lỗi gửi đánh giá: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Operation(summary = "Chỉnh sửa nội dung đánh giá (trong vòng 5 phút sau khi đăng)")
    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable("reviewId") Long reviewId,
            @RequestBody Map<String, Object> payload) {
        
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) ? auth.getName() : "customer_qa";

        Integer ratingValue = payload.get("ratingValue") != null ? ((Number) payload.get("ratingValue")).intValue() : 5;
        String comment = (String) payload.get("comment");

        if (comment == null || comment.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Nội dung nhận xét không được để trống!");
            return ResponseEntity.badRequest().body(error);
        }

        boolean success = productReviewDAO.updateReview(reviewId, username, ratingValue, comment.trim());
        if (success) {
            ProductReview updatedReview = productReviewDAO.findReview(reviewId);
            return ResponseEntity.ok(updatedReview);
        } else {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Không thể sửa bài đánh giá (đã quá 5 phút kể từ lúc đăng hoặc không có quyền sửa).");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "Xóa bài đánh giá theo ID")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable("reviewId") Long reviewId,
            @RequestParam(value = "username", required = false) String paramUsername) {
        
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) ? auth.getName() : (paramUsername != null ? paramUsername : "customer_qa");

        boolean success = productReviewDAO.deleteReview(reviewId, username);
        if (success) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã xóa bài đánh giá thành công!");
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Không thể xóa bài đánh giá (không tồn tại hoặc không phải là người sở hữu).");
            return ResponseEntity.badRequest().body(error);
        }
    }
}
