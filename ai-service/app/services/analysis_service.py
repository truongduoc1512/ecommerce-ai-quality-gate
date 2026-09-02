"""
AI Analysis Aggregator Service
==============================
Aggregates outputs from YOLOv8 object detection, OpenCV blur detection,
and Heuristic Centering/Border Angle Check into a standardized Quality Gate contract.
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
    Decodes raw image bytes and runs 3-stage AI analysis pipeline:
    1. Stage 1: OpenCV Laplacian Variance Blur Score Calculation
    2. Stage 2: YOLOv8 Object Detection and Coverage Ratio Extraction
    3. Stage 3: Heuristic Centering, Border Margin, and Aspect Ratio Angle Check
    """
    nparr = np.frombuffer(image_bytes, np.uint8)
    image_bgr = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

    if image_bgr is None or image_bgr.size == 0:
        return {
            "approved": False,
            "status": "REJECTED",
            "reason": "Kh\u00f4ng th\u1ec3 \u0111\u1ecd c d\u1ef1 li\u1ec7u file \u1ea3nh. File c\u00f3 th\u1ec3 b\u1ecb h\u1ecfng ho\u1eb7c sai \u0111\u1ecbnh d\u1ea1ng m\u00e3 h\u00f3a.",
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
            "reason": f"K\u00edch th\u01b0\u1edbc \u1ea3nh qu\u00e1 nh\u1ecf ({image_size_str}px). Y\u00eau c\u1ea7u t\u1ed1i thi\u1ec3u t\u1edb 100x100px tr\u1edf l\u00ean.",
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

    # Stage 1: OpenCV Blur Detection Engine
    blur_score = calculate_blur_score(image_bgr)
    is_blurry = blur_score < blur_threshold

    # Stage 2 & Stage 3: YOLOv8 Object Detection & Heuristic Angle Check
    yolo_result = detect_objects(image_bgr, conf_threshold=0.25, min_ratio_required=min_object_ratio)
    max_object_ratio = yolo_result.get("max_object_ratio", 0.0)
    detected_classes = yolo_result.get("detected_classes", [])
    num_objects = yolo_result.get("num_objects", 0)
    has_valid_object = yolo_result.get("has_object", False)
    is_bad_angle = yolo_result.get("is_bad_angle", False)
    angle_reasons = yolo_result.get("angle_reasons", [])

    # Formulate 3-Stage Evaluation & Accented Vietnamese Reason
    rejection_reasons = []

    if is_blurry:
        rejection_reasons.append(
            f"\u1ea2nh b\u1ecb m\u1edd nh\u00f2e n\u00e9t (\u0110i\u1ec3m s\u1eafc n\u00e9t: {blur_score:.1f} < ng\u01b0\u1ee1ng t\u1ed1i thi\u1ec3u {blur_threshold:.1f})."
        )

    if not has_valid_object or max_object_ratio < min_object_ratio:
        if num_objects == 0:
            rejection_reasons.append("Kh\u00f4ng ph\u00e1t hi\u1ec7n \u0111\u1ed1i t\u01b0\u1ed9ng s\u1ea3n ph\u1ea9m (gi\u00e0y/d\u00e9p/trang ph\u1ee5c) trong \u1ea3nh.")
        else:
            rejection_reasons.append(
                f"S\u1ea3n ph\u1ea9m qu\u00e1 nh\u1ecf ho\u1eb7c n\u1eb1m qu\u00e1 xa (T\u1ec9 l\u1ec7 di\u1ec7n t\u00edch: {max_object_ratio:.1%} < ng\u01b0\u1ee1ng y\u00eau c\u1ea7u {min_object_ratio:.1%})."
            )

    if is_bad_angle and len(rejection_reasons) == 0:
        angle_str = ", ".join(angle_reasons) if angle_reasons else "Góc chụp sản phẩm bị lệch hoặc bị cắt xén sát mép ảnh."
        rejection_reasons.append(f"Góc chụp không đạt tiêu chuẩn: {angle_str}.")

    is_approved = len(rejection_reasons) == 0
    status_str = "APPROVED" if is_approved else "REJECTED"
    reason_str = "\u1ea2nh \u0111\u1ea1t ti\u00eau chu\u1ea9n ch\u1ea5t lư\u1ee3ng s\u1ea3n ph\u1ea9m." if is_approved else " ".join(rejection_reasons)

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
            "is_bad_angle": bool(is_bad_angle),
            "num_objects": int(num_objects),
            "detected_classes": detected_classes,
            "image_size": image_size_str
        }
    }
