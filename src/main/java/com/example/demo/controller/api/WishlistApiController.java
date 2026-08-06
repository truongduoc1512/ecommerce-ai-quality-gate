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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dao.ProductDAO;
import com.example.demo.dao.WishlistDAO;
import com.example.demo.entity.Product;
import com.example.demo.model.ProductInfo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "6. Wishlist REST API", description = "RESTful APIs dành cho danh sách sản phẩm yêu thích (JSON output)")
@RestController
@RequestMapping("/api/v1/wishlist")
public class WishlistApiController {

    @Autowired
    private WishlistDAO wishlistDAO;

    @Autowired
    private ProductDAO productDAO;

    private String getCurrentUsername() {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }

    @Operation(summary = "Lấy danh sách các sản phẩm yêu thích của người dùng đang đăng nhập")
    @GetMapping
    public ResponseEntity<?> getWishlist() {
        String username = getCurrentUsername();
        if (username == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Vui lòng đăng nhập để xem danh sách yêu thích!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        List<ProductInfo> wishlist = wishlistDAO.getUserWishlistProducts(username);
        return ResponseEntity.ok(wishlist);
    }

    @Operation(summary = "Kiểm tra xem sản phẩm có trong danh sách yêu thích không")
    @GetMapping("/check/{productCode}")
    public ResponseEntity<?> checkWishlist(@PathVariable("productCode") String productCode) {
        String username = getCurrentUsername();
        boolean isFav = false;
        if (username != null && productCode != null) {
            isFav = wishlistDAO.isFavorite(username, productCode);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("favorite", isFav);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Thêm hoặc Hủy yêu thích sản phẩm (Toggle 1 chạm)")
    @PostMapping("/{productCode}")
    public ResponseEntity<?> toggleWishlist(@PathVariable("productCode") String productCode) {
        String username = getCurrentUsername();
        if (username == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Vui lòng đăng nhập tài khoản để thả tim / thêm sản phẩm yêu thích!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        Product product = productDAO.findProduct(productCode);
        if (product == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Không tìm thấy sản phẩm với mã: " + productCode);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        boolean isFav = wishlistDAO.isFavorite(username, productCode);
        boolean newFavState;
        String message;

        if (isFav) {
            wishlistDAO.removeWishlist(username, productCode);
            newFavState = false;
            message = "Đã xóa sản phẩm khỏi danh sách yêu thích!";
        } else {
            wishlistDAO.addWishlist(username, productCode);
            newFavState = true;
            message = "Đã thêm sản phẩm vào danh sách yêu thích!";
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("favorite", newFavState);
        response.put("message", message);
        response.put("wishlistCount", wishlistDAO.getWishlistCount(username));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Xóa một sản phẩm khỏi danh sách yêu thích")
    @DeleteMapping("/{productCode}")
    public ResponseEntity<?> removeWishlist(@PathVariable("productCode") String productCode) {
        String username = getCurrentUsername();
        if (username == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Vui lòng đăng nhập!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        boolean success = wishlistDAO.removeWishlist(username, productCode);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("favorite", false);
        response.put("message", success ? "Đã xóa sản phẩm khỏi danh sách yêu thích!" : "Sản phẩm không có trong danh sách yêu thích!");
        response.put("wishlistCount", wishlistDAO.getWishlistCount(username));
        return ResponseEntity.ok(response);
    }
}
