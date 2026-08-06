package com.example.demo.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.dao.AccountDAO;
import com.example.demo.dao.OrderDAO;
import com.example.demo.entity.Account;
import com.example.demo.form.RegisterForm;
import com.example.demo.form.UserProfileForm;
import com.example.demo.pagination.PaginationResult;
import com.example.demo.validator.RegisterFormValidator;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User Controller", description = "Các API đăng ký, đăng nhập, khôi phục mật khẩu, hồ sơ cá nhân và quản lý người dùng")
@Controller
@Transactional
public class UserController {

    @Autowired
    private AccountDAO accountDAO;

    @Autowired
    private OrderDAO orderDAO;

    @Autowired
    private com.example.demo.dao.WishlistDAO wishlistDAO;

    @Autowired
    private RegisterFormValidator registerFormValidator;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @InitBinder
    public void myInitBinder(WebDataBinder dataBinder) {
        Object target = dataBinder.getTarget();
        if (target == null) {
            return;
        }
        if (target.getClass() == RegisterForm.class) {
            dataBinder.setValidator(registerFormValidator);
        }
    }

    // GET: Show Login Page
    @RequestMapping(value = { "/admin/login" }, method = RequestMethod.GET)
    public String login(Model model) {
        return "login";
    }

    // GET: Display Registration Form
    @RequestMapping(value = { "/register" }, method = RequestMethod.GET)
    public String registerPage(Model model) {
        RegisterForm form = new RegisterForm();
        model.addAttribute("registerForm", form);
        return "register";
    }

    // POST: Process Registration
    @RequestMapping(value = { "/register" }, method = RequestMethod.POST)
    public String registerSave(Model model,
            @ModelAttribute("registerForm") @Validated RegisterForm registerForm,
            BindingResult result,
            final RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "register";
        }

        try {
            Account account = new Account();
            account.setUserName(registerForm.getUserName().trim());
            account.setEmail(registerForm.getEmail().trim().toLowerCase());
            account.setEncrytedPassword(passwordEncoder.encode(registerForm.getPassword()));
            account.setActive(true);
            account.setUserRole(Account.ROLE_USER);
            account.setProvider("LOCAL");

            accountDAO.saveAccount(account);
            redirectAttributes.addFlashAttribute("message", "Đăng ký tài khoản thành công! Vui lòng đăng nhập.");
            return "redirect:/admin/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi tạo tài khoản: " + e.getMessage());
            return "register";
        }
    }

    // GET: Display Forgot Password Form
    @RequestMapping(value = { "/forgotPassword" }, method = RequestMethod.GET)
    public String forgotPasswordPage() {
        return "forgotPassword";
    }

    // POST: Process Forgot Password Request
    @RequestMapping(value = { "/forgotPassword" }, method = RequestMethod.POST)
    public String processForgotPassword(Model model,
            @RequestParam("email") String email,
            final RedirectAttributes redirectAttributes) {

        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("errorMessage", "Vui lòng nhập địa chỉ email của bạn.");
            return "forgotPassword";
        }

        Account account = accountDAO.findAccountByEmail(email.trim().toLowerCase());
        if (account == null) {
            model.addAttribute("errorMessage", "Không tìm thấy tài khoản nào kết nối với email này.");
            return "forgotPassword";
        }

        String token = UUID.randomUUID().toString();
        account.setResetToken(token);
        accountDAO.saveAccount(account);

        model.addAttribute("successMessage", "Mã đặt lại mật khẩu đã được tạo thành công! (Mã Token Demo: " + token + ")");
        model.addAttribute("resetToken", token);
        return "forgotPassword";
    }

    // GET: Display Reset Password Form
    @RequestMapping(value = { "/resetPassword" }, method = RequestMethod.GET)
    public String resetPasswordPage(Model model, @RequestParam(value = "token", required = false) String token) {
        if (token != null && !token.trim().isEmpty()) {
            Account account = accountDAO.findAccountByResetToken(token.trim());
            if (account == null) {
                model.addAttribute("errorMessage", "Mã xác thực không hợp lệ hoặc đã hết hạn.");
                return "forgotPassword";
            }
            model.addAttribute("token", token.trim());
        }
        return "resetPassword";
    }

    // POST: Save New Password
    @RequestMapping(value = { "/resetPassword" }, method = RequestMethod.POST)
    public String processResetPassword(Model model,
            @RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            final RedirectAttributes redirectAttributes) {

        if (token == null || token.trim().isEmpty()) {
            model.addAttribute("errorMessage", "Mã xác thực không hợp lệ.");
            return "resetPassword";
        }

        if (password == null || password.trim().isEmpty()) {
            model.addAttribute("errorMessage", "Vui lòng nhập mật khẩu mới.");
            model.addAttribute("token", token);
            return "resetPassword";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Mật khẩu xác nhận không khớp.");
            model.addAttribute("token", token);
            return "resetPassword";
        }

        Account account = accountDAO.findAccountByResetToken(token.trim());
        if (account == null) {
            model.addAttribute("errorMessage", "Mã xác thực không tồn tại hoặc đã bị hủy.");
            return "forgotPassword";
        }

        account.setEncrytedPassword(passwordEncoder.encode(password));
        account.setResetToken(null);
        accountDAO.saveAccount(account);

        redirectAttributes.addFlashAttribute("message", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập bằng mật khẩu mới.");
        return "redirect:/admin/login";
    }

    // GET: Account Info Dashboard
    @RequestMapping(value = { "/admin/accountInfo" }, method = RequestMethod.GET)
    public String accountInfo(Model model) {
       org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
       Object principal = auth != null ? auth.getPrincipal() : null;

       String username = "";
       if (principal instanceof UserDetails) {
          username = ((UserDetails) principal).getUsername();
       } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
          org.springframework.security.oauth2.core.user.OAuth2User oauthUser = (org.springframework.security.oauth2.core.user.OAuth2User) principal;
          username = oauthUser.getAttribute("name") != null ? (String) oauthUser.getAttribute("name") : oauthUser.getName();
       } else if (auth != null) {
          username = auth.getName();
       }

       String role = (auth != null) ? auth.getAuthorities().stream()
               .map(org.springframework.security.core.GrantedAuthority::getAuthority)
               .filter(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_USER"))
               .findFirst().orElse("") : "";

       model.addAttribute("userName", username);
       model.addAttribute("userRole", role);
       model.addAttribute("totalOrders", orderDAO.getTotalOrdersCount(username, role));
       model.addAttribute("totalRevenue", orderDAO.getTotalRevenue(username, role));
       model.addAttribute("wishlistCount", wishlistDAO.getWishlistCount(username));
       return "accountInfo";
    }

    // GET: Show User Profile (Customer & Admin)
    @RequestMapping(value = { "/admin/user/profile" }, method = RequestMethod.GET)
    public String userProfile(Model model) {
       org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
       if (auth == null || !auth.isAuthenticated()) {
          return "redirect:/admin/login";
       }

       String username = auth.getName();
       Account account = accountDAO.findAccount(username);
       if (account == null && auth.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
          org.springframework.security.oauth2.core.user.OAuth2User oauthUser = (org.springframework.security.oauth2.core.user.OAuth2User) auth.getPrincipal();
          String email = (String) oauthUser.getAttribute("email");
          if (email != null) {
             account = accountDAO.findAccountByEmail(email.toLowerCase());
          }
       }

       if (account == null) {
          return "redirect:/";
       }

       UserProfileForm form = new UserProfileForm();
       form.setUserName(account.getUserName());
       form.setFullName(account.getFullName() != null ? account.getFullName() : account.getUserName());
       form.setEmail(account.getEmail());
       form.setPhoneNumber(account.getPhoneNumber());
       form.setAvatarUrl(account.getAvatarUrl());
       form.setUserRole(account.getUserRole());
       form.setProvider(account.getProvider());
       form.setActive(account.isActive());
       form.setAccountNonLocked(account.isAccountNonLocked());

       model.addAttribute("profileForm", form);
       model.addAttribute("account", account);
       return "userProfile";
    }

    // POST: Save User Profile
    @RequestMapping(value = { "/admin/user/profile" }, method = RequestMethod.POST)
    public String userProfileSave(Model model,
          @ModelAttribute("profileForm") UserProfileForm profileForm,
          final RedirectAttributes redirectAttributes) {

       org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
       if (auth == null || !auth.isAuthenticated()) {
          return "redirect:/admin/login";
       }

       Account account = accountDAO.findAccount(profileForm.getUserName());
       if (account == null) {
          redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông tin tài khoản!");
          return "redirect:/admin/user/profile";
       }

       account.setFullName(profileForm.getFullName());
       account.setEmail(profileForm.getEmail());
       account.setPhoneNumber(profileForm.getPhoneNumber());
       if (profileForm.getAvatarUrl() != null && !profileForm.getAvatarUrl().trim().isEmpty()) {
          account.setAvatarUrl(profileForm.getAvatarUrl().trim());
       }

       // Xử lý Đổi Mật Khẩu (Chỉ áp dụng cho tài khoản đăng nhập LOCAL)
       if (profileForm.getNewPassword() != null && !profileForm.getNewPassword().trim().isEmpty()) {
          if (!"GOOGLE".equalsIgnoreCase(account.getProvider())) {
             if (profileForm.getOldPassword() == null || !passwordEncoder.matches(profileForm.getOldPassword(), account.getEncrytedPassword())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu cũ không chính xác!");
                return "redirect:/admin/user/profile";
             }
             if (!profileForm.getNewPassword().equals(profileForm.getConfirmPassword())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Xác nhận mật khẩu mới không khớp!");
                return "redirect:/admin/user/profile";
             }
             account.setEncrytedPassword(passwordEncoder.encode(profileForm.getNewPassword()));
          }
       }

       accountDAO.saveAccount(account);
       redirectAttributes.addFlashAttribute("message", "Cập nhật hồ sơ cá nhân thành công!");
       return "redirect:/admin/user/profile";
    }

    // GET: Admin List All Users
    @RequestMapping(value = { "/admin/users" }, method = RequestMethod.GET)
    public String userList(Model model,
          @RequestParam(value = "page", defaultValue = "1") String pageStr) {
       org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
       boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
       if (!isAdmin) {
          return "redirect:/403";
       }

       int page = 1;
       try {
          page = Integer.parseInt(pageStr);
       } catch (Exception e) {}

       final int MAX_RESULT = 10;
       final int MAX_NAVIGATION_PAGE = 10;
       PaginationResult<Account> paginationResult = accountDAO.listAccounts(page, MAX_RESULT, MAX_NAVIGATION_PAGE);

       model.addAttribute("paginationResult", paginationResult);
       return "userList";
    }

    // GET: Admin Edit User
    @RequestMapping(value = { "/admin/user/edit" }, method = RequestMethod.GET)
    public String userEdit(Model model, @RequestParam("userName") String userName) {
       org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
       boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
       if (!isAdmin) {
          return "redirect:/403";
       }

       Account account = accountDAO.findAccount(userName);
       if (account == null) {
          return "redirect:/admin/users";
       }

       UserProfileForm form = new UserProfileForm();
       form.setUserName(account.getUserName());
       form.setFullName(account.getFullName() != null ? account.getFullName() : account.getUserName());
       form.setEmail(account.getEmail());
       form.setPhoneNumber(account.getPhoneNumber());
       form.setAvatarUrl(account.getAvatarUrl());
       form.setUserRole(account.getUserRole());
       form.setProvider(account.getProvider());
       form.setActive(account.isActive());
       form.setAccountNonLocked(account.isAccountNonLocked());

       model.addAttribute("profileForm", form);
       model.addAttribute("account", account);
       return "userEdit";
    }

    // POST: Admin Save User
    @RequestMapping(value = { "/admin/user/edit" }, method = RequestMethod.POST)
    public String userEditSave(Model model,
          @ModelAttribute("profileForm") UserProfileForm profileForm,
          final RedirectAttributes redirectAttributes) {

       org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
       boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
       if (!isAdmin) {
          return "redirect:/403";
       }

       Account account = accountDAO.findAccount(profileForm.getUserName());
       if (account == null) {
          redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng!");
          return "redirect:/admin/users";
       }

       account.setFullName(profileForm.getFullName());
       account.setEmail(profileForm.getEmail());
       account.setPhoneNumber(profileForm.getPhoneNumber());
       account.setUserRole(profileForm.getUserRole());
       account.setActive(profileForm.isActive());
       account.setAccountNonLocked(profileForm.isAccountNonLocked());

       accountDAO.saveAccount(account);
       redirectAttributes.addFlashAttribute("message", "Cập nhật người dùng thành công!");
       return "redirect:/admin/users";
    }
}
