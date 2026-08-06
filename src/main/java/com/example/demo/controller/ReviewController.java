package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.dao.ProductReviewDAO;
import com.example.demo.entity.ProductReview;
import com.example.demo.form.ProductReviewForm;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Review Controller", description = "Các API đánh giá và nhận xét sản phẩm")
@Controller
@Transactional
public class ReviewController {

   @Autowired
   private ProductReviewDAO productReviewDAO;

   // POST: Save Product Review & recalculate rating cache counter (Login required)
   @RequestMapping(value = { "/product/review" }, method = RequestMethod.POST)
   public String saveReview(Model model,
         @ModelAttribute("productReviewForm") ProductReviewForm reviewForm,
         final RedirectAttributes redirectAttributes) {

      org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
         redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập tài khoản để viết đánh giá sản phẩm.");
         return "redirect:/admin/login";
      }

      boolean isManagerOrAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("MANAGER") || a.getAuthority().equals("ADMIN"));
      if (isManagerOrAdmin) {
         redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản Manager/Admin chỉ có quyền xem chi tiết và đọc đánh giá từ khách hàng, không thể tạo bài đánh giá.");
         return "redirect:/productDetail?code=" + reviewForm.getProductCode();
      }

      if (reviewForm.getProductCode() == null || reviewForm.getComment() == null || reviewForm.getComment().trim().isEmpty()) {
         redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập nội dung đánh giá.");
         return "redirect:/productDetail?code=" + reviewForm.getProductCode();
      }

      String username = auth.getName();
      ProductReview review = new ProductReview(reviewForm.getProductCode(), username, reviewForm.getRatingValue(), reviewForm.getComment().trim());
      productReviewDAO.saveReview(review);

      redirectAttributes.addFlashAttribute("reviewMessage", "Cảm ơn bạn đã gửi đánh giá cho sản phẩm!");
      return "redirect:/productDetail?code=" + reviewForm.getProductCode();
   }

   // POST: Edit Product Review (within 5-minute time window)
   @RequestMapping(value = { "/product/review/edit" }, method = RequestMethod.POST)
   public String editReview(@RequestParam("reviewId") Long reviewId,
         @RequestParam("productCode") String productCode,
         @RequestParam("ratingValue") int ratingValue,
         @RequestParam("comment") String comment,
         final RedirectAttributes redirectAttributes) {

      org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
         return "redirect:/admin/login";
      }

      boolean success = productReviewDAO.updateReview(reviewId, auth.getName(), ratingValue, comment.trim());
      if (success) {
         redirectAttributes.addFlashAttribute("reviewMessage", "Đã cập nhật bài đánh giá thành công!");
      } else {
         redirectAttributes.addFlashAttribute("errorMessage", "Không thể sửa bài đánh giá (đã quá 5 phút kể từ lúc đăng hoặc bạn không có quyền sửa).");
      }

      return "redirect:/productDetail?code=" + productCode;
   }

   // POST: Delete Product Review (Owner only)
   @RequestMapping(value = { "/product/review/delete" }, method = RequestMethod.POST)
   public String deleteReview(@RequestParam("reviewId") Long reviewId,
         @RequestParam("productCode") String productCode,
         final RedirectAttributes redirectAttributes) {

      org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
         return "redirect:/admin/login";
      }

      boolean success = productReviewDAO.deleteReview(reviewId, auth.getName());
      if (success) {
         redirectAttributes.addFlashAttribute("reviewMessage", "Đã xóa bài đánh giá của bạn.");
      } else {
         redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa bài đánh giá này.");
      }

      return "redirect:/productDetail?code=" + productCode;
   }
}
