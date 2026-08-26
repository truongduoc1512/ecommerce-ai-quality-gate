"""
AI Analysis Aggregator Service
==============================
Aggregates outputs from YOLOv8 object detection and OpenCV blur detection engines
into a standardized E-commerce Quality Gate response payload.
"""

import cv2
import numpy as np
from app.services.blur_service import calculate_blur_score
from app.services.yolo_service import detect_objects

DEFAULT_BLUR_THRESHOLD = 50.0
DEFAULT_MIN_OBJECT_RATIO = 0.08
DEFAULT_MAX_CLUTTER_THRESHOLD = 0.14


def analyze_product_image_data(
    image_bytes: bytes,
    filename: str = "unknown.jpg",
    blur_threshold: float = DEFAULT_BLUR_THRESHOLD,
    min_object_ratio: float = DEFAULT_MIN_OBJECT_RATIO,
    max_clutter_threshold: float = DEFAULT_MAX_CLUTTER_THRESHOLD
) -> dict:
    """
    Decodes raw image bytes and runs multi-stage AI analysis pipeline:
    1. OpenCV Laplacian Variance Blur Score Calculation
    2. YOLOv8 Object Detection and Coverage Ratio Extraction
    3. Rule-based Evaluation to construct standardized Response Payload Contract.
    """
    nparr = np.frombuffer(image_bytes, np.uint8)
    image_bgr = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

    if image_bgr is None or image_bgr.size == 0:
        return {
            "approved": False,
            "status": "REJECTED",
            "reason": "Khong the doc du lieu file anh. File co the bi hong hoac sai dinh dang ma hoa.",
            "filename": filename,
            "metrics": {
                "blur_score": 0.0,
                "blur_threshold": blur_threshold,
                "is_blurry": True,
                "max_object_ratio": 0.0,
                "min_object_ratio_required": min_object_ratio,
                "is_cluttered": False,
                "num_objects": 0,
                "detected_classes": [],
                "image_size": "0x0"
            }
        }

    img_h, img_w = image_bgr.shape[:2]
    image_size_str = f"{img_w}x{img_h}"

    if img_w < 100 or img_h < 100:
        return {
            "approved": False,
            "status": "REJECTED",
            "reason": f"Kich thuoc anh qua nho ({image_size_str}px). Yeu cau toi thieu tu 100x100px tro len.",
            "filename": filename,
            "metrics": {
                "blur_score": 0.0,
                "blur_threshold": blur_threshold,
                "is_blurry": True,
                "max_object_ratio": 0.0,
                "min_object_ratio_required": min_object_ratio,
                "is_cluttered": False,
                "num_objects": 0,
                "detected_classes": [],
                "image_size": image_size_str
            }
        }

    # 1. OpenCV Blur Detection Engine
    blur_score = calculate_blur_score(image_bgr)
    is_blurry = blur_score < blur_threshold

    # 2. YOLOv8 Object Detection Engine
    yolo_result = detect_objects(image_bgr, conf_threshold=0.25, min_ratio_required=min_object_ratio)
    max_object_ratio = yolo_result.get("max_object_ratio", 0.0)
    detected_classes = yolo_result.get("detected_classes", [])
    num_objects = yolo_result.get("num_objects", 0)
    has_valid_object = yolo_result.get("has_object", False)

    # 3. Formulate Status Evaluation & Localized Reason (Unaccented Vietnamese)
    rejection_reasons = []

    if is_blurry:
        rejection_reasons.append(
            f"Anh bi mo nhoe net (Diem sac net: {blur_score:.1f} < nguong toi thieu {blur_threshold:.1f})."
        )

    if not has_valid_object or max_object_ratio < min_object_ratio:
        if num_objects == 0:
            rejection_reasons.append("Khong phat hien doi tuong san pham (giay/dep/trang phuc) trong anh.")
        else:
            rejection_reasons.append(
                f"San pham qua nho hoac nam qua xa (Ti le dien tich: {max_object_ratio:.1%} < nguong yeu cau {min_object_ratio:.1%})."
            )

    is_approved = len(rejection_reasons) == 0
    status_str = "APPROVED" if is_approved else "REJECTED"
    reason_str = "Anh dat tieu chuan chat luong san pham." if is_approved else " ".join(rejection_reasons)

    return {
        "approved": is_approved,
        "status": status_str,
        "reason": reason_str,
        "filename": filename,
        "metrics": {
            "blur_score": round(float(blur_score), 2),
            "blur_threshold": float(blur_threshold),
            "is_blurry": bool(is_blurry),
            "max_object_ratio": round(float(max_object_ratio), 4),
            "min_object_ratio_required": float(min_object_ratio),
            "is_cluttered": False,
            "num_objects": int(num_objects),
            "detected_classes": detected_classes,
            "image_size": image_size_str
        }
    }
