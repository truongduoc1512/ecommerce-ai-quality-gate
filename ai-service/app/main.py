"""
FastAPI Main Application Module
===============================
Định nghĩa các API endpoints cho AI Quality Gate Service:
- GET  / : Health Check endpoint
- GET  /health : Microservice Status endpoint
- POST /api/v1/analyze : Endpoint kiểm duyệt ảnh sản phẩm tự động
"""

from fastapi import FastAPI, File, UploadFile, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from app import __version__
from app.image_qa import analyze_image

# Khởi tạo ứng dụng FastAPI với OpenAPI metadata đầy đủ
app = FastAPI(
    title="ShoeShop AI Quality Gate Service",
    description="Dịch vụ AI kiểm duyệt chất lượng hình ảnh sản phẩm tự động (YOLOv8 + OpenCV)",
    version=__version__,
    docs_url="/docs",
    redoc_url="/redoc"
)

# Cấu hình CORS Middleware cho phép Spring Boot / Frontend gọi API
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}


@app.get("/", tags=["Health Check"])
def root_health_check():
    """Health check endpoint cho Docker & Load Balancer."""
    return {
        "service": "ShoeShop AI Quality Gate Service",
        "status": "UP",
        "version": __version__
    }


@app.get("/health", tags=["Health Check"])
def health_status():
    """Health check chi tiết."""
    return {
        "status": "HEALTHY",
        "microservice": "ai-service",
        "version": __version__
    }


@app.post("/api/v1/analyze", tags=["Image Quality Assessment"])
async def analyze_product_image(file: UploadFile = File(...)):
    """
    Endpoint nhận file ảnh sản phẩm từ Spring Boot Backend và phân tích chất lượng:
    - 1. Độ sắc nét (Blur Score via OpenCV Laplacian Variance)
    - 2. Nhận diện đối tượng sản phẩm (Object Detection & Coverage Ratio via YOLOv8)
    
    Returns JSON response với approved=true/false và các chỉ số đo lường chi tiết.
    """
    # 1. Kiểm tra định dạng file
    filename = file.filename or "unknown.jpg"
    ext = "." + filename.split(".")[-1].lower() if "." in filename else ""
    
    if ext not in ALLOWED_EXTENSIONS:
        return JSONResponse(
            status_code=status.HTTP_400_BAD_REQUEST,
            content={
                "approved": False,
                "status": "REJECTED",
                "reason": f"Định dạng file '{ext}' không được hỗ trợ. Vui lòng gửi ảnh (.jpg, .jpeg, .png, .webp, .bmp).",
                "filename": filename
            }
        )

    # 2. Đọc file byte stream và gọi thuật toán AI
    try:
        contents = await file.read()
        if not contents or len(contents) == 0:
            return JSONResponse(
                status_code=status.HTTP_400_BAD_REQUEST,
                content={
                    "approved": False,
                    "status": "REJECTED",
                    "reason": "File ảnh tải lên bị rỗng (0 bytes).",
                    "filename": filename
                }
            )

        # Gọi hàm xử lý phân tích AI
        result = analyze_image(image_bytes=contents, filename=filename)
        return JSONResponse(content=result)

    except Exception as e:
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={
                "approved": False,
                "status": "ERROR",
                "reason": f"Lỗi hệ thống AI Service: {str(e)}",
                "filename": filename
            }
        )