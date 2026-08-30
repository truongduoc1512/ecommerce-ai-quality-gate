import os
import cv2
import numpy as np

os.makedirs("tests/samples", exist_ok=True)

# 1. Scenario 1: Sharp, Centered Product (Valid)
img_valid = np.full((600, 600, 3), 245, dtype=np.uint8)
cv2.ellipse(img_valid, (300, 320), (180, 70), 0, 0, 360, (40, 40, 40), -1)
cv2.rectangle(img_valid, (220, 260), (380, 330), (30, 30, 200), -1)
for i in range(10):
    cv2.line(img_valid, (240 + i*14, 260), (250 + i*14, 330), (255, 255, 255), 2)
cv2.imwrite("tests/samples/scenario_1_valid.jpg", img_valid)

# 2. Scenario 2: Blurry Image (Out-of-focus)
img_blurry = cv2.GaussianBlur(img_valid, (51, 51), 0)
cv2.imwrite("tests/samples/scenario_2_blurry.jpg", img_blurry)

# 3. Scenario 3: Small / Far Object (High-angle, ratio < 0.08)
img_small = np.full((800, 800, 3), 240, dtype=np.uint8)
cv2.ellipse(img_small, (700, 100), (30, 15), 0, 0, 360, (40, 40, 40), -1)
cv2.imwrite("tests/samples/scenario_3_small.jpg", img_small)

# 4. Scenario 4: Invalid Class (Coffee cup / non-footwear)
img_invalid = np.full((600, 600, 3), 250, dtype=np.uint8)
cv2.circle(img_invalid, (300, 300), 100, (180, 105, 255), -1)
cv2.rectangle(img_invalid, (380, 270), (420, 330), (180, 105, 255), 10)
cv2.imwrite("tests/samples/scenario_4_invalid.jpg", img_invalid)

print(" Đã khởi tạo thành công 4 file ảnh mẫu kiểm thử trong tests/samples/")