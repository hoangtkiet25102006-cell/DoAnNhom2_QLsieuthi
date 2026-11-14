Để chạy dự án trên máy khác, cần:

Toàn bộ thư mục mã nguồn dự án, không chỉ riêng file SQL. Có thể tải ZIP hoặc clone dự án.
JDK 17 — dự án khai báo Java 17 trong pom.xml.
PostgreSQL và psql để tạo, nạp cơ sở dữ liệu. File sieuthi_backup.sql chứa cấu trúc cùng dữ liệu đã sao lưu.
Không cần cài Maven riêng: dự án có Maven Wrapper (mvnw/mvnw.cmd). Cần Internet trong lần chạy đầu để tải thư viện.
Các bước cơ bản:

Tạo database tên sieuthi, rồi nạp file sao lưu bằng psql -U postgres -d sieuthi -f sieuthi_backup.sql.
Cập nhật thông tin host, tên database, username và password PostgreSQL trong application.properties cho khớp máy mới.
Chạy ./mvnw spring-boot:run trên Linux/macOS, hoặc mvnw.cmd spring-boot:run trên Windows.
Mở http://localhost:8080.
VS Code hoặc IntelliJ chỉ cần nếu muốn mở và sửa mã nguồn; không bắt buộc để chạy
