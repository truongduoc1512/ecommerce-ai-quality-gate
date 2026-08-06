package com.example.demo.controller.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dao.ProductDAO;
import com.example.demo.entity.Product;
import com.example.demo.form.ProductForm;
import com.example.demo.model.ProductInfo;
import com.example.demo.pagination.PaginationResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "1. Product REST API", description = "RESTful APIs dành cho quản lý và tra cứu sản phẩm (JSON output)")
@RestController
@RequestMapping("/api/v1/products")
public class ProductApiController {

    @Autowired
    private ProductDAO productDAO;

    @Operation(summary = "Lấy danh sách sản phẩm có phân trang và bộ lọc")
    @GetMapping
    public ResponseEntity<PaginationResult<ProductInfo>> getProducts(
            @RequestParam(value = "name", defaultValue = "") String likeName,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "sort", defaultValue = "newest") String sort,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "isMall", required = false) Boolean isMall,
            @RequestParam(value = "isFavored", required = false) Boolean isFavored,
            @RequestParam(value = "rating", required = false) Integer rating) {
        
        int maxResult = 12;
        int maxNavigationPage = 10;
        PaginationResult<ProductInfo> result = productDAO.queryProducts(page, maxResult, maxNavigationPage, 
                likeName, null, sort, minPrice, maxPrice, location, brand, isMall, isFavored, rating);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Lấy thông tin chi tiết một sản phẩm theo mã Code")
    @GetMapping("/{code}")
    public ResponseEntity<?> getProductByCode(@PathVariable("code") String code) {
        ProductInfo productInfo = productDAO.findProductInfo(code);
        if (productInfo == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Không tìm thấy sản phẩm với mã: " + code);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        return ResponseEntity.ok(productInfo);
    }

    @Operation(summary = "Tạo mới hoặc cập nhật sản phẩm (JSON Payload)")
    @PostMapping
    public ResponseEntity<?> saveProduct(@RequestBody ProductForm productForm) {
        if (productForm.getCode() == null || productForm.getCode().trim().isEmpty() ||
            productForm.getName() == null || productForm.getName().trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Mã và tên sản phẩm không được để trống!");
            return ResponseEntity.badRequest().body(error);
        }
        try {
            boolean isNew = (productDAO.findProduct(productForm.getCode()) == null);
            productDAO.save(productForm);
            ProductInfo savedProduct = productDAO.findProductInfo(productForm.getCode());
            if (isNew) {
                return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
            }
            return ResponseEntity.ok(savedProduct);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Lỗi lưu sản phẩm: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Operation(summary = "Xóa sản phẩm theo mã Code")
    @DeleteMapping("/{code}")
    public ResponseEntity<?> deleteProduct(@PathVariable("code") String code) {
        Product product = productDAO.findProduct(code);
        if (product == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Không tìm thấy sản phẩm cần xóa với mã: " + code);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        try {
            productDAO.deleteProduct(code);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã xóa sản phẩm thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Không thể xóa sản phẩm: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
