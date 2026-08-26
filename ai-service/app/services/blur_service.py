import cv2
import numpy as np


def calculate_blur_score(image_bgr: np.ndarray, max_dim: int = 500) -> float:
    """Calculates the blur score of an image using OpenCV's Laplacian operator.

    Args:
        image_bgr (np.ndarray): Input image in BGR format.
        max_dim (int): Maximum dimension to resize image for resolution independence.

    Returns:
        float: Variance of the Laplacian (higher value = sharper image).
    """
    if image_bgr is None or image_bgr.size == 0:
        raise ValueError("Invalid image input")

    height, width = image_bgr.shape[:2]

    # Multi-Scale Image Normalization (Resize nếu kích thước vượt max_dim)
    if max(height, width) > max_dim:
        scale = max_dim / float(max(height, width))
        new_width = int(width * scale)
        new_height = int(height * scale)
        image_bgr = cv2.resize(
            image_bgr, (new_width, new_height), interpolation=cv2.INTER_AREA
        )

    # Grayscale Conversion
    gray = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2GRAY)

    # Variance Computation via 2D Laplacian operator
    blur_score = cv2.Laplacian(gray, cv2.CV_64F).var()

    return float(blur_score)