package com.example.demo.dao;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderDetail;
import com.example.demo.entity.Product;

import com.example.demo.model.CartInfo;
import com.example.demo.model.CartLineInfo;
import com.example.demo.model.CustomerInfo;
import com.example.demo.model.OrderDetailInfo;
import com.example.demo.model.OrderInfo;
import com.example.demo.pagination.PaginationResult;

@Transactional
@Repository
public class OrderDAO {
 
    @Autowired
    private SessionFactory sessionFactory;
 
    @Autowired
    private ProductDAO productDAO;

    @Autowired
    private VoucherDAO voucherDAO;
 
    private int getMaxOrderNum() {
        String sql = "Select max(o.orderNum) from " + Order.class.getName() + " o ";
        Session session = this.sessionFactory.getCurrentSession();
        Query<Integer> query = session.createQuery(sql, Integer.class);
        Integer value = (Integer) query.getSingleResult();
        if (value == null) {
            return 0;
        }
        return value;
    }
 
    @Transactional(rollbackFor = Exception.class)
    public void saveOrder(CartInfo cartInfo) {
        Session session = this.sessionFactory.getCurrentSession();
 
        int orderNum = this.getMaxOrderNum() + 1;
        Order order = new Order();
 
        order.setId(UUID.randomUUID().toString());
        order.setOrderNum(orderNum);
        order.setOrderDate(new Date());
        order.setAmount(cartInfo.getFinalAmount());
        order.setStatus("PENDING");
 
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            order.setCustomerUsername(auth.getName());
        }

        CustomerInfo customerInfo = cartInfo.getCustomerInfo();
        order.setCustomerName(customerInfo.getName());
        order.setCustomerEmail(customerInfo.getEmail());
        order.setCustomerPhone(customerInfo.getPhone());
        order.setCustomerAddress(customerInfo.getAddress());
 
        session.persist(order);
 
        List<CartLineInfo> lines = cartInfo.getCartLines();
 
        for (CartLineInfo line : lines) {
            String code = line.getProductInfo().getCode();
            Product product = this.productDAO.findProduct(code);
            if (product == null || product.getStockQuantity() < line.getQuantity()) {
                int available = (product != null) ? product.getStockQuantity() : 0;
                String name = (product != null) ? product.getName() : code;
                throw new RuntimeException("Sản phẩm '" + name + "' không đủ số lượng trong kho! (Chỉ còn " + available + " sản phẩm)");
            }

            // Deduct inventory & increase sales count
            product.setStockQuantity(product.getStockQuantity() - line.getQuantity());
            product.setSalesCount(product.getSalesCount() + line.getQuantity());
            session.update(product);

            OrderDetail detail = new OrderDetail();
            detail.setId(UUID.randomUUID().toString());
            detail.setOrder(order);
            detail.setAmount(line.getAmount());
            detail.setPrice(line.getProductInfo().getPrice());
            detail.setQuanity(line.getQuantity());
            detail.setProduct(product);

            session.persist(detail);
        }

        if (cartInfo.getVoucherCode() != null && !cartInfo.getVoucherCode().trim().isEmpty()) {
            voucherDAO.recordVoucherUsage(cartInfo.getVoucherCode(), order.getCustomerUsername(), order.getId());
        }
 
        // Order Number!
        cartInfo.setOrderNum(orderNum);
        // Flush
        session.flush();
    }
 
    // @page = 1, 2, ...
    public PaginationResult<OrderInfo> listOrderInfo(int page, int maxResult, int maxNavigationPage, String username, String role) {
        String sql = "Select new " + OrderInfo.class.getName()//
                + "(ord.id, ord.orderDate, ord.orderNum, ord.amount, "
                + " ord.customerName, ord.customerAddress, ord.customerEmail, ord.customerPhone, ord.status) " + " from "
                + Order.class.getName() + " ord ";
        
        boolean isUser = "ROLE_USER".equals(role) || "ROLE_CUSTOMER".equals(role);
        boolean isAdmin = "ROLE_ADMIN".equals(role) || "ROLE_MANAGER".equals(role);
        
        if (isUser && username != null && !username.trim().isEmpty()) {
            sql += " where ord.customerUsername = :username ";
        } else if (isAdmin && username != null && !username.trim().isEmpty()) {
            sql += " where (exists (select 1 from com.example.demo.entity.OrderDetail od where od.order.id = ord.id and od.product.ownerUsername = :username) or ord.customerUsername = :username) ";
        }
        
        sql += " order by ord.orderNum desc";

        Session session = this.sessionFactory.getCurrentSession();
        Query<OrderInfo> query = session.createQuery(sql, OrderInfo.class);
        
        if (sql.contains(":username")) {
            query.setParameter("username", username != null ? username : "");
        }
        return new PaginationResult<OrderInfo>(query, page, maxResult, maxNavigationPage);
    }
 
    public PaginationResult<OrderInfo> listOrderInfo(int page, int maxResult, int maxNavigationPage) {
        return listOrderInfo(page, maxResult, maxNavigationPage, null, null);
    }

    public Order findOrder(String orderId) {
        Session session = this.sessionFactory.getCurrentSession();
        return session.find(Order.class, orderId);
    }
 
    public OrderInfo getOrderInfo(String orderId) {
        Order order = this.findOrder(orderId);
        if (order == null) {
            return null;
        }
        return new OrderInfo(order.getId(), order.getOrderDate(), //
                order.getOrderNum(), order.getAmount(), order.getCustomerName(), //
                order.getCustomerAddress(), order.getCustomerEmail(), order.getCustomerPhone(), order.getStatus());
    }
 
    public List<OrderDetailInfo> listOrderDetailInfos(String orderId) {
        String sql = "Select new " + OrderDetailInfo.class.getName() //
                + "(d.id, d.product.code, d.product.name , d.quanity,d.price,d.amount) "//
                + " from " + OrderDetail.class.getName() + " d "//
                + " where d.order.id = :orderId ";

        Session session = this.sessionFactory.getCurrentSession();
        Query<OrderDetailInfo> query = session.createQuery(sql, OrderDetailInfo.class);
        query.setParameter("orderId", orderId);

        return query.getResultList();
    }
 
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatus(String orderId, String status) {
        Order order = this.findOrder(orderId);
        if (order != null) {
            order.setStatus(status);
            this.sessionFactory.getCurrentSession().flush();
        }
    }
 
    public long getTotalOrdersCount(String username, String role) {
        String sql = "Select count(o.id) from " + Order.class.getName() + " o ";
        
        boolean isUser = "ROLE_USER".equals(role) || "ROLE_CUSTOMER".equals(role);
        boolean isAdmin = "ROLE_ADMIN".equals(role) || "ROLE_MANAGER".equals(role);
        
        if (isUser && username != null && !username.trim().isEmpty()) {
            sql = "Select count(o.id) from " + Order.class.getName() + " o Where o.customerUsername = :username ";
        } else if (isAdmin && username != null && !username.trim().isEmpty()) {
            sql = "Select count(distinct o.id) from " + Order.class.getName() + " o join com.example.demo.entity.OrderDetail od on o.id = od.order.id join com.example.demo.entity.Product p on od.product.code = p.code Where (p.ownerUsername = :username or o.customerUsername = :username) ";
        }
        
        Session session = this.sessionFactory.getCurrentSession();
        Query<Long> query = session.createQuery(sql, Long.class);
        if (sql.contains(":username")) {
            query.setParameter("username", username != null ? username : "");
        }
        Long val = query.getSingleResult();
        return val != null ? val : 0L;
    }

    public long getTotalOrdersCount() {
        return getTotalOrdersCount(null, null);
    }
 
    public double getTotalRevenue(String username, String role) {
        String sql = "Select sum(o.amount) from " + Order.class.getName() + " o ";
        
        boolean isUser = "ROLE_USER".equals(role) || "ROLE_CUSTOMER".equals(role);
        boolean isAdmin = "ROLE_ADMIN".equals(role) || "ROLE_MANAGER".equals(role);
        
        if (isUser && username != null && !username.trim().isEmpty()) {
            sql = "Select sum(o.amount) from " + Order.class.getName() + " o Where o.customerUsername = :username ";
        } else if (isAdmin && username != null && !username.trim().isEmpty()) {
            sql = "Select sum(od.amount) from com.example.demo.entity.OrderDetail od join com.example.demo.entity.Product p on od.product.code = p.code Where (p.ownerUsername = :username or od.order.customerUsername = :username) ";
        }
        
        Session session = this.sessionFactory.getCurrentSession();
        Query<Double> query = session.createQuery(sql, Double.class);
        if (sql.contains(":username")) {
            query.setParameter("username", username != null ? username : "");
        }
        Double val = query.getSingleResult();
        return val != null ? val : 0.0;
    }

    public double getTotalRevenue() {
        return getTotalRevenue(null, null);
    }
}