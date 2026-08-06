from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse
from app.image_qa import analyze_image

# Khởi tạo ứng dụng FastAPI
app = FastAPI(
    title="AI Image QA Service",
    description="Dịch vụ AI kiểm duyệt ảnh sản phẩm tự động cho ShoeShop",
    version="1.0.0"
)

@app.get("/")
def health_check():
    """Health-check endpoint — Docker & Spring Boot dùng để kiểm tra service còn sống."""
    return {"status": "AI Service is running perfectly", "version": "1.0.0"}

@app.post("/api/v1/analyze")
async def analyze_product_image(file: UploadFile = File(...)):
    """
    Endpoint kiểm duyệt ảnh sản phẩm.
    
    Nhận file ảnh từ Spring Boot backend (AdminController), phân tích:
    - Độ sắc nét (Blur Detection via OpenCV Laplacian)
    - Phát hiện đối tượng (Object Detection via YOLOv8n)
    
    Returns JSON với approved=true/false, reason, và metrics chi tiết.
    """
    try:
        contents = await file.read()
        result = analyze_image(image_bytes=contents, filename=file.filename or "unknown")
        return JSONResponse(content=result)

    except Exception as e:
        return JSONResponse(
            status_code=500,
            content={
                "approved": False,
                "status": "ERROR",
                "reason": f"Lỗi hệ thống AI: {str(e)}",
                "filename": file.filename or "unknown"
            }
        )