# Nghiên cứu thuật toán OpenCV Laplacian Variance để phát hiện độ mờ ảnh

## 1. Cơ sở Toán học

### Toán tử Laplacian 2D
Toán tử Laplacian là một toán tử vi phân bậc hai trong không gian hai chiều, được dùng để đo lường mức độ thay đổi tốc độ tăng/giảm cường độ sáng tại một điểm so với các điểm lân cận. Đối với hàm cường độ sáng liên tục $f(x, y)$, toán tử Laplacian được định nghĩa:

$$\Delta f = \nabla^2 f = \frac{\partial^2 f}{\partial x^2} + \frac{\partial^2 f}{\partial y^2}$$

Trong xử lý ảnh số rời rạc, đạo hàm cấp 2 được xấp xỉ bằng công thức sai phân trung tâm (central difference):

$$\frac{\partial^2 f}{\partial x^2} \approx f(x+1, y) - 2f(x, y) + f(x-1, y)$$

$$\frac{\partial^2 f}{\partial y^2} \approx f(x, y+1) - 2f(x, y) + f(x, y-1)$$

Cộng hai đạo hàm trên, ta thu được biểu thức rời rạc của Laplacian tại vị trí điểm ảnh $(x, y)$:

$$\nabla^2 f(x, y) = f(x+1, y) + f(x-1, y) + f(x, y+1) + f(x, y-1) - 4f(x, y)$$

### Ma trận Hạt nhân (Kernel Matrix)
Biểu thức rời rạc trên tương đương với việc thực hiện phép tích chập (convolution) giữa ảnh $I$ và ma trận Kernel $K$ kích thước $3 \times 3$:

$$K = \begin{bmatrix} 0 & 1 & 0 \\ 1 & -4 & 1 \\ 0 & 1 & 0 \end{bmatrix}$$

---

## 2. Bản chất của Phương sai (Variance) trong phát hiện độ mờ

Biểu thức tính phương sai $\sigma^2$ của phản hồi Laplacian $L$:

$$\mu = \frac{1}{M \times N} \sum_{x=1}^{M} \sum_{y=1}^{N} L(x, y)$$

$$\text{Variance } (\sigma^2) = \frac{1}{M \times N} \sum_{x=1}^{M} \sum_{y=1}^{N} \left( L(x, y) - \mu \right)^2$$

- **Ảnh sắc nét (Sharp Image):** Chứa nhiều viền cạnh sắc nét với sự biến đổi cường độ đột ngột. Giá trị phản hồi Laplacian $\vert{}L(x, y)\vert{}$ chênh lệch cao $\rightarrow$ **Phương sai ($\sigma^2$) cao**.
- **Ảnh mờ / Out nét (Blurry Image):** Quá trình mờ làm mịn các chuyển tiếp cường độ sáng, đóng vai trò như bộ lọc thông thấp. Giá trị phản hồi biến thiên ít $\rightarrow$ **Phương sai ($\sigma^2$) thấp**.

---

## 3. Quy trình xử lý ảnh 5 bước

1. **Đọc ảnh BGR:** Tải ảnh từ đường dẫn đĩa hoặc bộ nhớ bằng `cv2.imread()`.
2. **Chuyển sang ảnh xám (Grayscale):** Chuyển kênh màu BGR về đơn kênh bằng `cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)` để loại bỏ nhiễu màu.
3. **Chuẩn hóa kích thước (Resize max 500px):** Đưa chiều dài nhất của ảnh về tối đa 500px nhằm đảm bảo tính nhất quán của điểm số phương sai (không phụ thuộc vào độ phân giải ảnh).
4. **Áp dụng toán tử Laplacian:** Sử dụng `cv2.Laplacian(gray_img, cv2.CV_64F)` để giữ nguyên các giá trị biến đổi âm/dương với kiểu dữ liệu float 64-bit.
5. **Tính điểm phương sai:** Gọi `.var()` trên kết quả thu được để nhận giá trị điểm số mờ (Blur Score).

---

## 4. Code mẫu Python PoC (Proof of Concept)

```python
import cv2
import numpy as np

def calculate_blur_score(image_path: str, max_size: int = 500) -> float:
    """
    Tính điểm mờ của ảnh bằng thuật toán Laplacian Variance.
    
    Args:
        image_path (str): Đường dẫn tới file ảnh.
        max_size (int): Kích thước cạnh tối đa sau khi resize. Default: 500.
        
    Returns:
        float: Điểm phương sai Laplacian (Variance Score).
    """
    # Step 1: Đọc ảnh BGR
    img = cv2.imread(image_path)
    if img is None:
        raise ValueError(f"Không thể đọc file ảnh tại: {image_path}")
        
    # Step 2: Chuyển sang ảnh xám (Grayscale)
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    
    # Step 3: Chuẩn hóa kích thước (Resize max 500px)
    h, w = gray.shape[:2]
    if max(h, w) > max_size:
        scale = max_size / float(max(h, w))
        new_w = int(w * scale)
        new_h = int(h * scale)
        gray = cv2.resize(gray, (new_w, new_h), interpolation=cv2.INTER_AREA)
        
    # Step 4: Áp dụng toán tử cv2.Laplacian với CV_64F
    laplacian_response = cv2.Laplacian(gray, cv2.CV_64F)
    
    # Step 5: Tính điểm phương sai .var()
    blur_score = float(laplacian_response.var())
    return blur_score


if __name__ == "__main__":
    # Demo kiểm thử
    sample_image = "test.jpg"
    try:
        score = calculate_blur_score(sample_image)
        threshold = 100.0  # Ngưỡng phân loại mẫu
        
        print(f"--- Blur Score: {score:.2f} ---")
        if score < threshold:
            print("Kết quả: Ảnh bị MỜ (BLURRY)")
        else:
            print("Kết quả: Ảnh SẮC NÉT (SHARP)")
    except Exception as e:
        print(f"Lỗi: {e}")