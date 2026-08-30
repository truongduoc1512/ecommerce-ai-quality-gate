# Báo cáo lỗi 502 Bad Gateway và xử lý khắc phục

## 1. Tổng quan

Trong quá trình khởi động và kiểm thử toàn bộ hệ thống bằng Docker Compose, ứng dụng gặp lỗi HTTP 502 Bad Gateway khi truy cập `http://localhost` qua Nginx.

Hệ thống gồm 4 thành phần chính:
- MySQL: `shoeshop-ai-mysql`
- AI microservice FastAPI: `shoeshop-ai-container`
- Backend Spring Boot: `shoeshop-ai-api`
- Reverse proxy Nginx: `shoeshop-ai-nginx`

## 2. Triệu chứng

### 2.1. Hiện tượng quan sát được
- Truy cập `http://localhost` trả về `502 Bad Gateway`
- Nginx log hiển thị lỗi:
  - `connect() failed (111: Connection refused) while connecting to upstream`
  - `upstream: "http://172.18.0.2:8080/"`

### 2.2. Dấu hiệu rõ ràng
- Lỗi xảy ra ở tầng proxy Nginx, không phải lỗi render giao diện HTML
- Nginx không thể kết nối tới Spring Boot backend ở port `8080`
- Backend của ứng dụng khi kiểm tra trực tiếp lại đang chạy đúng

## 3. Điều tra nguyên nhân

### 3.1. Kiểm tra log Nginx

Log Nginx xác nhận rõ:
- request đến `/` bị Nginx proxy sang `http://172.18.0.2:8080/`
- Nginx báo `Connection refused`

Điều này cho thấy Nginx đang cố truy cập upstream nhưng backend chưa sẵn sàng hoặc đang không lắng nghe trên port mong đợi.

### 3.2. Kiểm tra log backend

Log Spring Boot cho thấy backend khởi động thành công:
- `Tomcat initialized with port(s): 8080 (http)`
- `Tomcat started on port(s): 8080`
- `Started SpringShoppingCart2Application`

Vậy backend thực sự đã bắt đầu, nhưng Nginx đã cố truy cập quá sớm trong giai đoạn khởi động hoặc do cấu hình proxy không đảm bảo startup ordering.

### 3.3. Kiểm tra mạng Docker nội bộ

Đã chạy kiểm tra từ trong container Nginx:

```bash
docker exec shoeshop-ai-nginx sh -c "getent hosts ai-shoeshop-backend; nslookup ai-shoeshop-backend; wget -S --spider -T 5 http://ai-shoeshop-backend:8080 || curl -I --max-time 5 http://ai-shoeshop-backend:8080 || echo 'UPSTREAM_CONNECT_FAILED'"
```

Kết quả cho thấy:
- DNS resolve đúng `ai-shoeshop-backend -> 172.18.0.3`
- request tới `http://ai-shoeshop-backend:8080` trả về `HTTP/1.1 200`

Như vậy, không phải lỗi DNS hoặc lỗi backend vĩnh viễn. Lỗi 502 ban đầu là do thời điểm khởi động container không đồng bộ, gây Nginx gọi đến backend trước khi backend sẵn sàng.

## 4. Hành vi cụ thể của hệ thống Docker

File compose đang có cấu hình:
- MySQL: healthcheck ping
- AI service: khởi động cùng stack
- Backend: có `depends_on` nhưng chỉ đảm bảo service được khởi động, không đảm bảo backend ready
- Nginx: chạy song song với backend, nhưng không chờ healthy status

Do đó, hệ thống có thể rơi vào trạng thái:
1. Nginx khởi động trước backend
2. Nginx nhận request sớm
3. Nginx proxy tới `ai-shoeshop-backend:8080`
4. Backend chưa kịp lắng nghe hoặc đang khởi tạo
5. `Connection refused` -> 502

## 5. Các bước sửa lỗi

### Bước 1: Cập nhật healthcheck cho backend
Trong file `docker-compose.yml`, thêm healthcheck cho backend để xác định khi ứng dụng đã sẵn sàng phục vụ:

```yaml
healthcheck:
  test: ["CMD-SHELL", "wget -q --spider http://localhost:8080 || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 10
  start_period: 20s
```

### Bước 2: Chỉ cho Nginx khởi động sau khi backend healthy
Sửa `depends_on` của Nginx:

```yaml
depends_on:
  ai-shoeshop-backend:
    condition: service_healthy
```

### Bước 3: Cấu hình Nginx rõ ràng hơn
Trong file `nginx.conf`, thêm:
- `resolver 127.0.0.11 ipv6=off valid=10s;`
- timeout kết nối và đọc
- `proxy_next_upstream` để retry khi upstream tạm thời lỗi

Ví dụ:

```nginx
resolver 127.0.0.11 ipv6=off valid=10s;

server {
    listen 80;
    server_name localhost;

    location / {
        proxy_pass http://ai-shoeshop-backend:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 5s;
        proxy_read_timeout 60s;
        proxy_next_upstream error timeout invalid_header http_500 http_502 http_503;
    }
}
```

### Bước 4: Xóa stack cũ và restart từ đầu
Thực hiện clean restart để tránh container cũ còn chiếm tên và trạng thái không đồng bộ:

```bash
docker rm -f shoeshop-ai-api shoeshop-ai-nginx shoeshop-ai-container shoeshop-ai-mysql 2>$null
docker-compose up -d --build --remove-orphans
```

## 6. Kết quả kiểm chứng sau khi sửa

Sau khi áp dụng các bước trên, chạy kiểm tra sau:

```powershell
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
Invoke-WebRequest -Uri http://localhost -UseBasicParsing | Select-Object StatusCode, StatusDescription, ContentLength
```

Kết quả thực tế đã thu được:
- `shoeshop-ai-nginx` đang chạy
- `shoeshop-ai-api` health status OK
- `shoeshop-ai-mysql` healthy
- `http://localhost` trả về `StatusCode 200`

## 7. Kết luận

Lỗi 502 không phải do giao diện web bị lỗi, mà là do lỗi khởi động phụ thuộc giữa Nginx và Spring Boot trong Docker Compose. Nginx được cấu hình proxy tới upstream quá sớm, nên khi backend chưa ready, Nginx trả `502 Bad Gateway`.

Sau khi thêm healthcheck và chờ backend healthy, hệ thống hoạt động ổn định.

## 8. Gợi ý phòng ngừa trong tương lai

- Luôn dùng `healthcheck` cho các service phụ thuộc
- Không chỉ dùng `depends_on` đơn giản nếu cần yêu cầu ready
- Khi deploy hoặc restart, nên dùng clean restart để tránh container cũ và tên trùng
- Nên lưu log Nginx và backend khi xảy ra 502 để nhanh chóng xác định upstream nào lỗi

## 9. File liên quan

- [docker-compose.yml](docker-compose.yml)
- [nginx.conf](nginx.conf)
- [README.md](README.md)
