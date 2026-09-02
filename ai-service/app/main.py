"""
ShoeShop AI Quality Gate Service - FastAPI Application
======================================================
Production REST API service providing automated image quality checks:
- Root & Health Check Endpoints
- /api/v1/analyze: Image Quality Gate endpoint integrating YOLOv8 & OpenCV Blur engines
"""

import os
from fastapi import FastAPI, File, UploadFile, Query, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from app.services.analysis_service import analyze_product_image_data

ALLOWED_IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}

app = FastAPI(
    title="ShoeShop AI Quality Gate Engine API",
    description="Microservice evaluating product image quality for ShoeShop E-commerce Platform",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/", summary="Root Health Endpoint")
def read_root():
    return {
        "service": "ShoeShop AI Quality Gate Engine API",
        "status": "HEALTHY",
        "version": "1.0.0"
    }


@app.get("/health", summary="Service Health Check")
def health_check():
    return {
        "status": "UP",
        "engine": "YOLOv8 + OpenCV Laplacian",
        "uptime": "HEALTHY"
    }


@app.post(
    "/api/v1/analyze",
    summary="Analyze Product Image Quality",
    response_class=JSONResponse
)
async def analyze_image_endpoint(
    file: UploadFile = File(...),
    blur_threshold: float = Query(50.0, ge=0.0, le=1000.0, description="Laplacian blur variance threshold"),
    min_object_ratio: float = Query(0.08, ge=0.0, le=1.0, description="Minimum object coverage ratio")
):
    filename = file.filename or "unknown.jpg"
    ext = os.path.splitext(filename)[1].lower()

    # 1. HTTP 400 - Unsupported File Extension Check
    if ext not in ALLOWED_IMAGE_EXTENSIONS:
        return JSONResponse(
            status_code=status.HTTP_400_BAD_REQUEST,
            content={
                "approved": False,
                "status": "REJECTED",
                "reason": f"??nh d?ng file '{ext}' kh?ng ???c h? tr?. Vui l?ng t?i l?n file ?nh (.jpg, .jpeg, .png, .webp, .bmp).",
                "filename": filename
            }
        )

    try:
        image_bytes = await file.read()

        # 2. HTTP 400 - Empty File (0 Bytes) Check
        if not image_bytes or len(image_bytes) == 0:
            return JSONResponse(
                status_code=status.HTTP_400_BAD_REQUEST,
                content={
                    "approved": False,
                    "status": "REJECTED",
                    "reason": "File t?i l?n r?ng (0 bytes). Vui l?ng ch?n m?t file ?nh h?p l?.",
                    "filename": filename
                }
            )

        # 3. AI Pipeline Analysis
        result = analyze_product_image_data(
            image_bytes=image_bytes,
            filename=filename,
            blur_threshold=blur_threshold,
            min_object_ratio=min_object_ratio
        )

        return JSONResponse(status_code=status.HTTP_200_OK, content=result)

    except Exception as exc:
        # 4. HTTP 500 - Internal Server Error Safety Wrapper
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={
                "approved": False,
                "status": "ERROR",
                "reason": f"L?i h? th?ng AI Service: {str(exc)}",
                "filename": filename
            }
        )
