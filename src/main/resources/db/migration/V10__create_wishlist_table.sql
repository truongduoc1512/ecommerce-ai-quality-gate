-- V10: Tạo bảng Wishlist lưu danh sách sản phẩm yêu thích của người dùng
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS Wishlist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    product_code VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_product UNIQUE (username, product_code),
    CONSTRAINT fk_wishlist_product FOREIGN KEY (product_code) REFERENCES Products(CODE) ON DELETE CASCADE
);

SET FOREIGN_KEY_CHECKS = 1;
