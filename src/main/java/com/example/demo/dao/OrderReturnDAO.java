package com.example.demo.dao;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderDetail;
import com.example.demo.entity.OrderReturn;
import com.example.demo.entity.Product;
import com.example.demo.form.OrderReturnForm;

@Repository
@Transactional
public class OrderReturnDAO {

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private OrderDAO orderDAO;

    public OrderReturn findReturnByOrderId(String orderId) {
        if (orderId == null) return null;
        Session session = this.sessionFactory.getCurrentSession();
        String hql = "Select r from " + OrderReturn.class.getName() + " r Where r.orderId = :orderId";
        Query<OrderReturn> query = session.createQuery(hql, OrderReturn.class);
        query.setParameter("orderId", orderId);
        List<OrderReturn> list = query.getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(String username, String orderId) {
        Order order = orderDAO.findOrder(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId);
        }

        // Ownership check
        if (username != null && order.getCustomerUsername() != null && !order.getCustomerUsername().equals(username)) {
            throw new IllegalStateException("Bạn không có quyền hủy đơn hàng này!");
        }

        // Rule: Only PENDING orders can be cancelled
        String status = order.getStatus();
        if (status != null && !"PENDING".equalsIgnoreCase(status.trim()) && !"1".equals(status.trim())) {
            throw new IllegalStateException("Chỉ có thể hủy đơn hàng ở trạng thái 'Đang chờ xử lý' (PENDING)!");
        }

        Session session = this.sessionFactory.getCurrentSession();

        // Restore inventory & adjust sales count inside transaction
        List<OrderDetail> details = getOrderDetails(orderId);
        for (OrderDetail detail : details) {
            Product product = detail.getProduct();
            if (product != null) {
                int restoredStock = product.getStockQuantity() + detail.getQuanity();
                int restoredSales = Math.max(0, product.getSalesCount() - detail.getQuanity());
                product.setStockQuantity(restoredStock);
                product.setSalesCount(restoredSales);
                session.update(product);
            }
        }

        // Update order status to CANCELLED
        order.setStatus("CANCELLED");
        session.update(order);
        session.flush();

        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderReturn createReturnRequest(String username, String orderId, OrderReturnForm form) {
        Order order = orderDAO.findOrder(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId);
        }

        // Ownership check
        if (username != null && order.getCustomerUsername() != null && !order.getCustomerUsername().equals(username)) {
            throw new IllegalStateException("Bạn không có quyền yêu cầu trả hàng cho đơn này!");
        }

        // Prevent duplicate return requests
        OrderReturn existing = findReturnByOrderId(orderId);
        if (existing != null) {
            throw new IllegalStateException("Đơn hàng này đã có yêu cầu trả hàng đang được xử lý!");
        }

        Session session = this.sessionFactory.getCurrentSession();

        OrderReturn orderReturn = new OrderReturn();
        orderReturn.setOrderId(orderId);
        orderReturn.setUsername(username != null ? username : order.getCustomerUsername());
        orderReturn.setReason(form.getReason());
        orderReturn.setImageUrls(form.getImageUrls());
        orderReturn.setStatus("PENDING");
        orderReturn.setCreatedAt(new Date());
        orderReturn.setUpdatedAt(new Date());

        session.save(orderReturn);

        // Update Order status tag
        order.setStatus("RETURN_PENDING");
        session.update(order);
        session.flush();

        return orderReturn;
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderReturn updateReturnStatus(String adminUsername, String orderId, String action, String adminNote) {
        OrderReturn orderReturn = findReturnByOrderId(orderId);
        if (orderReturn == null) {
            throw new IllegalArgumentException("Không tìm thấy yêu cầu trả hàng cho đơn #" + orderId);
        }

        Order order = orderDAO.findOrder(orderId);
        Session session = this.sessionFactory.getCurrentSession();

        if ("APPROVE".equalsIgnoreCase(action)) {
            orderReturn.setStatus("APPROVED");
            orderReturn.setAdminNote(adminNote);
            orderReturn.setUpdatedAt(new Date());
            session.update(orderReturn);

            if (order != null) {
                order.setStatus("RETURNED");
                session.update(order);

                // Restore stock quantity upon approved return
                List<OrderDetail> details = getOrderDetails(orderId);
                for (OrderDetail detail : details) {
                    Product product = detail.getProduct();
                    if (product != null) {
                        product.setStockQuantity(product.getStockQuantity() + detail.getQuanity());
                        product.setSalesCount(Math.max(0, product.getSalesCount() - detail.getQuanity()));
                        session.update(product);
                    }
                }
            }
        } else if ("REJECT".equalsIgnoreCase(action)) {
            orderReturn.setStatus("REJECTED");
            orderReturn.setAdminNote(adminNote);
            orderReturn.setUpdatedAt(new Date());
            session.update(orderReturn);

            if (order != null) {
                order.setStatus("COMPLETED");
                session.update(order);
            }
        } else {
            throw new IllegalArgumentException("Hành động không hợp lệ! (Chỉ chấp nhận 'APPROVE' hoặc 'REJECT')");
        }

        session.flush();
        return orderReturn;
    }

    private List<OrderDetail> getOrderDetails(String orderId) {
        Session session = this.sessionFactory.getCurrentSession();
        String hql = "Select d from " + OrderDetail.class.getName() + " d Where d.order.id = :orderId";
        Query<OrderDetail> query = session.createQuery(hql, OrderDetail.class);
        query.setParameter("orderId", orderId);
        return query.getResultList();
    }
}
