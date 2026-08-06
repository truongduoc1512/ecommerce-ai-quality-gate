package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.dao.OrderDAO;
import com.example.demo.dao.ProductDAO;
import com.example.demo.entity.Product;
import com.example.demo.form.CustomerForm;
import com.example.demo.model.CartInfo;
import com.example.demo.model.CartLineInfo;
import com.example.demo.model.CustomerInfo;
import com.example.demo.model.ProductInfo;
import com.example.demo.pagination.PaginationResult;
import com.example.demo.utils.Utils;
import com.example.demo.validator.CustomerFormValidator;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Cart Controller", description = "Các API giỏ hàng, cập nhật số lượng và thanh toán")
@Controller
@Transactional
public class CartController {

   @Autowired
   private OrderDAO orderDAO;

   @Autowired
   private ProductDAO productDAO;

   @Autowired
   private CustomerFormValidator customerFormValidator;

   @InitBinder
   public void myInitBinder(WebDataBinder dataBinder) {
      Object target = dataBinder.getTarget();
      if (target == null) {
         return;
      }

      if (target.getClass() == CartInfo.class) {

      } else if (target.getClass() == CustomerForm.class) {
         dataBinder.setValidator(customerFormValidator);
      }
   }

   @RequestMapping({ "/buyProduct" })
   public String listProductHandler(HttpServletRequest request, Model model,
         @RequestParam(value = "code", defaultValue = "") String code,
         final RedirectAttributes redirectAttributes) {

      Product product = null;
      if (code != null && code.length() > 0) {
         product = productDAO.findProduct(code);
      }
      if (product != null) {
         if (product.getStockQuantity() <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm \"" + product.getName() + "\" hiện đã hết hàng trong kho!");
            return "redirect:/productList";
         }
         CartInfo cartInfo = Utils.getCartInSession(request);
         ProductInfo productInfo = new ProductInfo(product);
         cartInfo.addProduct(productInfo, 1);
      }

      return "redirect:/shoppingCart";
   }

   @RequestMapping({ "/addToCart" })
   public String addToCartHandler(HttpServletRequest request, Model model,
         @RequestParam(value = "code", defaultValue = "") String code,
         final RedirectAttributes redirectAttributes) {

      Product product = null;
      if (code != null && code.length() > 0) {
         product = productDAO.findProduct(code);
      }
      if (product != null) {
         if (product.getStockQuantity() <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm \"" + product.getName() + "\" hiện đã hết hàng trong kho!");
            return "redirect:/productList";
         }
         CartInfo cartInfo = Utils.getCartInSession(request);
         ProductInfo productInfo = new ProductInfo(product);
         cartInfo.addProduct(productInfo, 1);
         redirectAttributes.addFlashAttribute("message", "Đã thêm sản phẩm \"" + product.getName() + "\" vào giỏ hàng!");
      }

      return "redirect:/productList";
   }

   @RequestMapping({ "/shoppingCartRemoveProduct" })
   public String removeProductHandler(HttpServletRequest request, Model model,
         @RequestParam(value = "code", defaultValue = "") String code) {
      Product product = null;
      if (code != null && code.length() > 0) {
         product = productDAO.findProduct(code);
      }
      if (product != null) {
         CartInfo cartInfo = Utils.getCartInSession(request);
         ProductInfo productInfo = new ProductInfo(product);
         cartInfo.removeProduct(productInfo);
      }

      return "redirect:/shoppingCart";
   }

   // POST: Update quantity for product in cart
   @RequestMapping(value = { "/shoppingCart" }, method = RequestMethod.POST)
   public String shoppingCartUpdateQty(HttpServletRequest request,
         Model model,
         @ModelAttribute("cartForm") CartInfo cartForm) {

      CartInfo cartInfo = Utils.getCartInSession(request);
      cartInfo.updateQuantity(cartForm);

      return "redirect:/shoppingCart";
   }

   // GET: Show cart.
   @RequestMapping(value = { "/shoppingCart" }, method = RequestMethod.GET)
   public String shoppingCartHandler(HttpServletRequest request, Model model) {
      CartInfo myCart = Utils.getCartInSession(request);
      CartInfo cartInfo = Utils.getCartInSession(request);

      model.addAttribute("cartForm", myCart);
      model.addAttribute("myCart", cartInfo);
      
      // Query 4 recommended products for the "You May Also Like" section
      PaginationResult<ProductInfo> recommendedProducts = productDAO.queryProducts(1, 4, 5, null, null, null, null, null, null, null, null, null, null);
      if (recommendedProducts != null) {
         model.addAttribute("recommendedProducts", recommendedProducts.getList());
      }
      
      return "shoppingCart";
   }

   // GET: Enter customer information.
   @RequestMapping(value = { "/shoppingCartCustomer" }, method = RequestMethod.GET)
   public String shoppingCartCustomerForm(HttpServletRequest request, Model model) {
      CartInfo cartInfo = Utils.getCartInSession(request);

      if (cartInfo.isEmpty()) {
         return "redirect:/shoppingCart";
      }
      CustomerInfo customerInfo = cartInfo.getCustomerInfo();
      CustomerForm customerForm = new CustomerForm(customerInfo);

      model.addAttribute("customerForm", customerForm);
      return "shoppingCartCustomer";
   }

   // POST: Save customer information.
   @RequestMapping(value = { "/shoppingCartCustomer" }, method = RequestMethod.POST)
   public String shoppingCartCustomerSave(HttpServletRequest request,
         Model model,
         @ModelAttribute("customerForm") @Validated CustomerForm customerForm,
         BindingResult result,
         final RedirectAttributes redirectAttributes) {

      if (result.hasErrors()) {
         customerForm.setValid(false);
         return "shoppingCartCustomer";
      }

      customerForm.setValid(true);
      CartInfo cartInfo = Utils.getCartInSession(request);
      CustomerInfo customerInfo = new CustomerInfo(customerForm);
      cartInfo.setCustomerInfo(customerInfo);

      return "redirect:/shoppingCartConfirmation";
   }

   // GET: Show information to confirm.
   @RequestMapping(value = { "/shoppingCartConfirmation" }, method = RequestMethod.GET)
   public String shoppingCartConfirmationReview(HttpServletRequest request, Model model) {
      CartInfo cartInfo = Utils.getCartInSession(request);

      if (cartInfo == null || cartInfo.isEmpty()) {
         return "redirect:/shoppingCart";
      } else if (!cartInfo.isValidCustomer()) {
         return "redirect:/shoppingCartCustomer";
      }
      model.addAttribute("myCart", cartInfo);

      return "shoppingCartConfirmation";
   }

   // POST: Submit Cart (Save)
   @RequestMapping(value = { "/shoppingCartConfirmation" }, method = RequestMethod.POST)
   public String shoppingCartConfirmationSave(HttpServletRequest request, Model model) {
      CartInfo cartInfo = Utils.getCartInSession(request);

      if (cartInfo.isEmpty()) {
         return "redirect:/shoppingCart";
      } else if (!cartInfo.isValidCustomer()) {
         return "redirect:/shoppingCartCustomer";
      }
      try {
         orderDAO.saveOrder(cartInfo);
      } catch (Exception e) {
         e.printStackTrace();
         return "shoppingCartConfirmation";
      }

      Utils.removeCartInSession(request);
      Utils.storeLastOrderedCartInSession(request, cartInfo);

      return "redirect:/shoppingCartFinalize";
   }

   @RequestMapping(value = { "/shoppingCartFinalize" }, method = RequestMethod.GET)
   public String shoppingCartFinalize(HttpServletRequest request, Model model) {
      CartInfo lastOrderedCart = Utils.getLastOrderedCartInSession(request);

      if (lastOrderedCart == null) {
         return "redirect:/shoppingCart";
      }
      model.addAttribute("lastOrderedCart", lastOrderedCart);
      return "shoppingCartFinalize";
   }

   @ResponseBody
   @RequestMapping(value = { "/api/updateCartQuantity" }, method = RequestMethod.POST)
   public Map<String, Object> updateCartQuantityAjax(HttpServletRequest request,
           @RequestParam("code") String code,
           @RequestParam("quantity") int quantity) {
       Map<String, Object> response = new HashMap<>();
       CartInfo cartInfo = Utils.getCartInSession(request);
       Product product = productDAO.findProduct(code);
       if (product != null) {
           int actualQty = quantity;
           boolean capped = false;
           if (quantity > product.getStockQuantity()) {
               actualQty = product.getStockQuantity();
               capped = true;
           }
           cartInfo.updateProduct(code, actualQty);
           double lineAmount = 0;
           for (CartLineInfo line : cartInfo.getCartLines()) {
               if (line.getProductInfo().getCode().equals(code)) {
                   lineAmount = line.getAmount();
                   break;
               }
           }
           response.put("success", true);
           response.put("lineAmount", lineAmount);
           response.put("quantityTotal", cartInfo.getQuantityTotal());
           response.put("amountTotal", cartInfo.getAmountTotal());
           response.put("actualQuantity", actualQty);
           response.put("capped", capped);
           if (capped) {
               response.put("message", "Chỉ còn " + product.getStockQuantity() + " sản phẩm trong kho!");
           }
       } else {
           response.put("success", false);
           response.put("message", "Sản phẩm không tồn tại!");
       }
       return response;
   }

   @ResponseBody
   @RequestMapping(value = { "/api/removeCartProduct" }, method = RequestMethod.POST)
   public Map<String, Object> removeCartProductAjax(HttpServletRequest request,
           @RequestParam("code") String code) {
       Map<String, Object> response = new HashMap<>();
       CartInfo cartInfo = Utils.getCartInSession(request);
       Product product = productDAO.findProduct(code);
       if (product != null) {
           ProductInfo productInfo = new ProductInfo(product);
           cartInfo.removeProduct(productInfo);
           response.put("success", true);
           response.put("quantityTotal", cartInfo.getQuantityTotal());
           response.put("amountTotal", cartInfo.getAmountTotal());
       } else {
           response.put("success", false);
           response.put("message", "Sản phẩm không tồn tại!");
       }
       return response;
   }
}
