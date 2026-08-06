package com.example.demo.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.UserAddress;
import com.example.demo.form.UserAddressForm;

@Repository
@Transactional
public class UserAddressDAO {

    @Autowired
    private SessionFactory sessionFactory;

    public List<UserAddress> getUserAddresses(String username) {
        if (username == null || username.trim().isEmpty()) {
            return new ArrayList<>();
        }
        Session session = this.sessionFactory.getCurrentSession();
        String hql = "Select a from " + UserAddress.class.getName() + " a Where a.username = :username Order by a.isDefault desc, a.createdAt desc";
        Query<UserAddress> query = session.createQuery(hql, UserAddress.class);
        query.setParameter("username", username);
        return query.getResultList();
    }

    public UserAddress getAddressById(Long id) {
        if (id == null) return null;
        Session session = this.sessionFactory.getCurrentSession();
        return session.find(UserAddress.class, id);
    }

    public void unsetPreviousDefault(String username) {
        Session session = this.sessionFactory.getCurrentSession();
        String hql = "Update " + UserAddress.class.getName() + " a Set a.isDefault = false Where a.username = :username";
        Query query = session.createQuery(hql);
        query.setParameter("username", username);
        query.executeUpdate();
    }

    public UserAddress saveAddress(String username, UserAddressForm form) {
        if (username == null || form == null) return null;
        Session session = this.sessionFactory.getCurrentSession();

        List<UserAddress> existingList = getUserAddresses(username);
        boolean isFirstAddress = existingList.isEmpty();
        boolean shouldBeDefault = form.isDefault() || isFirstAddress;

        if (shouldBeDefault) {
            unsetPreviousDefault(username);
        }

        UserAddress address = null;
        if (form.getId() != null) {
            address = getAddressById(form.getId());
            // Verify ownership
            if (address != null && !address.getUsername().equals(username)) {
                return null;
            }
        }

        if (address == null) {
            address = new UserAddress();
            address.setUsername(username);
            address.setCreatedAt(new Date());
        }

        address.setReceiverName(form.getReceiverName());
        address.setPhone(form.getPhone());
        address.setProvince(form.getProvince());
        address.setDistrict(form.getDistrict());
        address.setWard(form.getWard());
        address.setStreetAddress(form.getStreetAddress());
        address.setNote(form.getNote());
        address.setDefault(shouldBeDefault);
        address.setUpdatedAt(new Date());

        session.saveOrUpdate(address);
        return address;
    }

    public boolean deleteAddress(String username, Long id) {
        UserAddress address = getAddressById(id);
        if (address == null || !address.getUsername().equals(username)) {
            return false;
        }
        Session session = this.sessionFactory.getCurrentSession();
        boolean wasDefault = address.isDefault();
        session.delete(address);
        session.flush();

        // If deleted address was default, set the next available address as default
        if (wasDefault) {
            List<UserAddress> remaining = getUserAddresses(username);
            if (!remaining.isEmpty()) {
                UserAddress nextDefault = remaining.get(0);
                nextDefault.setDefault(true);
                session.update(nextDefault);
            }
        }
        return true;
    }

    public boolean setDefaultAddress(String username, Long id) {
        UserAddress address = getAddressById(id);
        if (address == null || !address.getUsername().equals(username)) {
            return false;
        }
        unsetPreviousDefault(username);
        Session session = this.sessionFactory.getCurrentSession();
        address.setDefault(true);
        address.setUpdatedAt(new Date());
        session.update(address);
        return true;
    }
}
