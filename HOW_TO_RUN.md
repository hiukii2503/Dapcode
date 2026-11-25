# Hướng dẫn Cài đặt và Chạy Dự án Dapcode (LiệtCode)

Tài liệu hướng dẫn từng bước khởi chạy toàn bộ hệ thống **Dapcode (LiệtCode)** gồm: **PostgreSQL Database**, **Judge0 Sandbox**, **Backend (Spring Boot + DAO/HikariCP)** và **Frontend (React + Bootstrap 5.3)**.

---

## 1. Yêu cầu môi trường (Prerequisites)

Đảm bảo máy tính đã cài đặt các công cụ sau:
- **Java JDK 21** trở lên (`java -version`)
- **Node.js 18+** & **npm** (`node -v`, `npm -v`)
- **Docker Desktop** (để chạy PostgreSQL 16 và Judge0 Sandbox)

---

## 2. Bước 1: Khởi chạy Cơ sở dữ liệu (PostgreSQL) & Máy chấm code (Judge0)

Mở **Docker Desktop**, sau đó mở Terminal tại thư mục gốc dự án và chạy:

### 2.1. Khởi chạy PostgreSQL (Cổng `5432`)
```bash
docker compose -f infra/database/docker-compose.db.yml up -d
```
- **Database**: `dapcode_db`
- **Port**: `5432`
- **Username / Password**: `postgres` / `123123`

*(Tùy chọn — Nạp nhanh dữ liệu mẫu từ file SQL export nếu muốn khởi tạo thủ công thay vì để Flyway tự chạy)*:
```bash
docker exec -i dapcode-postgres psql -U postgres -d dapcode_db < dapcode_database_final.sql
```

### 2.2. Khởi chạy Judge0 Sandbox (Cổng `2358` — phục vụ tính năng Run / Submit code)
```bash
docker compose -f infra/sandbox/docker-compose.sandbox.yml up -d
```
Kiểm tra các container đang hoạt động:
```bash
docker ps
```

---

## 3. Bước 2: Cấu hình Biến môi trường Backend

1. Sao chép file mẫu cấu hình biến môi trường:
   ```bash
   cp backend-core/.env.example backend-core/.env
   ```
   *(Trên Windows PowerShell: `Copy-Item backend-core/.env.example backend-core/.env`)*
2. Mở file `backend-core/.env` và cập nhật các giá trị phù hợp (mặc định DB đã khớp cổng `5432`, `JWT_SECRET` nên điền chuỗi bí mật tối thiểu 32 ký tự):
   ```dotenv
   DB_URL=jdbc:postgresql://localhost:5432/dapcode_db
   DB_USERNAME=postgres
   DB_PASSWORD=123123
   JWT_SECRET=thay_bang_chuoi_bi_mat_toi_thieu_32_ky_tu_tai_day
   ```
3. Các thông số tối ưu **HikariCP Connection Pool** của tầng DAO được tách riêng tại file `backend-core/src/main/resources/hikari.properties`.

---

## 4. Bước 3: Khởi chạy Backend (Spring Boot — Cổng `8080`)

Mở Terminal tại thư mục `backend-core`:

* **Trên Windows (PowerShell / CMD)**:
  ```powershell
  cd backend-core
  .\mvnw.cmd spring-boot:run
  ```
* **Trên macOS / Linux**:
  ```bash
  cd backend-core
  ./mvnw spring-boot:run
  ```

Khi thấy dòng `Started BackendCoreApplication ...` và `Tomcat started on port 8080`:
- **API Base URL**: `http://localhost:8080/api/v1`
- **Tài liệu API (Swagger UI)**: `http://localhost:8080/swagger-ui/index.html`

---

## 5. Bước 4: Khởi chạy Frontend (React + Bootstrap 5.3 — Cổng `5173`)

Mở một cửa sổ Terminal mới tại thư mục `DapcodeFrontend`:

```bash
cd DapcodeFrontend
npm install
npm run dev
```

Truy cập ứng dụng trên trình duyệt:
- **Trang Học viên (Roadmap / Bài tập / Bài giảng)**: `http://localhost:5173`
- **Trang Quản trị (Admin Dashboard)**: `http://localhost:5173/admin`

---

## 6. Bước 5: Chạy Hệ thống Kiểm thử Tự động (Unit Tests & Integration Tests)

Đảm bảo container PostgreSQL (`dapcode-postgres` trên cổng `5432`) đang chạy, sau đó thực thi tại thư mục `backend-core`:

* **Chạy toàn bộ 75 bài kiểm thử (26 Unit Tests + 49 Integration Tests)**:
  ```powershell
  cd backend-core
  .\mvnw.cmd test
  ```
* **Hoặc chỉ chạy bộ Unit Tests (không cần bật Database)**:
  ```powershell
  cd backend-core
  .\mvnw.cmd test "-Dtest=*ServiceTest"
  ```

Danh sách các bộ kiểm thử tích hợp (Integration Tests):
1. `ProblemDAOIntegrationTest` (5 kịch bản): CRUD, Tìm kiếm, Lọc, Phân trang `LIMIT/OFFSET` và Validation qua HikariCP.
2. `SubmissionIntegrationTest` (5 kịch bản): Vòng đời nộp bài `PENDING -> ACCEPTED`, bọc hàm `main` (`CodeHarness`), chống trùng XP, che giấu test case ẩn.
3. `AdminLoginFlowIntegrationTest` (18 kịch bản): Đăng nhập BCrypt, xác thực chữ ký JWT, Filter chặn truy cập trái phép (`401`/`403`).
4. `AdminUserManagementFlowIntegrationTest` (21 kịch bản): Tìm kiếm học viên, khóa tài khoản thu hồi phiên tức thì tại Filter, mở khóa tài khoản.

---

## 7. Tài khoản Đăng nhập Mẫu (Phục vụ Demo)

| Vai trò | Đường dẫn truy cập | Tên đăng nhập / Email | Mật khẩu |
| :--- | :--- | :--- | :--- |
| **Quản trị viên (`ROLE_ADMIN`)** | `http://localhost:5173/admin` | `admin` *(hoặc `dapcode_admin` / `admin@dapcode.io`)* | `admin123` |
| **Học viên (`ROLE_USER`)** | `http://localhost:5173` | Đăng nhập Google OAuth2 hoặc `student01@dapcode.io` | `admin123` |

---

## 8. Xử lý nhanh một số lỗi thường gặp

- **Lỗi `Connection to localhost:5432 refused`**: Docker Desktop chưa bật hoặc container `dapcode-postgres` chưa chạy $\rightarrow$ chạy lại `docker compose -f infra/database/docker-compose.db.yml up -d`.
- **Lỗi chiếm cổng `5432` trên máy có sẵn PostgreSQL nội bộ**: Dừng dịch vụ PostgreSQL local trên Windows Services hoặc đổi cổng ánh xạ trong `docker-compose.db.yml` khớp với `DB_URL` trong `.env`.
- **Lỗi không chấm được code khi bấm Submit**: Đảm bảo các container Judge0 (`dapcode-judge0-server`, `dapcode-judge0-workers`, `dapcode-judge0-redis`) đang chạy ở cổng `2358`.
