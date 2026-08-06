package com.example.demo.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.dao.ProductDAO;
import com.example.demo.dao.ProductReviewDAO;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductReview;
import com.example.demo.form.ProductForm;
import com.example.demo.form.ProductReviewForm;
import com.example.demo.model.ProductInfo;
import com.example.demo.pagination.PaginationResult;
import com.example.demo.validator.ProductFormValidator;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Product Controller", description = "Các API quản lý, danh sách và chi tiết sản phẩm")
@Controller
@Transactional
public class ProductController {

   @Value("${ai.service.url:http://localhost:8000}")
   private String aiServiceUrl;

   private final RestTemplate restTemplate = new RestTemplate();

   @Autowired
   private ProductDAO productDAO;

   @Autowired
   private ProductReviewDAO productReviewDAO;

   @Autowired
   private ProductFormValidator productFormValidator;

   @InitBinder
   public void myInitBinder(WebDataBinder dataBinder) {
      Object target = dataBinder.getTarget();
      if (target == null) {
         return;
      }
      if (target.getClass() == ProductForm.class) {
         dataBinder.setValidator(productFormValidator);
      }
   }

   // GET: Product List
   @RequestMapping({ "/productList" })
   public String listProductHandler(HttpServletRequest request, Model model,
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

      String ownerUsername = null;
      org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
          boolean isManager = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
          if (isManager) {
              ownerUsername = auth.getName();
          }
      }

      PaginationResult<ProductInfo> result = productDAO.queryProducts(page, 
            maxResult, maxNavigationPage, likeName, ownerUsername, sort, minPrice, maxPrice, 
            location, brand, isMall, isFavored, rating);

      model.addAttribute("paginationProducts", result);
      model.addAttribute("likeName", likeName);
      model.addAttribute("sort", sort);
      model.addAttribute("minPrice", minPrice);
      model.addAttribute("maxPrice", maxPrice);
      model.addAttribute("location", location);
      model.addAttribute("brand", brand);
      model.addAttribute("isMall", isMall);
      model.addAttribute("isFavored", isFavored);
      return "productList";
   }

   // GET: Product Detail page with reviews, rating and stock info
   @RequestMapping(value = { "/productDetail" }, method = RequestMethod.GET)
   public String productDetail(Model model, @RequestParam("code") String code) {
      ProductInfo productInfo = productDAO.findProductInfo(code);
      if (productInfo == null) {
         return "redirect:/productList";
      }

      List<ProductReview> reviews = productReviewDAO.getReviewsByProductCode(code);
      ProductReviewForm reviewForm = new ProductReviewForm(code);

      model.addAttribute("productInfo", productInfo);
      model.addAttribute("reviewsList", reviews);
      model.addAttribute("productReviewForm", reviewForm);

      return "productDetail";
   }

   // GET: Product Image
   @RequestMapping(value = { "/productImage" }, method = RequestMethod.GET)
   public void productImage(HttpServletRequest request, HttpServletResponse response, Model model,
         @RequestParam("code") String code) throws IOException {
      Product product = null;
      if (code != null) {
         product = this.productDAO.findProduct(code);
      }
      if (product != null && product.getImage() != null) {
         response.setContentType("image/jpeg, image/jpg, image/png, image/gif");
         response.getOutputStream().write(product.getImage());
      }
      response.getOutputStream().close();
   }

   // GET: Show admin product edit form
   @RequestMapping(value = { "/admin/product" }, method = RequestMethod.GET)
   public String product(Model model, @RequestParam(value = "code", defaultValue = "") String code,
         final RedirectAttributes redirectAttributes) {
      ProductForm productForm = null;

      if (code != null && code.length() > 0) {
         Product product = productDAO.findProduct(code);
         if (product != null) {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            if (!product.getOwnerUsername().equals(currentUsername)) {
               redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền chỉnh sửa sản phẩm của người khác!");
               return "redirect:/productList";
            }
            productForm = new ProductForm(product);
         }
      }
      if (productForm == null) {
         productForm = new ProductForm();
         productForm.setNewProduct(true);
      }
      model.addAttribute("productForm", productForm);
      return "product";
   }

   // POST: Save product
   @RequestMapping(value = { "/admin/product" }, method = RequestMethod.POST)
   public String productSave(Model model,
         @ModelAttribute("productForm") @Validated ProductForm productForm,
         BindingResult result,
         final RedirectAttributes redirectAttributes) {

      if (result.hasErrors()) {
         return "product";
      }

      // ── AI Quality Gate: Kiểm duyệt ảnh trước khi lưu ──────────────────────
      if (productForm.getFileData() != null && !productForm.getFileData().isEmpty()) {
         try {
            byte[] imageBytes = productForm.getFileData().getBytes();
            String originalFilename = productForm.getFileData().getOriginalFilename();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
               @Override
               public String getFilename() {
                  return originalFilename != null ? originalFilename : "product.jpg";
               }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", imageResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String analyzeUrl = aiServiceUrl + "/api/v1/analyze";
            ResponseEntity<Map> response = restTemplate.postForEntity(analyzeUrl, requestEntity, Map.class);

            if (response.getBody() != null) {
               Map<?, ?> aiResult = response.getBody();
               Boolean approved = (Boolean) aiResult.get("approved");
               String reason = (String) aiResult.get("reason");

               if (approved != null && !approved) {
                  model.addAttribute("aiError", reason);
                  model.addAttribute("aiMetrics", aiResult.get("metrics"));
                  return "product";
               }
            }
         } catch (Exception aiEx) {
            System.err.println("[AI-QA] Cảnh báo: Không thể kết nối AI service: " + aiEx.getMessage());
            model.addAttribute("aiWarning", "AI service tạm thời không khả dụng. Ảnh sẽ được lưu mà không qua kiểm duyệt.");
         }
      }

      try {
         productDAO.save(productForm);
         redirectAttributes.addFlashAttribute("message", "Lưu sản phẩm thành công!");
      } catch (Exception e) {
         Throwable rootCause = ExceptionUtils.getRootCause(e);
         String message = rootCause != null ? rootCause.getMessage() : e.getMessage();
         model.addAttribute("errorMessage", message);
         return "product";
      }

      return "redirect:/productList";
   }

   // GET: Delete product
   @RequestMapping(value = { "/admin/deleteProduct" }, method = RequestMethod.GET)
   public String deleteProduct(Model model, @RequestParam(value = "code", defaultValue = "") String code,
         final RedirectAttributes redirectAttributes) {
      if (code != null && code.length() > 0) {
         try {
            Product product = productDAO.findProduct(code);
            if (product != null) {
               String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
               if (!product.getOwnerUsername().equals(currentUsername)) {
                  redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xóa sản phẩm của người khác!");
                  return "redirect:/productList";
               }
            }
            productDAO.deleteProduct(code);
            redirectAttributes.addFlashAttribute("message", "Xóa sản phẩm thành công!");
         } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa sản phẩm: " + e.getMessage());
         }
      }
      return "redirect:/productList";
   }
}
