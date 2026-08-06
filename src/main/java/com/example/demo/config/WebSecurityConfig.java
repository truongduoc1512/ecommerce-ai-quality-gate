package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.example.demo.service.CustomOAuth2UserService;
import com.example.demo.service.UserDetailsServiceImpl;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        return bCryptPasswordEncoder;
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.csrf().disable();

        // Public endpoints
        http.authorizeRequests().antMatchers("/", "/productList", "/productImage", "/productDetail", "/product/review", "/register", "/forgotPassword", "/resetPassword", "/oauth2/**", "/login/oauth2/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/api/v1/**").permitAll();

        // Requires login with role ROLE_USER or ROLE_ADMIN.
        http.authorizeRequests().antMatchers("/wishlist", "/admin/orderList", "/admin/order", "/admin/accountInfo", "/admin/user/profile", "/product/review/edit", "/product/review/delete")
                .access("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')");

        // Pages only for ADMIN
        http.authorizeRequests().antMatchers("/admin/product", "/admin/deleteProduct", "/admin/users", "/admin/user/edit").access("hasRole('ROLE_ADMIN')");

        http.authorizeRequests().and().exceptionHandling().accessDeniedPage("/403");

        // Configuration for Form Login
        http.authorizeRequests().and().formLogin()
                .loginProcessingUrl("/j_spring_security_check")
                .loginPage("/admin/login")
                .defaultSuccessUrl("/")
                .failureUrl("/admin/login?error=true")
                .usernameParameter("userName")
                .passwordParameter("password")
                .and().logout().logoutUrl("/admin/logout").logoutSuccessUrl("/");

        // Configuration for Google OAuth2 Login
        http.oauth2Login()
                .loginPage("/admin/login")
                .defaultSuccessUrl("/")
                .userInfoEndpoint()
                .userService(customOAuth2UserService);
    }
}
