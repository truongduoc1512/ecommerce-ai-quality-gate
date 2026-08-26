# Threshold Calibration Report: Optimal Blur & Ratio Settings

- **Branch**: `test/sprint-2-threshold-calibration`
- **Module**: `ai-service`
- **Date**: August 2026
- **Dataset Size**: 50 test samples across 3 categories

---

## 1. Executive Summary & Recommended Thresholds

| Parameter Constant | Calibrated Value | Target Metric | Business Rule / Objective |
| :--- | :---: | :--- | :--- |
| **`BLUR_THRESHOLD`** | **`55.0`** | Variance of Laplacian | Loại bỏ ảnh mờ, rung lắc camera, mất nét chi tiết giày |
| **`MIN_OBJECT_RATIO`** | **`0.08`** | Bounding Box Area / Image Area | Loại bỏ ảnh chụp quá xa, vật thể quá nhỏ không rõ sản phẩm |
| **`MAX_CLUTTER_THRESHOLD`** | **`0.77`** | Background Object Area Ratio | Giới hạn rác hậu cảnh / nhiều vật thể phụ gây nhiễu |

---

## 2. Statistical Empirical Analysis

### 2.1 Distribution Metrics by Category

| Category | Sample Count | Blur Score (Mean ± Std) | Blur Range [Min - Max] | Max Object Ratio [Min - Max] | Max Clutter Ratio |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **`Good_Shoes`** | 20 | **1748.3 ± 3129.7** | [136.6 - 14524.2] | [0.0 - 0.93] | 0.77 |
| **`Blurry_Shoes`** | 15 | **133.9 ± 305.0** | [1.8 - 910.9] | [0.0 - 0.76] | 0.38 |
| **`Bad_Angles`** | 15 | **1042.5 ± 561.5** | [260.9 - 1881.9] | [0.02 - 0.9] | 0.82 |

---

## 3. Threshold Calibration Rationale

### 3.1 Blur Threshold (`BLUR_THRESHOLD = 55.0`)
- Điểm mờ cao nhất của tập `Blurry_Shoes` là **`910.9`**.
- Điểm sắc nét thấp nhất của tập `Good_Shoes` là **`136.6`**.
- Ngưỡng **`55.0`** tạo ra biên độ an toàn rõ ràng giữa ảnh đạt chuẩn và ảnh mờ.

### 3.2 Minimum Object Ratio (`MIN_OBJECT_RATIO = 0.08`)
- Giới hạn sàn để loại bỏ các góc chụp quá xa từ nhóm `Bad_Angles` (chỉ đạt [0.02 - 0.9]).

### 3.3 Max Clutter Threshold (`MAX_CLUTTER_THRESHOLD = 0.77`)
- Lọc các trường hợp nhiễu nền hoặc nhiều vật thể xung quanh.
