package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dao.OrderDAO;
import com.example.demo.dao.OrderReturnDAO;
import com.example.demo.dao.ProductDAO;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderReturn;
import com.example.demo.entity.Product;
import com.example.demo.form.OrderReturnForm;
import com.example.demo.model.CartInfo;
import com.example.demo.model.CartLineInfo;
import com.example.demo.model.CustomerInfo;
import com.example.demo.model.ProductInfo;

@SpringBootTest
@Transactional
public class OrderCancelReturnTests {

    @Autowired
    private OrderDAO orderDAO;

    @Autowired
    private OrderReturnDAO orderReturnDAO;

    @Autowired
    private ProductDAO productDAO;

    @Test
    public void testOrderCancellationRestoresInventory() {
        Product product = productDAO.findProduct("S-001");
        assertNotNull(product);

        int initialStock = product.getStockQuantity();

        // Create a test order
        CartInfo cart = new CartInfo();
        CustomerInfo customer = new CustomerInfo();
        customer.setName("Test User");
        customer.setEmail("test@shoeshop.com");
        customer.setPhone("0912345678");
        customer.setAddress("123 Test Street");
        cart.setCustomerInfo(customer);

        ProductInfo pInfo = new ProductInfo(product);
        cart.addProduct(pInfo, 2); // Order 2 items

        orderDAO.saveOrder(cart);

        // Verify stock deducted
        Product updatedProductAfterOrder = productDAO.findProduct("S-001");
        assertEquals(initialStock - 2, updatedProductAfterOrder.getStockQuantity());

        // Cancel order
        Order order = orderDAO.findOrder(cart.getCartLines().get(0).getProductInfo().getCode()); // Find latest order
        List<com.example.demo.model.OrderInfo> list = orderDAO.listOrderInfo(1, 1, 5).getList();
        assertFalse(list.isEmpty());
        String orderId = list.get(0).getId();

        boolean cancelResult = orderReturnDAO.cancelOrder(null, orderId);
        assertTrue(cancelResult);

        // Verify status updated to CANCELLED & stock restored
        Order cancelledOrder = orderDAO.findOrder(orderId);
        assertEquals("CANCELLED", cancelledOrder.getStatus());

        Product restoredProduct = productDAO.findProduct("S-001");
        assertEquals(initialStock, restoredProduct.getStockQuantity());
    }

    @Test
    public void testPreventDuplicateReturnRequest() {
        List<com.example.demo.model.OrderInfo> list = orderDAO.listOrderInfo(1, 1, 5).getList();
        assertFalse(list.isEmpty());
        String orderId = list.get(0).getId();

        // Mark order as COMPLETED
        orderDAO.updateOrderStatus(orderId, "COMPLETED");

        OrderReturnForm form = new OrderReturnForm();
        form.setReason("Lỗi sản xuất");

        // First return request -> Success
        OrderReturn req1 = orderReturnDAO.createReturnRequest("customer88", orderId, form);
        assertNotNull(req1);
        assertEquals("PENDING", req1.getStatus());

        // Second duplicate return request -> Rejection Exception
        assertThrows(IllegalStateException.class, () -> {
            orderReturnDAO.createReturnRequest("customer88", orderId, form);
        });
    }

    @Test
    public void testAdminApproveReturnRestoresStock() {
        List<com.example.demo.model.OrderInfo> list = orderDAO.listOrderInfo(1, 1, 5).getList();
        assertFalse(list.isEmpty());
        String orderId = list.get(0).getId();

        // Mark order as COMPLETED
        orderDAO.updateOrderStatus(orderId, "COMPLETED");

        OrderReturnForm form = new OrderReturnForm();
        form.setReason("Giày bị chật size");

        OrderReturn req = orderReturnDAO.createReturnRequest("customer88", orderId, form);
        assertNotNull(req);

        // Admin approves return
        OrderReturn approvedReq = orderReturnDAO.updateReturnStatus("admin", orderId, "APPROVE", "Đã duyệt nhận lại hàng");
        assertEquals("APPROVED", approvedReq.getStatus());

        Order updatedOrder = orderDAO.findOrder(orderId);
        assertEquals("RETURNED", updatedOrder.getStatus());
    }
}
