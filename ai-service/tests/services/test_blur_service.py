import cv2
import numpy as np
import pytest
from app.services.blur_service import calculate_blur_score


def test_calculate_blur_score_sharp_vs_blurry():
    # Tạo ảnh rõ nét (chứa nhiều đường nét/cạnh)
    sharp_img = np.zeros((600, 600, 3), dtype=np.uint8)
    cv2.rectangle(sharp_img, (100, 100), (500, 500), (255, 255, 255), -1)
    cv2.circle(sharp_img, (300, 300), 150, (0, 0, 0), -1)

    # Tạo ảnh mờ bằng Gaussian Blur
    blurry_img = cv2.GaussianBlur(sharp_img, (21, 21), 0)

    score_sharp = calculate_blur_score(sharp_img)
    score_blurry = calculate_blur_score(blurry_img)

    # Điểm ảnh nét phải lớn hơn điểm ảnh mờ
    assert score_sharp > score_blurry


def test_calculate_blur_score_invalid_input():
    with pytest.raises(ValueError):
        calculate_blur_score(None)