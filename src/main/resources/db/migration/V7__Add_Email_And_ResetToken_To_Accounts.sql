-- V7: Bổ sung email, reset_token và provider cho bảng Accounts
ALTER TABLE Accounts 
ADD COLUMN EMAIL VARCHAR(128) NULL,
ADD COLUMN RESET_TOKEN VARCHAR(128) NULL,
ADD COLUMN PROVIDER VARCHAR(20) DEFAULT 'LOCAL';

-- Cập nhật email mặc định cho các tài khoản hiện có
UPDATE Accounts SET EMAIL = 'manager1@shoeshop.com' WHERE USER_NAME = 'manager1';
UPDATE Accounts SET EMAIL = 'employee1@shoeshop.com' WHERE USER_NAME = 'employee1';
