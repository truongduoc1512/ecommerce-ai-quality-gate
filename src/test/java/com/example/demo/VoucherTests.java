package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dao.VoucherDAO;
import com.example.demo.entity.Voucher;
import com.example.demo.form.VoucherForm;
import com.example.demo.model.VoucherApplyResult;

@SpringBootTest
@Transactional
public class VoucherTests {

    @Autowired
    private VoucherDAO voucherDAO;

    @Test
    public void testPercentageDiscountWithMaxDiscountCap() {
        VoucherForm form = new VoucherForm();
        form.setCode("TESTPERCENT20");
        form.setDiscountType("PERCENT");
        form.setDiscountValue(20.0); // 20%
        form.setMaxDiscount(50.0);   // Tối đa 50K
        form.setMinOrderValue(100.0);
        form.setActive(true);
        form.setUsageLimit(10);
        form.setPerUserLimit(2);
        voucherDAO.saveVoucher(form);

        // Order amount 500K -> 20% of 500 = 100K, capped at 50K
        VoucherApplyResult result = voucherDAO.validateAndApplyVoucher("TESTPERCENT20", 500.0, "testuser");
        assertTrue(result.isSuccess());
        assertEquals(50.0, result.getDiscountAmount());
        assertEquals(450.0, result.getFinalAmount());
    }

    @Test
    public void testFixedDiscountCalculation() {
        VoucherForm form = new VoucherForm();
        form.setCode("TESTFIXED30");
        form.setDiscountType("FIXED");
        form.setDiscountValue(30.0); // 30K
        form.setMinOrderValue(100.0);
        form.setActive(true);
        form.setUsageLimit(10);
        form.setPerUserLimit(2);
        voucherDAO.saveVoucher(form);

        // Order amount 200K -> Fixed 30K discount -> Final 170K
        VoucherApplyResult result = voucherDAO.validateAndApplyVoucher("TESTFIXED30", 200.0, "testuser");
        assertTrue(result.isSuccess());
        assertEquals(30.0, result.getDiscountAmount());
        assertEquals(170.0, result.getFinalAmount());
    }

    @Test
    public void testMinimumOrderValueRejection() {
        VoucherForm form = new VoucherForm();
        form.setCode("TESTMINORDER");
        form.setDiscountType("FIXED");
        form.setDiscountValue(50.0);
        form.setMinOrderValue(500.0); // Tối thiểu 500K
        form.setActive(true);
        voucherDAO.saveVoucher(form);

        // Order amount 200K < 500K -> Reject
        VoucherApplyResult result = voucherDAO.validateAndApplyVoucher("TESTMINORDER", 200.0, "testuser");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("tối thiểu"));
    }

    @Test
    public void testExpiredVoucherRejection() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -5); // Hết hạn 5 ngày trước

        VoucherForm form = new VoucherForm();
        form.setCode("TESTEXPIRED");
        form.setDiscountType("FIXED");
        form.setDiscountValue(20.0);
        form.setMinOrderValue(50.0);
        form.setExpiryDate(cal.getTime());
        form.setActive(true);
        voucherDAO.saveVoucher(form);

        VoucherApplyResult result = voucherDAO.validateAndApplyVoucher("TESTEXPIRED", 200.0, "testuser");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("hết hạn"));
    }

    @Test
    public void testUsageLimitRejection() {
        VoucherForm form = new VoucherForm();
        form.setCode("TESTLIMITREJECT");
        form.setDiscountType("FIXED");
        form.setDiscountValue(20.0);
        form.setMinOrderValue(50.0);
        form.setActive(true);
        form.setUsageLimit(1);
        form.setPerUserLimit(5);
        voucherDAO.saveVoucher(form);

        // Record 1 usage
        voucherDAO.recordVoucherUsage("TESTLIMITREJECT", "user1", "ORD-001");

        VoucherApplyResult result = voucherDAO.validateAndApplyVoucher("TESTLIMITREJECT", 200.0, "user2");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("hết số lượt"));
    }
}
