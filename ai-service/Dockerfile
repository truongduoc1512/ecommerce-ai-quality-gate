# Sử dụng image Python phiên bản rút gọn
FROM python:3.10-slim

# Thiết lập thư mục làm việc mặc định trong container
WORKDIR /app

# Cài đặt các thư viện hệ thống cần thiết cho OpenCV và YOLO
RUN apt-get update && apt-get install -y \
    libglib2.0-0 \
    libgl1 \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# Copy file requirements vào container trước để tận dụng Docker Cache
COPY requirements.txt .

# Cài đặt các thư viện Python
RUN pip install --no-cache-dir -r requirements.txt

# Pre-download YOLOv8n model vào image (baked-in, không cần internet khi chạy)
# Nếu đã có file .pt hợp lệ trong models/, nó sẽ được ưu tiên (xem image_qa.py)
RUN python -c "from ultralytics import YOLO; YOLO('yolov8n.pt')" && \
    mkdir -p /root/.config/Ultralytics

# Copy toàn bộ mã nguồn vào container
COPY . .

# Mở port 8000 để ứng dụng Spring Boot giao tiếp
EXPOSE 8000

# Lệnh khởi chạy server FastAPI
# main:app → file main.py ở root (re-export từ app.main)
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]