package com.example.demo.validator;

import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.example.demo.dao.AccountDAO;
import com.example.demo.entity.Account;
import com.example.demo.form.RegisterForm;

@Component
public class RegisterFormValidator implements Validator {

    @Autowired
    private AccountDAO accountDAO;

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz == RegisterForm.class;
    }

    @Override
    public void validate(Object target, Errors errors) {
        RegisterForm form = (RegisterForm) target;

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "userName", "NotEmpty.registerForm.userName", "Tên tài khoản không được để trống");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "email", "NotEmpty.registerForm.email", "Email không được để trống");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "NotEmpty.registerForm.password", "Mật khẩu không được để trống");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "confirmPassword", "NotEmpty.registerForm.confirmPassword", "Xác nhận mật khẩu không được để trống");

        if (errors.hasErrors()) {
            return;
        }

        // Check email format
        if (!EmailValidator.getInstance().isValid(form.getEmail())) {
            errors.rejectValue("email", "Pattern.registerForm.email", "Email không hợp lệ");
        }

        // Check password matching
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "Match.registerForm.confirmPassword", "Mật khẩu xác nhận không khớp");
        }

        // Check duplicate username
        Account existingAccount = accountDAO.findAccount(form.getUserName());
        if (existingAccount != null) {
            errors.rejectValue("userName", "Duplicate.registerForm.userName", "Tên tài khoản này đã được sử dụng");
        }

        // Check duplicate email
        Account existingEmailAccount = accountDAO.findAccountByEmail(form.getEmail());
        if (existingEmailAccount != null) {
            errors.rejectValue("email", "Duplicate.registerForm.email", "Email này đã được đăng ký tài khoản khác");
        }
    }
}
