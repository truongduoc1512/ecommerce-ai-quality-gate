-- V13: Tạo bảng Yêu Cầu Trả Hàng / Hoàn Tiền (Order_Returns)
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS Order_Returns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    username VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    reason TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    image_urls VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    status VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
    admin_note VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_returns_order (order_id),
    INDEX idx_order_returns_user (username)
);

SET FOREIGN_KEY_CHECKS = 1;
