# ==============================================================================
# ShoeShop AI Quality Gate Engine Service - Dockerfile
# ==============================================================================
# Multi-stage lightweight container build for FastAPI + YOLOv8 + OpenCV Engine:
# - Base Image: python:3.10-slim
# - System Libraries: libgl1, libglib2.0-0, libgomp1 (headless C++ runtimes)
# - CPU-Only PyTorch optimization (< 800MB image vs > 4GB CUDA)
# - Pre-baked yolov8n.pt model for offline execution in isolated networks
# ==============================================================================

FROM python:3.10-slim

WORKDIR /app

# Set environment variables for Python & Ultralytics
ENV PYTHONUNBUFFERED=1     PYTHONDONTWRITEBYTECODE=1     YOLO_CONFIG_DIR=/root/.config/Ultralytics

# Install essential build & C++ runtime dependencies without X11 bloat
RUN apt-get update && apt-get install -y --no-install-recommends     libgl1     libglib2.0-0     libgomp1     curl     && rm -rf /var/lib/apt/lists/*

# Copy requirements.txt and install CPU-only PyTorch and dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Model Baking Step: Pre-download yolov8n.pt for isolated offline network execution
RUN python -c "from ultralytics import YOLO; YOLO('yolov8n.pt')" &&     mkdir -p /root/.config/Ultralytics /app/models &&     cp yolov8n.pt /app/models/ 2>/dev/null || true

# Copy application source code
COPY . .

EXPOSE 8000

# Healthcheck to verify container readiness
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3     CMD curl -f http://localhost:8000/health || exit 1

# Start Uvicorn ASGI server
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
