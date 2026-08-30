# AI Model Evaluation Report: Object Detection & Quality Assessment
**Sprint:** 3  
**Document:** docs/reports/Confusion_Matrix_AI_Report.md  
**Branch:** docs/sprint-3-confusion-matrix-report  
**Evaluation Set:** 50 Ground-Truth Test Samples  

---

## 1. Theoretical Background

### 1.1 Mathematical Foundations of YOLOv8 Architecture

YOLOv8 sử dụng kiến trúc mạng nơ-ron tích chập liền mạch (anchor-free single-stage detector) gồm 3 phần chính: Backbone, Neck (PAN-FPN), và Decoupled Head.

#### A. Anchor-Free Regression & Task-Aligned Assigner (TAL)
Thay vì sử dụng các anchor boxes định sẵn, YOLOv8 dự đoán trực tiếp khoảng cách từ tâm anchor point $(x, y)$ đến 4 cạnh của bounding box: $l, t, r, b$ (left, top, right, bottom).

Để gán nhãn mẫu trong huấn luyện, YOLOv8 áp dụng chỉ số căn chỉnh nhiệm vụ (Task-Aligned Metric):
$$t = s^\alpha \times \text{IoU}^\beta$$
Trong đó:
- $s$: Điểm xác suất dự đoán của lớp (classification score).
- $\text{IoU}$: Mức độ giao thoa giữa box dự đoán và ground-truth box.
- $\alpha, \beta$: Hệ số cân bằng giữa độ chính xác phân loại và định vị.

#### B. Loss Functions Formulation
Tổng hàm mất mát của YOLOv8 được biểu diễn:
$$\mathcal{L}_{total} = \lambda_{cls} \mathcal{L}_{cls} + \lambda_{box} \mathcal{L}_{box} + \lambda_{dfl} \mathcal{L}_{dfl}$$

1. **Classification Loss ($\mathcal{L}_{cls}$):** Sử dụng Binary Cross-Entropy (BCE) Loss:
   $$\mathcal{L}_{cls} = -\frac{1}{N} \sum_{i=1}^N \left[ y_i \log(\hat{p}_i) + (1 - y_i) \log(1 - \hat{p}_i) \right]$$

2. **Bounding Box Regression Loss ($\mathcal{L}_{box}$):** Sử dụng Complete IoU (CIoU):
   $$\mathcal{L}_{CIoU} = 1 - \text{IoU} + \frac{\rho^2(b, b^{gt})}{c^2} + \alpha v$$
   Trong đó:
   - $\rho(b, b^{gt})$: Khoảng cách Euclidean giữa hai tâm hộp.
   - $c$: Độ dài đường chéo của hộp chữ nhật nhỏ nhất bao quanh cả hai hộp.
   - $v = \frac{4}{\pi^2} \left( \arctan\frac{w^{gt}}{h^{gt}} - \arctan\frac{w}{h} \right)^2$ đo tính nhất quán của tỷ lệ khung hình.
   - $\alpha = \frac{v}{(1 - \text{IoU}) + v}$.

3. **Distribution Focal Loss ($\mathcal{L}_{dfl}$):** Tối ưu hóa phân phối xác suất quanh ground-truth $y$:
   $$\mathcal{L}_{dfl}(S_i, S_{i+1}) = - \left( (y_{i+1} - y)\log(S_i) + (y - y_i)\log(S_{i+1}) \right)$$

---

### 1.2 OpenCV Laplacian Variance Blur Detection

Thuật toán phát hiện độ mờ (blur detection) của Pech-Pacheco dựa trên toán tử vi phân bậc 2 Laplace applied lên ảnh xám $I(x, y)$.

#### A. Continuous & Discrete Formulation
Toán tử Laplace liên tục:
$$\Delta I = \nabla^2 I = \frac{\partial^2 I}{\partial x^2} + \frac{\partial^2 I}{\partial y^2}$$

Rời rạc hóa bằng phép nhân chập với kernel $3 \times 3$:
$$K_L = \begin{bmatrix} 0 & 1 & 0 \\ 1 & -4 & 1 \\ 0 & 1 & 0 \end{bmatrix} \quad \text{hoặc} \quad K_{L8} = \begin{bmatrix} 1 & 1 & 1 \\ 1 & -8 & 1 \\ 1 & 1 & 1 \end{bmatrix}$$

Phản hồi biên cạnh tại điểm ảnh $(x, y)$:
$$L(x, y) = I(x, y) * K_L$$

#### B. Blur Metric (Variance of Laplacian)
Độ sắc nét của hình ảnh tỷ lệ thuận với phương sai của ma trận phản hồi Laplace:
$$\bar{L} = \frac{1}{M \times N} \sum_{x=1}^M \sum_{y=1}^N L(x, y)$$
$$\text{Score} = \sigma^2_L = \frac{1}{M \times N} \sum_{x=1}^M \sum_{y=1}^N \left( L(x, y) - \bar{L} \right)^2$$

- **Quy tắc phân loại:** 
  - Nếu $\sigma^2_L < \tau_{blur}$: Ảnh bị mờ (Blurry).
  - Nếu $\sigma^2_L \ge \tau_{blur}$: Ảnh rõ nét (Sharp).

---

## 2. Confusion Matrix Evaluation (50 Ground-Truth Samples)

Đánh giá hệ thống trên tập kiểm thử gồm **50 mẫu gán nhãn thực tế** (Ground Truth), với ngưỡng quyết định $\text{IoU} \ge 0.5$ và Confidence Score $\ge 0.5$.

### 2.1 Định nghĩa thực nghiệm
- **True Positive (TP):** Mẫu Positive được mô hình nhận diện chính xác là Positive.
- **False Negative (FN):** Mẫu Positive nhưng mô hình bỏ sót hoặc dự đoán sai thành Negative.
- **False Positive (FP):** Mẫu Negative nhưng mô hình phát hiện nhầm là Positive (hoặc dự đoán sai vị trí).
- **True Negative (TN):** Mẫu Negative và mô hình xác nhận chính xác không có đối tượng.

### 2.2 Ma trận nhầm lẫn

| | Dự đoán Positive (Predicted Pos) | Dự đoán Negative (Predicted Neg) | Tổng Ground Truth |
|---|---|---|---|
| **Thực tế Positive (Actual Pos)** | **TP = 38** | **FN = 4** | **42** |
| **Thực tế Negative (Actual Neg)** | **FP = 3** | **TN = 5** | **8** |
| **Tổng số dự đoán** | **41** | **9** | **50** |



---

## 3. Metric Calculations

Áp dụng các công thức chuẩn:

$$\text{Accuracy} = \frac{\text{TP} + \text{TN}}{\text{TP} + \text{TN} + \text{FP} + \text{FN}} = \frac{38 + 5}{50} = \frac{43}{50} = 0.8600 \quad (86.00\%)$$

$$\text{Precision} = \frac{\text{TP}}{\text{TP} + \text{FP}} = \frac{38}{38 + 3} = \frac{38}{41} \approx 0.9268 \quad (92.68\%)$$

$$\text{Recall} = \frac{\text{TP}}{\text{TP} + \text{FN}} = \frac{38}{42} \approx 0.9048 \quad (90.48\%)$$

$$\text{F1-Score} = 2 \times \frac{\text{Precision} \times \text{Recall}}{\text{Precision} + \text{Recall}} = 2 \times \frac{0.9268 \times 0.9048}{0.9268 + 0.9048} = \frac{1.6771}{1.8316} \approx 0.9156 \quad (91.56\%)$$

### Bảng tổng kết chỉ số

| Chỉ số (Metric) | Công thức | Giá trị tính toán | Tỷ lệ phần trăm |
|---|---|---|---|
| **Accuracy** | $\frac{TP + TN}{TP + TN + FP + FN}$ | $43 / 50$ | **86.00%** |
| **Precision** | $\frac{TP}{TP + FP}$ | $38 / 41$ | **92.68%** |
| **Recall** | $\frac{TP}{TP + FN}$ | $38 / 42$ | **90.48%** |
| **F1-Score** | $2 \times \frac{Precision \times Recall}{Precision + Recall}$ | $1.6771 / 1.8316$ | **91.56%** |

---

## 4. Failure Case Analysis

Phân tích nguyên nhân gốc rễ (Root Cause Analysis) của 7 trường hợp dự đoán sai ($4 \text{ FN} + 3 \text{ FP}$):

### 4.1 Phân tích False Negatives (FN = 4)
* **Sample #12 & #27 (Motion Blur & Low Contrast):**
  - *Hiện tượng:* Camera rung nhẹ khiến giá trị Laplacian Variance giảm sâu ($\sigma^2_L < 65$), làm mất thông tin biên cạnh (high-frequency components).
  - *Nguyên nhân:* Backbone YOLOv8 không trích xuất đủ feature maps cục bộ ở tầng P3/8.
* **Sample #39 (Heavy Occlusion > 60%):**
  - *Hiện tượng:* Vật thể bị che khuất phần lớn bởi vật cản phía trước.
  - *Nguyên nhân:* Confidence score dự đoán chỉ đạt $0.41$, dưới ngưỡng filter threshold ($0.5$).
* **Sample #44 (Extreme Scale Variation):**
  - *Hiện tượng:* Kích thước bounding box thực tế quá nhỏ ($< 16 \times 16$ pixels).

### 4.2 Phân tích False Positives (FP = 3)
* **Sample #08 & #19 (Background Clutter & Texture Similarity):**
  - *Hiện tượng:* Chi tiết nền có vân bề mặt phản quang lặp lại gây kích hoạt cụm điểm tích chập tương tự hình dáng vật thể.
* **Sample #33 (Duplicate Detection / NMS Failure):**
  - *Hiện tượng:* Một vật thể bị sinh ra 2 bounding boxes chồng lấn có IoU xấp xỉ $0.48$ (vượt qua ngưỡng Non-Maximum Suppression tiêu chuẩn $\text{IoU}_{NMS} = 0.45$).

### 4.3 Đề xuất cải tiến cho Sprint tiếp theo
1. **Tiền xử lý:** Tích hợp bộ lọc Laplacian Variance để loại bỏ hoặc cảnh báo ảnh đầu vào có $\sigma^2_L < 75$ trước khi đưa vào mô hình phát hiện.
2. **Data Augmentation:** Bổ sung tập dữ liệu huấn luyện bằng kỹ thuật Mosaic và MixUp với các mẫu che khuất ngẫu nhiên (CutOut/Random Erasing).
3. **Hiệu chỉnh NMS:** Thử nghiệm Soft-NMS để giải quyết bài toán vật thể sát nhau và triệt tiêu box trùng lặp tốt hơn.