import cv2
import numpy as np
import pytest
from app.services.yolo_service import detect_objects, get_yolo_model


def test_yolo_singleton_pattern():
    model1 = get_yolo_model()
    model2 = get_yolo_model()
    assert model1 is model2


def test_detect_objects_valid_image():
    img = np.zeros((600, 600, 3), dtype=np.uint8)
    cv2.rectangle(img, (100, 100), (500, 500), (255, 255, 255), -1)

    result = detect_objects(img, conf_threshold=0.25)
    assert isinstance(result, dict)
    assert "has_object" in result
    assert "max_object_ratio" in result
    assert "detected_classes" in result
    assert "inference_time_ms" in result
    assert result["inference_time_ms"] < 200.0


def test_detect_objects_invalid_input():
    with pytest.raises(ValueError):
        detect_objects(None)
