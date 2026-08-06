package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.example.demo.dao.WishlistDAO;
import com.example.demo.model.ProductInfo;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Wishlist Controller", description = "Các API hiển thị trang danh sách sản phẩm yêu thích (HTML view)")
@Controller
@Transactional
public class WishlistController {

    @Autowired
    private WishlistDAO wishlistDAO;

    @RequestMapping(value = { "/wishlist" }, method = RequestMethod.GET)
    public String wishlistPage(Model model) {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/admin/login";
        }

        String username = auth.getName();
        List<ProductInfo> wishlist = wishlistDAO.getUserWishlistProducts(username);
        model.addAttribute("wishlistProducts", wishlist);
        model.addAttribute("wishlistCount", wishlist.size());

        return "wishlist";
    }
}
