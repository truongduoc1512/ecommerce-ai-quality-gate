package com.example.demo.dao;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Account;
import com.example.demo.pagination.PaginationResult;

@Transactional
@Repository
public class AccountDAO {
 
    @Autowired
    private SessionFactory sessionFactory;
 
    public Account findAccount(String userName) {
        Session session = this.sessionFactory.getCurrentSession();
        return session.find(Account.class, userName);
    }

    public Account findAccountByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        Session session = this.sessionFactory.getCurrentSession();
        String sql = "Select e from " + Account.class.getName() + " e Where e.email = :email";
        Query<Account> query = session.createQuery(sql, Account.class);
        query.setParameter("email", email);
        return query.uniqueResult();
    }

    public Account findAccountByResetToken(String resetToken) {
        if (resetToken == null || resetToken.trim().isEmpty()) {
            return null;
        }
        Session session = this.sessionFactory.getCurrentSession();
        String sql = "Select e from " + Account.class.getName() + " e Where e.resetToken = :token";
        Query<Account> query = session.createQuery(sql, Account.class);
        query.setParameter("token", resetToken);
        return query.uniqueResult();
    }

    public void saveAccount(Account account) {
        Session session = this.sessionFactory.getCurrentSession();
        account.setUpdatedAt(new Date());
        session.saveOrUpdate(account);
    }

    public PaginationResult<Account> listAccounts(int page, int maxResult, int maxNavigationPage) {
        Session session = this.sessionFactory.getCurrentSession();
        String sql = "Select a from " + Account.class.getName() + " a Order by a.createdAt desc";
        Query<Account> query = session.createQuery(sql, Account.class);
        return new PaginationResult<Account>(query, page, maxResult, maxNavigationPage);
    }
}
