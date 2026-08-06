package com.example.demo.dao;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Voucher;
import com.example.demo.entity.VoucherUsage;
import com.example.demo.form.VoucherForm;
import com.example.demo.model.VoucherApplyResult;

@Repository
@Transactional
public class VoucherDAO {

    @Autowired
    private SessionFactory sessionFactory;

    public Voucher findVoucher(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        Session session = this.sessionFactory.getCurrentSession();
        return session.find(Voucher.class, code.trim().toUpperCase());
    }

    public List<Voucher> listActiveVouchers() {
        Session session = this.sessionFactory.getCurrentSession();
        String hql = "Select v from " + Voucher.class.getName() + " v Where v.active = true and (v.expiryDate is null or v.expiryDate >= :now) and v.usedCount < v.usageLimit Order by v.createdAt desc";
        Query<Voucher> query = session.createQuery(hql, Voucher.class);
        query.setParameter("now", new Date());
        return query.getResultList();
    }

    public List<Voucher> listAllVouchers() {
        Session session = this.sessionFactory.getCurrentSession();
        String hql = "Select v from " + Voucher.class.getName() + " v Order by v.createdAt desc";
        Query<Voucher> query = session.createQuery(hql, Voucher.class);
        return query.getResultList();
    }

    public void saveVoucher(VoucherForm form) {
        Session session = this.sessionFactory.getCurrentSession();
        String code = form.getCode().trim().toUpperCase();
        Voucher voucher = session.find(Voucher.class, code);
        if (voucher == null) {
            voucher = new Voucher();
            voucher.setCode(code);
            voucher.setCreatedAt(new Date());
        }
        voucher.setDiscountType(form.getDiscountType());
        voucher.setDiscountValue(form.getDiscountValue());
        voucher.setMaxDiscount(form.getMaxDiscount());
        voucher.setMinOrderValue(form.getMinOrderValue());
        voucher.setExpiryDate(form.getExpiryDate());
        voucher.setActive(form.isActive());
        voucher.setUsageLimit(form.getUsageLimit());
        voucher.setPerUserLimit(form.getPerUserLimit());

        session.saveOrUpdate(voucher);
    }

    public boolean deleteVoucher(String code) {
        Voucher voucher = findVoucher(code);
        if (voucher == null) {
            return false;
        }
        Session session = this.sessionFactory.getCurrentSession();
        voucher.setActive(false);
        session.update(voucher);
        return true;
    }

    public int getUserVoucherUsageCount(String username, String voucherCode) {
        if (username == null || voucherCode == null) {
            return 0;
        }
        Session session = this.sessionFactory.getCurrentSession();
        String hql = "Select count(u) from " + VoucherUsage.class.getName() + " u Where u.username = :username and u.voucherCode = :code";
        Query<Long> query = session.createQuery(hql, Long.class);
        query.setParameter("username", username);
        query.setParameter("code", voucherCode.trim().toUpperCase());
        Long count = query.uniqueResult();
        return count != null ? count.intValue() : 0;
    }

    public void recordVoucherUsage(String voucherCode, String username, String orderId) {
        Voucher voucher = findVoucher(voucherCode);
        if (voucher != null) {
            Session session = this.sessionFactory.getCurrentSession();
            voucher.setUsedCount(voucher.getUsedCount() + 1);
            session.update(voucher);

            VoucherUsage usage = new VoucherUsage(voucherCode, username != null ? username : "guest", orderId);
            session.save(usage);
        }
    }

    public VoucherApplyResult validateAndApplyVoucher(String voucherCode, double orderAmount, String username) {
        if (voucherCode == null || voucherCode.trim().isEmpty()) {
            return new VoucherApplyResult(false, "Vui lòng nhập mã giảm giá!");
        }

        Voucher voucher = findVoucher(voucherCode);
        if (voucher == null || !voucher.isActive()) {
            return new VoucherApplyResult(false, "Mã giảm giá không tồn tại hoặc đã bị vô hiệu hóa!");
        }

        // 1. Validate Expiration
        if (voucher.getExpiryDate() != null && voucher.getExpiryDate().before(new Date())) {
            return new VoucherApplyResult(false, "Mã giảm giá đã hết hạn sử dụng!");
        }

        // 2. Validate Minimum Order Value
        if (orderAmount < voucher.getMinOrderValue()) {
            return new VoucherApplyResult(false, "Đơn hàng tối thiểu phải từ " + (long) voucher.getMinOrderValue() + ".000 ₫ để áp dụng mã này!");
        }

        // 3. Validate Total Usage Limit
        if (voucher.getUsedCount() >= voucher.getUsageLimit()) {
            return new VoucherApplyResult(false, "Mã giảm giá đã hết số lượt sử dụng!");
        }

        // 4. Validate Per-User Limit
        if (username != null && !username.trim().isEmpty()) {
            int userUsed = getUserVoucherUsageCount(username, voucher.getCode());
            if (userUsed >= voucher.getPerUserLimit()) {
                return new VoucherApplyResult(false, "Tài khoản của bạn đã dùng hết số lượt cho phép (" + voucher.getPerUserLimit() + " lần) đối với mã này!");
            }
        }

        // 5. Calculate Discount Correctly
        double discountAmount = 0;
        if (Voucher.TYPE_PERCENT.equalsIgnoreCase(voucher.getDiscountType())) {
            discountAmount = orderAmount * (voucher.getDiscountValue() / 100.0);
            if (voucher.getMaxDiscount() != null && voucher.getMaxDiscount() > 0) {
                discountAmount = Math.min(discountAmount, voucher.getMaxDiscount());
            }
        } else if (Voucher.TYPE_FIXED.equalsIgnoreCase(voucher.getDiscountType())) {
            discountAmount = Math.min(voucher.getDiscountValue(), orderAmount);
        }

        double finalAmount = Math.max(0, orderAmount - discountAmount);

        VoucherApplyResult result = new VoucherApplyResult(true, "Áp dụng mã giảm giá thành công!");
        result.setVoucherCode(voucher.getCode());
        result.setDiscountType(voucher.getDiscountType());
        result.setDiscountValue(voucher.getDiscountValue());
        result.setDiscountAmount(discountAmount);
        result.setOriginalAmount(orderAmount);
        result.setFinalAmount(finalAmount);

        return result;
    }
}
