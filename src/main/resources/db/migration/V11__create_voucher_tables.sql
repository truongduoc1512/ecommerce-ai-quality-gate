-- V11: Tạo các bảng quản lý Mã giảm giá (Vouchers) và Lịch sử sử dụng (Voucher_Usages)
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS Vouchers (
    code VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY,
    discount_type VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PERCENT',
    discount_value DOUBLE NOT NULL DEFAULT 0,
    max_discount DOUBLE DEFAULT NULL,
    min_order_value DOUBLE NOT NULL DEFAULT 0,
    expiry_date DATETIME DEFAULT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    usage_limit INT NOT NULL DEFAULT 100,
    used_count INT NOT NULL DEFAULT 0,
    per_user_limit INT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS Voucher_Usages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    voucher_code VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    username VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    order_id VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    used_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usage_voucher FOREIGN KEY (voucher_code) REFERENCES Vouchers(code) ON DELETE CASCADE
);

-- Seed voucher mẫu
INSERT INTO Vouchers (code, discount_type, discount_value, max_discount, min_order_value, expiry_date, active, usage_limit, used_count, per_user_limit)
VALUES 
('WELCOME50', 'PERCENT', 20, 100, 200, DATE_ADD(NOW(), INTERVAL 30 DAY), 1, 500, 0, 2),
('GIAM50K', 'FIXED', 50, NULL, 300, DATE_ADD(NOW(), INTERVAL 30 DAY), 1, 200, 0, 1),
('SHOESHOP100', 'FIXED', 100, NULL, 500, DATE_ADD(NOW(), INTERVAL 60 DAY), 1, 100, 0, 1)
ON DUPLICATE KEY UPDATE active=1;

SET FOREIGN_KEY_CHECKS = 1;
