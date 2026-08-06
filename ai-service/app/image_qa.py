"""
AI Quality Gate — Production-Grade Product Image Verification Engine
====================================================================
Chuẩn hóa kiểm duyệt ảnh sản phẩm E-commerce (Shopee/Lazada/Amazon standard):
  1. Multi-scale Blur Detection: Laplacian Variance trên ảnh đã chuẩn hóa độ phân giải
  2. Center-Weighted Salient Product Detection: Tách sản phẩm trung tâm qua LAB Color Saliency & Contour
  3. Mask-Gated Background Clutter: Phân tích độ nhiễu phông nền ĐỘC LẬP bên ngoài vùng sản phẩm
"""

import cv2
import numpy as np
from ultralytics import YOLO
import os

# ─── System Thresholds ────────────────────────────────────────────────────────
BLUR_THRESHOLD = 50.0          # Điểm sắc nét tối thiểu (sau khi chuẩn hóa độ phân giải về 500px)
MIN_OBJECT_RATIO = 0.08        # Sản phẩm phải chiếm ít nhất 8% diện tích ảnh
MAX_CLUTTER_THRESHOLD = 0.14   # Mật độ nhiễu phông nền bên ngoài sản phẩm tối đa (14%)

_model = None

def get_model():
    """Lazy-load YOLOv8n model."""
    global _model
    if _model is None:
        local_path = os.path.join(os.path.dirname(__file__), '..', 'models', 'yolov8n.pt')
        if os.path.exists(local_path) and os.path.getsize(local_path) > 1000:
            print(f'[AI-QA] Loading model from: {local_path}')
            _model = YOLO(local_path)
        else:
            print('[AI-QA] Local model missing or 0 bytes, downloading/using default yolov8n.pt...')
            _model = YOLO('yolov8n.pt')
        print('[AI-QA] Model loaded successfully.')
    return _model


def normalize_image(image_bgr, max_dim=600):
    """Chuẩn hóa ảnh về độ phân giải chuẩn để tính toán nhất quán độc lập với camera."""
    h, w = image_bgr.shape[:2]
    if max(h, w) <= max_dim:
        return image_bgr
    scale = max_dim / float(max(h, w))
    new_w = int(w * scale)
    new_h = int(h * scale)
    return cv2.resize(image_bgr, (new_w, new_h), interpolation=cv2.INTER_AREA)


def calculate_blur_score(image_bgr):
    """
    Tính điểm sắc nét bằng Laplacian Variance trên ảnh đã chuẩn hóa về max 500px.
    Giúp điểm blur nhất quán độc lập với việc ảnh chụp 4K hay 720p.
    """
    norm_img = normalize_image(image_bgr, max_dim=500)
    gray = cv2.cvtColor(norm_img, cv2.COLOR_BGR2GRAY)
    return float(cv2.Laplacian(gray, cv2.CV_64F).var())


def compute_salient_product_mask(image_bgr):
    """
    Tách vùng sản phẩm chính (Salient Foreground Product) dựa trên thuật toán LAB Color Contrast
    & Center-weighted Distance Map.
    """
    img_norm = normalize_image(image_bgr, max_dim=500)
    img_h, img_w = img_norm.shape[:2]
    img_area = img_h * img_w
    
    # 1. Chuyển sang không gian màu LAB (phù hợp cho tương phản thị giác)
    lab = cv2.cvtColor(img_norm, cv2.COLOR_BGR2LAB)
    l, a, b = cv2.split(lab)
    
    # 2. Lấy mẫu màu phông nền từ 4 góc ảnh
    corner_w = int(img_w * 0.1)
    corner_h = int(img_h * 0.1)
    corners = np.vstack([
        lab[:corner_h, :corner_w].reshape(-1, 3),
        lab[:corner_h, -corner_w:].reshape(-1, 3),
        lab[-corner_h:, :corner_w].reshape(-1, 3),
        lab[-corner_h:, -corner_w:].reshape(-1, 3)
    ])
    bg_mean_lab = np.mean(corners, axis=0)
    
    # 3. Tính khoảng cách màu giữa từng pixel với màu phông nền
    dist_map = np.sqrt(
        (l.astype(float) - bg_mean_lab[0])**2 +
        (a.astype(float) - bg_mean_lab[1])**2 +
        (b.astype(float) - bg_mean_lab[2])**2
    )
    dist_map = cv2.normalize(dist_map, None, 0, 255, cv2.NORM_MINMAX).astype(np.uint8)
    
    # 4. Áp dụng trọng số trung tâm (Center Weight Map)
    cy, cx = img_h / 2.0, img_w / 2.0
    y_grid, x_grid = np.ogrid[:img_h, :img_w]
    dist_from_center = np.sqrt((x_grid - cx)**2 + (y_grid - cy)**2)
    max_dist = np.sqrt(cx**2 + cy**2)
    center_weight = 1.0 - (dist_from_center / max_dist) * 0.5  # Rời xa trung tâm giảm 50% trọng số
    
    weighted_saliency = (dist_map * center_weight).astype(np.uint8)
    
    # 5. Thresholding tìm mask sản phẩm
    _, mask = cv2.threshold(weighted_saliency, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    
    # Làm sạch mask bằng Morphological operations
    kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel)
    mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel)
    
    # 6. Lọc Contour sản phẩm
    contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    
    product_mask = np.zeros((img_h, img_w), dtype=np.uint8)
    max_ratio = 0.0
    found_product = False
    
    for cnt in contours:
        x, y, w, h = cv2.boundingRect(cnt)
        box_area = w * h
        ratio = box_area / img_area
        
        # BỘ LỌC CHUẨN XÁC:
        # - Vật thể phải nằm trong khoảng 8% đến 85% diện tích
        # - Không được là khối tràn viền chạm cả 4 mép ảnh
        touches_all_borders = (x <= 5 and y <= 5 and (x + w) >= img_w - 5 and (y + h) >= img_h - 5)
        
        if 0.08 <= ratio <= 0.85 and not touches_all_borders:
            cv2.drawContours(product_mask, [cnt], -1, 255, -1)
            found_product = True
            if ratio > max_ratio:
                max_ratio = ratio
                
    return product_mask, found_product, round(max_ratio, 4)


def calculate_background_clutter(image_bgr, product_mask=None):
    """
    Tính độ nhiễu phông nền (Clutter Score) ĐỘC LẬP BÊN NGOÀI vùng sản phẩm.
    """
    img_norm = normalize_image(image_bgr, max_dim=500)
    img_h, img_w = img_norm.shape[:2]
    gray = cv2.cvtColor(img_norm, cv2.COLOR_BGR2GRAY)
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    edges = cv2.Canny(blurred, 40, 120)
    
    if product_mask is not None and product_mask.shape == (img_h, img_w):
        # Đảo ngược mask sản phẩm → Lấy vùng phông nền bên ngoài
        bg_mask = cv2.bitwise_not(product_mask)
    else:
        # Nếu không có mask sản phẩm, lấy vùng 15% viền ngoài
        margin_h, margin_w = int(img_h * 0.15), int(img_w * 0.15)
        bg_mask = np.ones((img_h, img_w), dtype=np.uint8) * 255
        bg_mask[margin_h:img_h-margin_h, margin_w:img_w-margin_w] = 0
        
    bg_edges = cv2.bitwise_and(edges, edges, mask=bg_mask)
    bg_pixel_count = np.count_nonzero(bg_mask)
    
    if bg_pixel_count == 0:
        return 0.0
        
    edge_pixels = np.count_nonzero(bg_edges)
    clutter_score = edge_pixels / float(bg_pixel_count)
    return round(float(clutter_score), 4)


def detect_objects(image_bgr):
    """
    Hybrid Detection Engine:
    - YOLOv8n Object Detection (cho các lớp COCO chuẩn)
    - LAB Color Saliency Foreground Segmentation (cho mọi loại quần áo/giày dép/sản phẩm E-commerce)
    """
    model = get_model()
    img_norm = normalize_image(image_bgr, max_dim=600)
    img_h, img_w = img_norm.shape[:2]
    img_area = img_h * img_w
    
    VALID_PRODUCT_CLASSES = {
        'product_item', 'backpack', 'handbag', 'suitcase', 'tie',
        'sports ball', 'bottle', 'umbrella', 'person', 'shoes'
    }
    
    # ── 1. YOLO Detection ──
    results = model(img_norm, conf=0.15, verbose=False)
    yolo_max_ratio = 0.0
    detected_classes = []
    
    for result in results:
        boxes = result.boxes
        if boxes is None or len(boxes) == 0:
            continue
        for box in boxes:
            x1, y1, x2, y2 = box.xyxy[0].tolist()
            ratio = (x2 - x1) * (y2 - y1) / img_area
            cls_id = int(box.cls[0])
            cls_name = model.names.get(cls_id, f'class_{cls_id}')
            
            if ratio <= 0.85 and cls_name in VALID_PRODUCT_CLASSES:
                if ratio > yolo_max_ratio:
                    yolo_max_ratio = ratio
                if cls_name not in detected_classes:
                    detected_classes.append(cls_name)
                    
    # ── 2. Saliency Foreground Product Detection ──
    product_mask, found_salient_prod, salient_ratio = compute_salient_product_mask(image_bgr)
    
    if found_salient_prod and 'product_item' not in detected_classes:
        detected_classes.append('product_item')
        
    final_max_ratio = max(yolo_max_ratio, salient_ratio)
    has_valid_product = found_salient_prod or (yolo_max_ratio >= MIN_OBJECT_RATIO)
    
    return {
        'has_object': has_valid_product,
        'max_object_ratio': round(final_max_ratio, 4),
        'detected_classes': detected_classes,
        'num_objects': len(detected_classes),
        'product_mask': product_mask
    }


def analyze_image(image_bytes, filename='unknown'):
    nparr = np.frombuffer(image_bytes, np.uint8)
    image_bgr = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    
    if image_bgr is None:
        return {
            'approved': False, 'status': 'REJECTED',
            'reason': 'Không thể đọc file ảnh. Vui lòng kiểm tra định dạng (JPG/PNG).',
            'filename': filename,
            'metrics': {'blur_score': 0.0, 'blur_threshold': BLUR_THRESHOLD,
                        'max_object_ratio': 0.0, 'min_object_ratio_required': MIN_OBJECT_RATIO,
                        'clutter_score': 0.0, 'max_clutter_threshold': MAX_CLUTTER_THRESHOLD,
                        'num_objects': 0, 'detected_classes': []}
        }
        
    img_h, img_w = image_bgr.shape[:2]
    if img_w < 100 or img_h < 100:
        return {
            'approved': False, 'status': 'REJECTED',
            'reason': f'Ảnh quá nhỏ ({img_w}x{img_h}px). Yêu cầu tối thiểu 100x100px.',
            'filename': filename,
            'metrics': {'blur_score': 0.0, 'blur_threshold': BLUR_THRESHOLD,
                        'max_object_ratio': 0.0, 'min_object_ratio_required': MIN_OBJECT_RATIO,
                        'clutter_score': 0.0, 'max_clutter_threshold': MAX_CLUTTER_THRESHOLD,
                        'num_objects': 0, 'detected_classes': [], 'image_size': f'{img_w}x{img_h}'}
        }
    
    # ── 1. Blur Detection ──
    blur_score = calculate_blur_score(image_bgr)
    is_blurry = blur_score < BLUR_THRESHOLD
    
    # ── 2. Product Detection ──
    obj_result = detect_objects(image_bgr)
    has_sufficient_object = (obj_result['has_object'] and obj_result['max_object_ratio'] >= MIN_OBJECT_RATIO)
    
    # ── 3. Background Clutter Check (ở phông bên ngoài sản phẩm) ──
    product_mask = obj_result.get('product_mask')
    clutter_score = calculate_background_clutter(image_bgr, product_mask=product_mask)
    is_cluttered = clutter_score > MAX_CLUTTER_THRESHOLD
    
    # ── 4. Quyết định & Tổng hợp lý do ──
    reasons = []
    if is_blurry:
        reasons.append(f'Ảnh bị mờ (blur_score={blur_score:.1f} < ngưỡng={BLUR_THRESHOLD:.1f}). Vui lòng chụp lại với ánh sáng tốt hơn.')
    
    if not obj_result['has_object']:
        reasons.append('Không phát hiện được đối tượng sản phẩm trong ảnh. Vui lòng đảm bảo sản phẩm nằm rõ ở trung tâm khung hình.')
    elif not has_sufficient_object:
        reasons.append(f'Sản phẩm quá nhỏ (chiếm {obj_result["max_object_ratio"]*100:.1f}% < yêu cầu {MIN_OBJECT_RATIO*100:.0f}%). Vui lòng chụp cận hơn.')
        
    if is_cluttered:
        reasons.append(f'Phông nền quá lộn xộn (clutter_score={clutter_score:.2f} > ngưỡng={MAX_CLUTTER_THRESHOLD:.2f}). Vui lòng chọn phông nền đơn giản hoặc tách nền.')
        
    approved = len(reasons) == 0
    
    return {
        'approved': approved,
        'status': 'APPROVED' if approved else 'REJECTED',
        'reason': ' | '.join(reasons) if reasons else 'Ảnh đạt tiêu chuẩn chất lượng.',
        'filename': filename,
        'metrics': {
            'blur_score': round(blur_score, 2),
            'blur_threshold': BLUR_THRESHOLD,
            'is_blurry': is_blurry,
            'max_object_ratio': obj_result['max_object_ratio'],
            'min_object_ratio_required': MIN_OBJECT_RATIO,
            'clutter_score': clutter_score,
            'max_clutter_threshold': MAX_CLUTTER_THRESHOLD,
            'is_cluttered': is_cluttered,
            'num_objects': obj_result['num_objects'],
            'detected_classes': obj_result['detected_classes'],
            'image_size': f'{img_w}x{img_h}'
        }
    }



