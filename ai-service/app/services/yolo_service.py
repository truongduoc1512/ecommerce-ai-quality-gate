"""
YOLOv8 Object Detection Engine Service
======================================
@roduction-grade object detection pipeline for ShoeShop E-commerce Quality Gate:
- Singleton Pattern model loading for ultralytics yolov8n.pt
- Multi-scale image normalization for ultra-fast CPU inference (< 30ms)
- Bounding Box tensor extraction (x1, y1, x2, y2) and area coverage ratio calculation
- E-commerce product class filtering with confidence thresholding (conf >= 0.25)
"""

import os
import time
import cv2
import numpy as np
from ultralytics import YOLO

VALID_PRODUCT_CLASSES = {
    'shoes', 'footwear', 'backpack', 'handbag', 'suitcase',
    'tie', 'sports ball', 'bottle', 'umbrella', 'person', 'product_item'
}

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
    h, p = image_bgr.shape[:2]
    if max(h, p) <= max_dim:
        return image_bgr
    scale = max_dim / float(max(h, p))
    new_w = int(p * scale)
    new_h = int(h * scale)
    return cv2.resize(image_bgr, (new_w, new_h), interpolation=cv2.INTER_AREA)


def detect_objects(
    image_bgr: np.ndarray,
    conf_threshold: float = 0.25,
    min_ratio_required: float = 0.08
) -> dict:
    """
    Detects product objects in BGR image using YOLOv8 model.
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

    for result in results:
        boxes = result.boxes
        if boxes is None or len(boxes) == 0:
            continue

        for box in boxes:
            x1, y1, x2, y2 = box.xyxy[0].tolist()
            box_width = max(0.0, x2 - x1)
            box_height = max(0.0, y2 - y1)
            box_area = box_width * box_height
            ratio = box_area / img_area

            confidence = float(box.conf[0])
            cls_id = int(box.cls[0])
            cls_name = model.names.get(cls_id, f"class_{cls_id}")

            if ratio <= 0.85 and (cls_name in FILTER_CLASSES or confidence >= 0.40):
                if ratio > max_object_ratio:
                    max_object_ratio = ratio

                if cls_name not in detected_classes:
                    detected_classes.append(cls_name)

                objects_list.append({
                    "class_name": cls_name,
                    "confidence": round(confidence, 4),
                    "coverage_ratio": round(float(ratio), 4),
                    "bbox": [round(x1, 1), round(y1, 1), round(x2, 1), round(y2, 1)]
                })

    inference_time_ms = (time.perf_counter() - start_time) * 1000.0
    has_valid_product = (max_object_ratio >= min_ratio_required) and (len(detected_classes) > 0)

    return {
        "has_object": bool(has_valid_product),
        "max_object_ratio": round(float(max_object_ratio), 4),
        "detected_classes": detected_classes,
        "num_objects": len(objects_list),
        "objects": objects_list,
        "inference_time_ms": round(float(inference_time_ms), 2)
    }
