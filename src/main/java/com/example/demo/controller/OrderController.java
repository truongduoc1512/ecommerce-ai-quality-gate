package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.dao.OrderDAO;
import com.example.demo.dao.ProductDAO;
import com.example.demo.entity.Order;
import com.example.demo.entity.Product;
import com.example.demo.model.OrderDetailInfo;
import com.example.demo.model.OrderInfo;
import com.example.demo.pagination.PaginationResult;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Order Controller", description = "Các API quản lý, danh sách và chi tiết đơn hàng")
@Controller
@Transactional
public class OrderController {

   @Autowired
   private OrderDAO orderDAO;

   @Autowired
   private ProductDAO productDAO;

   // GET: Admin/Seller Order List
   @RequestMapping(value = { "/admin/orderList" }, method = RequestMethod.GET)
   public String orderList(Model model,
         @RequestParam(value = "page", defaultValue = "1") String pageStr) {
      int page = 1;
      try {
         page = Integer.parseInt(pageStr);
      } catch (Exception e) {
      }
      final int MAX_RESULT = 5;
      final int MAX_NAVIGATION_PAGE = 10;

      org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      String username = auth.getName();
      String role = auth.getAuthorities().stream()
              .map(org.springframework.security.core.GrantedAuthority::getAuthority)
              .filter(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_USER"))
              .findFirst().orElse("");

      PaginationResult<OrderInfo> paginationResult
            = orderDAO.listOrderInfo(page, MAX_RESULT, MAX_NAVIGATION_PAGE, username, role);

      model.addAttribute("paginationResult", paginationResult);
      return "orderList";
   }

   // GET: View Order Detail
   @RequestMapping(value = { "/admin/order" }, method = RequestMethod.GET)
   public String orderView(Model model, @RequestParam("orderId") String orderId) {
      OrderInfo orderInfo = null;
      if (orderId != null) {
         orderInfo = this.orderDAO.getOrderInfo(orderId);
      }
      if (orderInfo == null) {
         return "redirect:/admin/orderList";
      }

      org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      String username = auth.getName();
      boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
      boolean isUser = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
      
      if (isUser) {
          Order orderEntity = orderDAO.findOrder(orderId);
          if (orderEntity == null || !username.equals(orderEntity.getCustomerUsername())) {
              return "redirect:/admin/orderList";
          }
      } else if (isAdmin) {
          boolean ownsAny = orderDAO.listOrderDetailInfos(orderId).stream()
                  .anyMatch(d -> {
                      Product p = productDAO.findProduct(d.getProductCode());
                      return p != null && username.equals(p.getOwnerUsername());
                  });
          if (!ownsAny) {
              return "redirect:/admin/orderList";
          }
      }

      List<OrderDetailInfo> details = this.orderDAO.listOrderDetailInfos(orderId);
      orderInfo.setDetails(details);

      model.addAttribute("orderInfo", orderInfo);

      return "order";
   }

   // POST: Update order status
   @RequestMapping(value = { "/admin/order/updateStatus" }, method = RequestMethod.POST)
   public String updateOrderStatus(Model model, 
         @RequestParam("orderId") String orderId,
         @RequestParam("status") String status,
         final RedirectAttributes redirectAttributes) {
      org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
      if (!isAdmin) {
         return "redirect:/403";
      }

      if (orderId != null && status != null) {
         try {
            orderDAO.updateOrderStatus(orderId, status);
            redirectAttributes.addFlashAttribute("message", "Cập nhật trạng thái đơn hàng thành công!");
         } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật trạng thái thất bại: " + e.getMessage());
         }
      }
      return "redirect:/admin/order?orderId=" + orderId;
   }
}
