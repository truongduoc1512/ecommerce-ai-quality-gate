package com.example.demo.dao;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Product;
import com.example.demo.entity.Wishlist;
import com.example.demo.model.ProductInfo;

@Repository
@Transactional
public class WishlistDAO {

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private ProductDAO productDAO;

    public Wishlist findWishlist(String username, String productCode) {
        if (username == null || productCode == null) {
            return null;
        }
        Session session = this.sessionFactory.getCurrentSession();
        String hql = "Select w from " + Wishlist.class.getName() + " w Where w.username = :username and w.productCode = :code";
        Query<Wishlist> query = session.createQuery(hql, Wishlist.class);
        query.setParameter("username", username);
        query.setParameter("code", productCode);
        List<Wishlist> list = query.getResultList();
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    public boolean isFavorite(String username, String productCode) {
        return findWishlist(username, productCode) != null;
    }

    public boolean addWishlist(String username, String productCode) {
        if (username == null || productCode == null) {
            return false;
        }
        Product product = productDAO.findProduct(productCode);
        if (product == null) {
            return false;
        }

        Wishlist existing = findWishlist(username, productCode);
        if (existing != null) {
            return true; // Already in wishlist
        }

        try {
            Session session = this.sessionFactory.getCurrentSession();
            Wishlist item = new Wishlist(username, productCode);
            session.save(item);
            return true;
        } catch (Exception e) {
            return true; // Handle potential race condition or duplicate key gracefully
        }
    }

    public boolean removeWishlist(String username, String productCode) {
        Wishlist existing = findWishlist(username, productCode);
        if (existing == null) {
            return false;
        }
        Session session = this.sessionFactory.getCurrentSession();
        session.delete(existing);
        return true;
    }

    public List<ProductInfo> getUserWishlistProducts(String username) {
        if (username == null || username.trim().isEmpty()) {
            return new ArrayList<>();
        }
        Session session = this.sessionFactory.getCurrentSession();
        String hql = "Select p from " + Wishlist.class.getName() + " w, " + Product.class.getName() + " p "
                + " Where w.productCode = p.code and w.username = :username Order by w.id desc";
        Query<Product> query = session.createQuery(hql, Product.class);
        query.setParameter("username", username);
        List<Product> products = query.getResultList();
        List<ProductInfo> list = new ArrayList<>();
        if (products != null) {
            for (Product p : products) {
                list.add(new ProductInfo(p));
            }
        }
        return list;
    }

    public int getWishlistCount(String username) {
        if (username == null || username.trim().isEmpty()) {
            return 0;
        }
        Session session = this.sessionFactory.getCurrentSession();
        String hql = "Select count(w) from " + Wishlist.class.getName() + " w Where w.username = :username";
        Query<Long> query = session.createQuery(hql, Long.class);
        query.setParameter("username", username);
        Long count = query.uniqueResult();
        return count != null ? count.intValue() : 0;
    }
}
