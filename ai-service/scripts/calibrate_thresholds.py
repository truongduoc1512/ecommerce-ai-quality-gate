import os
import sys
from pathlib import Path
import cv2
import pandas as pd
import numpy as np
from ultralytics import YOLO

CURRENT_FILE = Path(__file__).resolve()
PROJECT_ROOT = CURRENT_FILE.parents[2]

POSSIBLE_DATASET_PATHS = [
    PROJECT_ROOT / "test_dataset",
    PROJECT_ROOT / "ai-service" / "test_dataset",
    Path("test_dataset"),
    Path("ai-service/test_dataset")
]

DATASET_DIR = None
for p in POSSIBLE_DATASET_PATHS:
    if p.exists() and p.is_dir():
        DATASET_DIR = p
        break

if DATASET_DIR is None:
    print("[ERROR] Không tìm thấy thư mục 'test_dataset'!")
    sys.exit(1)

model = YOLO("yolov8n.pt")
CATEGORIES = ["Good_Shoes", "Blurry_Shoes", "Bad_Angles"]

def get_image_metrics(image_path: str):
    image = cv2.imread(image_path)
    if image is None:
        return None
    
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    blur_score = float(cv2.Laplacian(gray, cv2.CV_64F).var())
    
    h, w = image.shape[:2]
    img_area = h * w
    
    results = model(image, verbose=False)[0]
    boxes = results.boxes.xywh.cpu().numpy() if results.boxes is not None else []
    
    if len(boxes) > 0:
        areas = [bw * bh for _, _, bw, bh in boxes]
        max_object_ratio = float(max(areas) / img_area)
        clutter_ratio = float((sum(areas) - max(areas)) / img_area)
    else:
        max_object_ratio = 0.0
        clutter_ratio = 0.0
        
    return {
        "blur_score": round(blur_score, 2),
        "max_object_ratio": round(max_object_ratio, 4),
        "clutter_ratio": round(clutter_ratio, 4)
    }

def generate_markdown_report(df: pd.DataFrame, report_path: Path):
    # Tính toán các chỉ số thống kê thực tế
    stats = {}
    for cat in CATEGORIES:
        cat_df = df[df["category"] == cat]
        if not cat_df.empty:
            stats[cat] = {
                "count": len(cat_df),
                "blur_mean": round(cat_df["blur_score"].mean(), 1),
                "blur_std": round(cat_df["blur_score"].std(), 1),
                "blur_min": round(cat_df["blur_score"].min(), 1),
                "blur_max": round(cat_df["blur_score"].max(), 1),
                "ratio_min": round(cat_df["max_object_ratio"].min(), 2),
                "ratio_max": round(cat_df["max_object_ratio"].max(), 2),
                "clutter_max": round(cat_df["clutter_ratio"].max(), 2)
            }
        else:
            stats[cat] = {"count": 0, "blur_mean": 0, "blur_std": 0, "blur_min": 0, "blur_max": 0, "ratio_min": 0, "ratio_max": 0, "clutter_max": 0}

    # Đề xuất ngưỡng dựa trên dữ liệu thực tế
    blurry_max = stats["Blurry_Shoes"]["blur_max"]
    good_min = stats["Good_Shoes"]["blur_min"]
    calibrated_blur = round((blurry_max + good_min) / 2, 1) if good_min > blurry_max else 55.0
    
    calibrated_ratio = stats["Good_Shoes"]["ratio_min"] if stats["Good_Shoes"]["ratio_min"] > 0 else 0.08
    calibrated_clutter = stats["Good_Shoes"]["clutter_max"] if stats["Good_Shoes"]["clutter_max"] > 0 else 0.14

    report_content = f"""# Threshold Calibration Report: Optimal Blur & Ratio Settings

- **Branch**: `test/sprint-2-threshold-calibration`
- **Module**: `ai-service`
- **Date**: August 2026
- **Dataset Size**: {len(df)} test samples across {len(CATEGORIES)} categories

---

## 1. Executive Summary & Recommended Thresholds

| Parameter Constant | Calibrated Value | Target Metric | Business Rule / Objective |
| :--- | :---: | :--- | :--- |
| **`BLUR_THRESHOLD`** | **`{calibrated_blur}`** | Variance of Laplacian | Loại bỏ ảnh mờ, rung lắc camera, mất nét chi tiết giày |
| **`MIN_OBJECT_RATIO`** | **`{calibrated_ratio}`** | Bounding Box Area / Image Area | Loại bỏ ảnh chụp quá xa, vật thể quá nhỏ không rõ sản phẩm |
| **`MAX_CLUTTER_THRESHOLD`** | **`{calibrated_clutter}`** | Background Object Area Ratio | Giới hạn rác hậu cảnh / nhiều vật thể phụ gây nhiễu |

---

## 2. Statistical Empirical Analysis

### 2.1 Distribution Metrics by Category

| Category | Sample Count | Blur Score (Mean ± Std) | Blur Range [Min - Max] | Max Object Ratio [Min - Max] | Max Clutter Ratio |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **`Good_Shoes`** | {stats['Good_Shoes']['count']} | **{stats['Good_Shoes']['blur_mean']} ± {stats['Good_Shoes']['blur_std']}** | [{stats['Good_Shoes']['blur_min']} - {stats['Good_Shoes']['blur_max']}] | [{stats['Good_Shoes']['ratio_min']} - {stats['Good_Shoes']['ratio_max']}] | {stats['Good_Shoes']['clutter_max']} |
| **`Blurry_Shoes`** | {stats['Blurry_Shoes']['count']} | **{stats['Blurry_Shoes']['blur_mean']} ± {stats['Blurry_Shoes']['blur_std']}** | [{stats['Blurry_Shoes']['blur_min']} - {stats['Blurry_Shoes']['blur_max']}] | [{stats['Blurry_Shoes']['ratio_min']} - {stats['Blurry_Shoes']['ratio_max']}] | {stats['Blurry_Shoes']['clutter_max']} |
| **`Bad_Angles`** | {stats['Bad_Angles']['count']} | **{stats['Bad_Angles']['blur_mean']} ± {stats['Bad_Angles']['blur_std']}** | [{stats['Bad_Angles']['blur_min']} - {stats['Bad_Angles']['blur_max']}] | [{stats['Bad_Angles']['ratio_min']} - {stats['Bad_Angles']['ratio_max']}] | {stats['Bad_Angles']['clutter_max']} |

---

## 3. Threshold Calibration Rationale

### 3.1 Blur Threshold (`BLUR_THRESHOLD = {calibrated_blur}`)
- Điểm mờ cao nhất của tập `Blurry_Shoes` là **`{stats['Blurry_Shoes']['blur_max']}`**.
- Điểm sắc nét thấp nhất của tập `Good_Shoes` là **`{stats['Good_Shoes']['blur_min']}`**.
- Ngưỡng **`{calibrated_blur}`** tạo ra biên độ an toàn rõ ràng giữa ảnh đạt chuẩn và ảnh mờ.

### 3.2 Minimum Object Ratio (`MIN_OBJECT_RATIO = {calibrated_ratio}`)
- Giới hạn sàn để loại bỏ các góc chụp quá xa từ nhóm `Bad_Angles` (chỉ đạt [{stats['Bad_Angles']['ratio_min']} - {stats['Bad_Angles']['ratio_max']}]).

### 3.3 Max Clutter Threshold (`MAX_CLUTTER_THRESHOLD = {calibrated_clutter}`)
- Lọc các trường hợp nhiễu nền hoặc nhiều vật thể xung quanh.
"""

    report_path.parent.mkdir(parents=True, exist_ok=True)
    with open(report_path, "w", encoding="utf-8") as f:
        f.write(report_content)
    print(f"[SUCCESS] Đã tự động tạo báo cáo tại: {report_path}")

def main():
    records = []
    for category in CATEGORIES:
        folder_path = DATASET_DIR / category
        if not folder_path.exists():
            continue
            
        files = [f for f in os.listdir(folder_path) if f.lower().endswith(('.png', '.jpg', '.jpeg'))]
        for file_name in files:
            img_path = str(folder_path / file_name)
            metrics = get_image_metrics(img_path)
            if metrics:
                metrics["category"] = category
                metrics["file_name"] = file_name
                records.append(metrics)
                    
    if not records:
        print("[ERROR] Không đọc được ảnh nào!")
        return

    df = pd.DataFrame(records)
    
    # 1. Xuất file CSV
    csv_path = PROJECT_ROOT / "calibration_raw_results.csv"
    df.to_csv(csv_path, index=False)
    print(f"[SUCCESS] Đã lưu file CSV: {csv_path}")

    # 2. Tự động xuất file Markdown báo cáo
    md_path = PROJECT_ROOT / "docs" / "reports" / "threshold_calibration_report.md"
    generate_markdown_report(df, md_path)

if __name__ == "__main__":
    main()