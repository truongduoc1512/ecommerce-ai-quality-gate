package com.example.demo.controller.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dao.ProductDAO;
import com.example.demo.entity.Product;
import com.example.demo.form.ProductForm;
import com.example.demo.model.ProductInfo;
import com.example.demo.pagination.PaginationResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "1. Product REST API", description = "RESTful APIs d?nh cho qu?n l? v? tra c?u s?n ph?m (JSON output)")
@RestController
@RequestMapping("/api/v1/products")
public class ProductApiController {

    @Autowired
    private ProductDAO productDAO;

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Operation(summary = "L?y danh s?ch s?n ph?m c? ph?n trang v? b? l?c")
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

    @Operation(summary = "L?y th?ng tin chi ti?t m?t s?n ph?m theo m? Code")
    @GetMapping("/{code}")
    public ResponseEntity<?> getProductByCode(@PathVariable("code") String code) {
        ProductInfo productInfo = productDAO.findProductInfo(code);
        if (productInfo == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Kh?ng t?m th?y s?n ph?m v?i m?: " + code);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        return ResponseEntity.ok(productInfo);
    }

    @Operation(summary = "Ki?m tra ch?t l??ng ?nh s?n ph?m Realtime qua AI Service")
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeImageRealtime(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("approved", false);
            err.put("status", "REJECTED");
            err.put("reason", "File tai len rong (0 bytes). Vui long chon mot file anh hop le.");
            return ResponseEntity.badRequest().body(err);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource imageResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "product.jpg";
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", imageResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            String targetUrl = aiServiceUrl + "/api/v1/analyze";
            ResponseEntity<Map> response = restTemplate.postForEntity(targetUrl, requestEntity, Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception ex) {
            Map<String, Object> err = new HashMap<>();
            err.put("approved", false);
            err.put("status", "ERROR");
            err.put("reason", "Khong the ket noi den AI Service: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(err);
        }
    }

    @Operation(summary = "T?o m?i ho?c c?p nh?t s?n ph?m (JSON Payload)")
    @PostMapping
    public ResponseEntity<?> saveProduct(@RequestBody ProductForm productForm) {
        if (productForm.getCode() == null || productForm.getCode().trim().isEmpty() ||
            productForm.getName() == null || productForm.getName().trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "M? v? t?n s?n ph?m kh?ng ???c ?? tr?ng!");
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
            error.put("message", "L?i l?u s?n ph?m: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Operation(summary = "X?a s?n ph?m theo m? Code")
    @DeleteMapping("/{code}")
    public ResponseEntity<?> deleteProduct(@PathVariable("code") String code) {
        Product product = productDAO.findProduct(code);
        if (product == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Kh?ng t?m th?y s?n ph?m c?n x?a v?i m?: " + code);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        try {
            productDAO.deleteProduct(code);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "?? x?a s?n ph?m th?nh c?ng!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Kh?ng th? x?a s?n ph?m: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
