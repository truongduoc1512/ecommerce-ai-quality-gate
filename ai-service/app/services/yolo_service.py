"""
YOLOv8 & OpenCV Universal Dual-Engine Object Detection Service
==============================================================
Production-grade object detection pipeline for ShoeShop E-commerce Quality Gate:
1. Primary Stage: Multi-Scale YOLOv8 Object Detection Engine (yolov8n.pt)
2. Fallback Stage: Universal OpenCV Contour & Otsu/Canny Foreground Extraction Engine
3. Stage 3: Asymmetric Edge Crop & Off-Center Bad Angle Detection Engine:
   - Evaluates Asymmetric Edge Crop: One edge touches border (< 1.5%) while opposite edge has large empty gap (> 12%).
   - Evaluates Off-Center: YOLOv8 Bounding Box center vs Canvas center distance (> 30%).
"""

import os
import time
import cv2
import numpy as np
from ultralytics import YOLO

VALID_PRODUCT_CLASSES = {
    'shoes', 'footwear', 'backpack', 'handbag', 'suitcase',
    'tie', 'sports ball', 'bottle', 'umbrella', 'person', 'product_item',
    'chair', 'couch', 'potted plant', 'bed', 'laptop', 'mouse', 'remote', 'keyboard'
}
FILTER_CLASSES = VALID_PRODUCT_CLASSES


class YOLOModelSingleton:
    """
    Singleton Pattern class to lazy-load and cache the YOLOv8 model instance.
    Prevents redundant model re-loading on each API request.
    """
    _instance = None
    _model = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(YOLOModelSingleton, cls).__new__(cls)
        return cls._instance

    def get_model(self) -> YOLO:
        if self._model is None:
            local_model_path = os.path.abspath(
                os.path.join(os.path.dirname(__file__), '..', '..', 'models', 'yolov8n.pt')
            )
            if os.path.exists(local_model_path) and os.path.getsize(local_model_path) > 1000:
                print(f"[YOLO-Service] Loading local model from: {local_model_path}")
                self._model = YOLO(local_model_path)
            else:
                print("[YOLO-Service] Loading default yolov8n.pt model...")
                self._model = YOLO('yolov8n.pt')
            print("[YOLO-Service] YOLOv8 model loaded successfully.")
        return self._model


def get_yolo_model() -> YOLO:
    """Helper function to retrieve the singleton YOLOv8 model instance."""
    return YOLOModelSingleton().get_model()


def normalize_image_for_yolo(image_bgr: np.ndarray, max_dim: int = 600) -> np.ndarray:
    """
    Resizes image if its max dimension exceeds max_dim to ensure fast CPU inference (< 30ms).
    """
    h, w = image_bgr.shape[:2]
    if max(h, w) <= max_dim:
        return image_bgr
    scale = max_dim / float(max(h, w))
    new_w = int(w * scale)
    new_h = int(h * scale)
    return cv2.resize(image_bgr, (new_w, new_h), interpolation=cv2.INTER_AREA)


def detect_foreground_product_universal(image_bgr: np.ndarray, min_ratio_required: float = 0.08) -> dict:
    """
    Universal Fallback Engine: Uses OpenCV Canny Edge + Otsu Thresholding to extract
    product foreground objects for any studio / solid background product photos.
    """
    h, w = image_bgr.shape[:2]
    img_area = float(h * w)

    gray = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2GRAY)
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)

    _, thresh_otsu = cv2.threshold(blurred, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)
    edges = cv2.Canny(blurred, 30, 150)
    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (7, 7))
    closed = cv2.morphologyEx(edges, cv2.MORPH_CLOSE, kernel)
    combined = cv2.bitwise_or(thresh_otsu, closed)

    contours, _ = cv2.findContours(combined, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    if not contours:
        return {"detected": False, "ratio": 0.0, "bbox": [0, 0, w, h]}

    c = max(contours, key=cv2.contourArea)
    x, y, cw, ch = cv2.boundingRect(c)
    box_area = float(cw * ch)
    ratio = box_area / img_area

    if ratio >= min_ratio_required:
        return {
            "detected": True,
            "ratio": round(float(ratio), 4),
            "bbox": [float(x), float(y), float(x + cw), float(y + ch)]
        }

    return {"detected": False, "ratio": round(float(ratio), 4), "bbox": [0, 0, w, h]}


def detect_objects(
    image_bgr: np.ndarray,
    conf_threshold: float = 0.25,
    min_ratio_required: float = 0.08
) -> dict:
    """
    Universal Product Object & Bad Angle Detector pipeline with Asymmetric Crop Rule.
    """
    if image_bgr is None or image_bgr.size == 0:
        raise ValueError("Invalid image input: Image array is None or empty.")

    start_time = time.perf_counter()

    img_norm = normalize_image_for_yolo(image_bgr, max_dim=600)
    img_h, img_w = img_norm.shape[:2]
    img_area = float(img_h * img_w)

    model = get_yolo_model()
    results = model(img_norm, conf=conf_threshold, verbose=False)

    max_object_ratio = 0.0
    detected_classes = []
    objects_list = []
    yolo_bboxes = []
    is_bad_angle = False
    angle_reasons = []

    for result in results:
        boxes = result.boxes
        if boxes is None or len(boxes) == 0:
            continue

        for box in boxes:
            x1, y1, x2, y2 = box.xyxy[0].tolist()
            box_width = max(1.0, x2 - x1)
            box_height = max(1.0, y2 - y1)
            box_area = box_width * box_height
            ratio = box_area / img_area

            confidence = float(box.conf[0])
            cls_id = int(box.cls[0])
            cls_name = model.names.get(cls_id, f"class_{cls_id}")

            if ratio <= 0.99 and (cls_name in FILTER_CLASSES or confidence >= 0.20):
                if ratio > max_object_ratio:
                    max_object_ratio = ratio

                if cls_name not in detected_classes:
                    detected_classes.append(cls_name)

                yolo_bboxes.append([x1, y1, x2, y2])
                objects_list.append({
                    "class_name": cls_name,
                    "confidence": round(confidence, 4),
                    "coverage_ratio": round(float(ratio), 4),
                    "bbox": [round(x1, 1), round(y1, 1), round(x2, 1), round(y2, 1)]
                })

    # Stage 3: Asymmetric Edge Crop & Off-Center Bad Angle Detection on YOLO Bounding Boxes
    if len(yolo_bboxes) > 0:
        gx1 = min(b[0] for b in yolo_bboxes)
        gy1 = min(b[1] for b in yolo_bboxes)
        gx2 = max(b[2] for b in yolo_bboxes)
        gy2 = max(b[3] for b in yolo_bboxes)

        left_margin = gx1 / float(img_w)
        right_margin = (img_w - gx2) / float(img_w)
        top_margin = gy1 / float(img_h)
        bottom_margin = (img_h - gy2) / float(img_h)

        center_x = (gx1 + gx2) / 2.0
        center_y = (gy1 + gy2) / 2.0
        off_center_x = abs(center_x - img_w / 2.0) / (img_w / 2.0)
        off_center_y = abs(center_y - img_h / 2.0) / (img_h / 2.0)

        # Asymmetric Edge Crop: One edge touches border (< 1.5%) while opposite edge has large empty gap (> 12%)
        is_asymmetric_x = (left_margin < 0.015 and right_margin > 0.12) or (right_margin < 0.015 and left_margin > 0.12)
        is_asymmetric_y = (top_margin < 0.015 and bottom_margin > 0.12) or (bottom_margin < 0.015 and top_margin > 0.12)

        if is_asymmetric_x or is_asymmetric_y:
            is_bad_angle = True
            if "Sản phẩm bị cắt xén lệch góc" not in angle_reasons:
                angle_reasons.append("Sản phẩm bị cắt xén lệch góc")

        if off_center_x > 0.32 or off_center_y > 0.32:
            is_bad_angle = True
            if "Góc chụp lệch khỏi tâm ảnh" not in angle_reasons:
                angle_reasons.append("Góc chụp lệch khỏi tâm ảnh")

    # Studio Product Fallback Stage: If YOLOv8 finds 0 objects, run OpenCV Universal Detector
    if len(objects_list) == 0:
        cv_fg = detect_foreground_product_universal(img_norm, min_ratio_required=min_ratio_required)
        if cv_fg["detected"]:
            max_object_ratio = cv_fg["ratio"]
            detected_classes = ["product_item"]
            x1, y1, x2, y2 = cv_fg["bbox"]
            objects_list.append({
                "class_name": "product_item",
                "confidence": 0.95,
                "coverage_ratio": round(float(max_object_ratio), 4),
                "bbox": [round(x1, 1), round(y1, 1), round(x2, 1), round(y2, 1)]
            })

    inference_time_ms = (time.perf_counter() - start_time) * 1000.0
    has_valid_product = (max_object_ratio >= min_ratio_required) or (len(objects_list) > 0)

    return {
        "has_object": bool(has_valid_product),
        "max_object_ratio": round(float(max_object_ratio), 4),
        "detected_classes": detected_classes,
        "num_objects": len(objects_list),
        "objects": objects_list,
        "is_bad_angle": is_bad_angle,
        "angle_reasons": angle_reasons,
        "inference_time_ms": round(float(inference_time_ms), 2)
    }
