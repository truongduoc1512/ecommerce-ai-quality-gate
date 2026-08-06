package com.example.demo.controller.api;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dao.OrderDAO;
import com.example.demo.dao.ProductDAO;
import com.example.demo.entity.Product;
import com.example.demo.form.CustomerForm;
import com.example.demo.model.CartInfo;
import com.example.demo.model.CustomerInfo;
import com.example.demo.model.ProductInfo;
import com.example.demo.utils.Utils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "3. Cart REST API", description = "RESTful APIs dành cho giỏ hàng và đặt hàng (JSON output)")
@RestController
@RequestMapping("/api/v1/cart")
public class CartApiController {

    @Autowired
    private ProductDAO productDAO;

    @Autowired
    private OrderDAO orderDAO;

    @Operation(summary = "Xem thông tin giỏ hàng hiện tại trong Session")
    @GetMapping
    public ResponseEntity<CartInfo> getCart(HttpServletRequest request) {
        CartInfo cartInfo = Utils.getCartInSession(request);
        return ResponseEntity.ok(cartInfo);
    }

    @Operation(summary = "Thêm sản phẩm vào giỏ hàng (JSON payload: code, quantity)")
    @PostMapping("/items")
    public ResponseEntity<?> addCartItem(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        String code = (String) payload.get("code");
        Integer quantity = payload.get("quantity") != null ? ((Number) payload.get("quantity")).intValue() : 1;

        if (code == null || code.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Mã sản phẩm 'code' không được để trống!");
            return ResponseEntity.badRequest().body(error);
        }

        Product product = productDAO.findProduct(code.trim());
        if (product == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Sản phẩm không tồn tại!");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        if (product.getStockQuantity() <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Sản phẩm \"" + product.getName() + "\" đã hết hàng trong kho!");
            return ResponseEntity.badRequest().body(error);
        }

        CartInfo cartInfo = Utils.getCartInSession(request);
        ProductInfo productInfo = new ProductInfo(product);
        cartInfo.addProduct(productInfo, quantity);

        return ResponseEntity.ok(cartInfo);
    }

    @Operation(summary = "Cập nhật số lượng sản phẩm trong giỏ hàng")
    @PutMapping("/items")
    public ResponseEntity<?> updateCartItemQuantity(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        String code = (String) payload.get("code");
        Integer quantity = payload.get("quantity") != null ? ((Number) payload.get("quantity")).intValue() : 1;

        if (code == null || code.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Mã sản phẩm 'code' không được để trống!");
            return ResponseEntity.badRequest().body(error);
        }

        Product product = productDAO.findProduct(code.trim());
        if (product == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Sản phẩm không tồn tại!");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        CartInfo cartInfo = Utils.getCartInSession(request);
        int actualQty = quantity;
        boolean capped = false;
        if (quantity > product.getStockQuantity()) {
            actualQty = product.getStockQuantity();
            capped = true;
        }
        cartInfo.updateProduct(code, actualQty);

        Map<String, Object> response = new HashMap<>();
        response.put("cart", cartInfo);
        response.put("actualQuantity", actualQty);
        response.put("capped", capped);
        if (capped) {
            response.put("message", "Chỉ còn " + product.getStockQuantity() + " sản phẩm trong kho!");
        }
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Xóa một sản phẩm khỏi giỏ hàng theo mã Code")
    @DeleteMapping("/items/{code}")
    public ResponseEntity<?> removeCartItem(HttpServletRequest request, @PathVariable("code") String code) {
        Product product = productDAO.findProduct(code);
        CartInfo cartInfo = Utils.getCartInSession(request);
        if (product != null) {
            ProductInfo productInfo = new ProductInfo(product);
            cartInfo.removeProduct(productInfo);
        }
        return ResponseEntity.ok(cartInfo);
    }

    @Operation(summary = "Lưu thông tin giao hàng người mua (Name, Email, Phone, Address)")
    @PostMapping("/customer")
    public ResponseEntity<?> saveCustomerInfo(HttpServletRequest request, @RequestBody CustomerForm customerForm) {
        if (customerForm.getName() == null || customerForm.getName().trim().isEmpty() ||
            customerForm.getEmail() == null || customerForm.getEmail().trim().isEmpty() ||
            customerForm.getPhone() == null || customerForm.getPhone().trim().isEmpty() ||
            customerForm.getAddress() == null || customerForm.getAddress().trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Vui lòng nhập đầy đủ Name, Email, Phone, và Address!");
            return ResponseEntity.badRequest().body(error);
        }

        CartInfo cartInfo = Utils.getCartInSession(request);
        customerForm.setValid(true);
        CustomerInfo customerInfo = new CustomerInfo(customerForm);
        cartInfo.setCustomerInfo(customerInfo);

        return ResponseEntity.ok(cartInfo);
    }

    @Operation(summary = "Tạo đơn hàng từ giỏ hàng hiện tại (Checkout & Submit Order)")
    @PostMapping("/checkout")
    public ResponseEntity<?> checkoutOrder(HttpServletRequest request) {
        CartInfo cartInfo = Utils.getCartInSession(request);

        if (cartInfo.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Giỏ hàng đang trống, không thể đặt hàng!");
            return ResponseEntity.badRequest().body(error);
        }

        if (!cartInfo.isValidCustomer()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Vui lòng cập nhật thông tin người mua (CustomerInfo) trước khi đặt hàng!");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            orderDAO.saveOrder(cartInfo);
            Utils.removeCartInSession(request);
            Utils.storeLastOrderedCartInSession(request, cartInfo);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đặt hàng thành công!");
            response.put("orderedCart", cartInfo);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Lỗi tạo đơn hàng: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
