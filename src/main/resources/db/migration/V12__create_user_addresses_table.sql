-- V12: Tạo bảng Quản lý Sổ Địa Chỉ Giao Hàng (User_Addresses)
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS User_Addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    receiver_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    phone VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    province VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    district VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    ward VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    street_address VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    note VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_address_user (username)
);

-- Seed địa chỉ mẫu
INSERT INTO User_Addresses (username, receiver_name, phone, province, district, ward, street_address, note, is_default)
VALUES 
('user', 'Nguyễn Văn A', '0988123456', 'Hồ Chí Minh', 'Quận 1', 'Phường Bến Nghé', '123 Lê Lợi', 'Giao giờ hành chính', 1),
('user', 'Nguyễn Văn A (Nhà riêng)', '0988123456', 'Hồ Chí Minh', 'Quận 7', 'Phường Tân Phong', '456 Nguyễn Hữu Thọ', 'Gọi trước khi giao', 0);

SET FOREIGN_KEY_CHECKS = 1;
