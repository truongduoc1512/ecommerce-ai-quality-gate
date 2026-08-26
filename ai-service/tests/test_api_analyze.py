import io
import cv2
import numpy as np
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


def create_dummy_image_bytes(width=600, height=600):
    img = np.zeros((height, width, 3), dtype=np.uint8)
    cv2.rectangle(img, (100, 100), (500, 500), (255, 255, 255), -1)
    _, buf = cv2.imencode('.jpg', img)
    return buf.tobytes()


def test_root_endpoint():
    response = client.get("/")
    assert response.status_code == 200
    assert response.json()["status"] == "HEALTHY"


def test_health_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "UP"


def test_analyze_valid_image():
    img_bytes = create_dummy_image_bytes()
    files = {"file": ("test_shoe.jpg", img_bytes, "image/jpeg")}
    response = client.post("/api/v1/analyze", files=files)
    assert response.status_code == 200

    data = response.json()
    assert "approved" in data
    assert "status" in data
    assert "reason" in data
    assert "filename" in data
    assert data["filename"] == "test_shoe.jpg"
    assert "metrics" in data

    metrics = data["metrics"]
    assert "blur_score" in metrics
    assert "blur_threshold" in metrics
    assert "is_blurry" in metrics
    assert "max_object_ratio" in metrics
    assert "min_object_ratio_required" in metrics
    assert "is_cluttered" in metrics
    assert "num_objects" in metrics
    assert "detected_classes" in metrics
    assert "image_size" in metrics


def test_analyze_unsupported_file_extension():
    files = {"file": ("document.pdf", b"Dummy PDF content", "application/pdf")}
    response = client.post("/api/v1/analyze", files=files)
    assert response.status_code == 400

    data = response.json()
    assert data["approved"] is False
    assert data["status"] == "REJECTED"
    assert ".pdf" in data["reason"]


def test_analyze_empty_file():
    files = {"file": ("empty_image.jpg", b"", "image/jpeg")}
    response = client.post("/api/v1/analyze", files=files)
    assert response.status_code == 400

    data = response.json()
    assert data["approved"] is False
    assert data["status"] == "REJECTED"
    assert "0 bytes" in data["reason"] or "rong" in data["reason"]


if __name__ == "__main__":
    test_root_endpoint()
    test_health_endpoint()
    test_analyze_unsupported_file_extension()
    test_analyze_empty_file()
    test_analyze_valid_image()
    print("ALL API UNIT TESTS PASSED SUCCESSFULLY!")
