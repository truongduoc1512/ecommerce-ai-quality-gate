package com.example.demo.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Product;
import com.example.demo.entity.ProductReview;

@Repository
@Transactional
public class ProductReviewDAO {

    @Autowired
    private SessionFactory sessionFactory;

    public ProductReview findReview(Long reviewId) {
        if (reviewId == null) {
            return null;
        }
        Session session = this.sessionFactory.getCurrentSession();
        return session.find(ProductReview.class, reviewId);
    }

    public void saveReview(ProductReview review) {
        Session session = this.sessionFactory.getCurrentSession();
        session.save(review);
        recalculateProductCacheCounter(session, review.getProductCode());
    }

    public boolean updateReview(Long reviewId, String username, int newRating, String newComment) {
        Session session = this.sessionFactory.getCurrentSession();
        ProductReview review = session.find(ProductReview.class, reviewId);

        if (review == null || !review.getUsername().equalsIgnoreCase(username)) {
            return false;
        }

        // Check 5 minutes time window (5 * 60 * 1000 ms = 300,000 ms)
        long diff = System.currentTimeMillis() - review.getCreatedAt().getTime();
        if (diff > 5 * 60 * 1000) {
            return false;
        }

        review.setRatingValue(newRating);
        review.setComment(newComment);
        session.update(review);

        recalculateProductCacheCounter(session, review.getProductCode());
        return true;
    }

    public boolean deleteReview(Long reviewId, String username) {
        Session session = this.sessionFactory.getCurrentSession();
        ProductReview review = session.find(ProductReview.class, reviewId);

        if (review == null || !review.getUsername().equalsIgnoreCase(username)) {
            return false;
        }

        String productCode = review.getProductCode();
        session.delete(review);
        recalculateProductCacheCounter(session, productCode);
        return true;
    }

    public List<ProductReview> getReviewsByProductCode(String productCode) {
        Session session = this.sessionFactory.getCurrentSession();
        String hql = "Select r from " + ProductReview.class.getName() + " r Where r.productCode = :code Order by r.createdAt desc";
        Query<ProductReview> query = session.createQuery(hql, ProductReview.class);
        query.setParameter("code", productCode);
        return query.getResultList();
    }

    private void recalculateProductCacheCounter(Session session, String productCode) {
        Product product = session.find(Product.class, productCode);
        if (product != null) {
            String hql = "Select count(r), avg(r.ratingValue) from " + ProductReview.class.getName() + " r Where r.productCode = :code";
            Query<?> query = session.createQuery(hql);
            query.setParameter("code", productCode);
            Object[] result = (Object[]) query.uniqueResult();

            if (result != null) {
                Long count = (Long) result[0];
                Double avg = (Double) result[1];

                if (count != null && count > 0 && avg != null) {
                    BigDecimal roundedAvg = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);
                    product.setReviewCount(count.intValue());
                    product.setRating(roundedAvg.doubleValue());
                } else {
                    product.setReviewCount(0);
                    product.setRating(5.0);
                }
                session.update(product);
            }
        }
    }
}
