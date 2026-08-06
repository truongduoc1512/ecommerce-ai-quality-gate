package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dao.WishlistDAO;
import com.example.demo.entity.Wishlist;
import com.example.demo.model.ProductInfo;

@SpringBootTest
@Transactional
public class WishlistTests {

    @Autowired
    private WishlistDAO wishlistDAO;

    @Test
    public void testAddAndRemoveWishlist() {
        String testUser = "test_user_qa";
        String testProductCode = "S001";

        // 1. Initially should not be favorite
        boolean isFavBefore = wishlistDAO.isFavorite(testUser, testProductCode);

        // 2. Add to wishlist
        boolean added = wishlistDAO.addWishlist(testUser, testProductCode);
        assertTrue(added);

        // 3. Verify favorite status
        boolean isFavAfter = wishlistDAO.isFavorite(testUser, testProductCode);
        assertTrue(isFavAfter);

        // 4. Duplicate add should return true (graceful handling)
        boolean addedAgain = wishlistDAO.addWishlist(testUser, testProductCode);
        assertTrue(addedAgain);

        // 5. Query user wishlist items
        List<ProductInfo> wishlistProducts = wishlistDAO.getUserWishlistProducts(testUser);
        assertNotNull(wishlistProducts);

        // 6. Remove from wishlist
        boolean removed = wishlistDAO.removeWishlist(testUser, testProductCode);
        assertTrue(removed);

        // 7. Verify removal
        boolean isFavEnd = wishlistDAO.isFavorite(testUser, testProductCode);
        assertFalse(isFavEnd);
    }
}
