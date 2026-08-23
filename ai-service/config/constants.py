"""
System Parameter Constants for Image Quality Gate
Calibrated in Sprint 2 (test/sprint-2-threshold-calibration)
"""

# Ngưỡng sắc nét tối thiểu (Laplacian Variance)
BLUR_THRESHOLD: float = 55.0

# Tỷ lệ diện tích tối thiểu của vật thể chính trên toàn khung hình (8%)
MIN_OBJECT_RATIO: float = 0.08

# Giới hạn tối đa mật độ vật thể phụ / rác hậu cảnh (14%)
MAX_CLUTTER_THRESHOLD: float = 0.14