<<<<<<< HEAD
# Quản lý siêu thị

Ứng dụng web quản lý bán hàng và kho cho siêu thị, xây dựng bằng Java 17, Spring Boot, JDBC và PostgreSQL. Giao diện tĩnh HTML/CSS/JavaScript được phục vụ từ Spring Boot; dữ liệu được đọc và ghi qua REST API.

## Chức năng

- Tổng quan doanh thu, chi phí nhập hàng, chi hoàn tiền và báo cáo thu - chi theo tuần, tháng hoặc năm.
- Bán hàng tại quầy: quản lý giỏ hàng, thanh toán tiền mặt/thẻ/chuyển khoản, giảm giá thủ công và thẻ VIP.
- In phiếu thu cho đơn hàng; in lại từ chi tiết đơn hàng gần đây.
- Lập phiếu chi hoàn tiền liên kết với đơn hàng; hỗ trợ hoàn một phần hoặc nhiều lần, không cho tổng tiền hoàn vượt quá số tiền đơn hàng.
- Nhập kho theo nhà cung cấp, cập nhật tồn kho và ghi nhận chi phí nhập hàng.
- Quản lý mặt hàng, tồn kho, khách hàng và thẻ VIP.

Phiếu thu được tạo từ dữ liệu `don_hang` và `chi_tiet_don_hang`. Phiếu chi hoàn tiền được lưu vào bảng `phieu_chi`, liên kết bằng `ma_don_hang`; thao tác này ghi nhận khoản tiền hoàn nhưng không tự động điều chỉnh tồn kho hoặc hoàn tác đơn bán. Nếu cơ sở dữ liệu còn bảng `phieu_chi` theo cấu trúc cũ trong file sao lưu, schema hiện tại bổ sung mã phiếu và liên kết với `don_hang`, nới liên kết hóa đơn cũ thành tùy chọn và giữ lại các cột cũ để tương thích.

## Yêu cầu

- JDK 17 trở lên
- PostgreSQL
- Maven Wrapper có sẵn trong repository

## Chạy ứng dụng

1. Tạo database PostgreSQL tên `sieuthi`.
2. Cấu hình kết nối trong `src/main/resources/application.properties` (URL, tên đăng nhập và mật khẩu).
3. Khởi chạy ứng dụng:

   ```sh
   ./mvnw spring-boot:run
   ```

4. Mở `http://localhost:8080`.

Spring SQL initialization dùng `src/main/resources/schema.sql` để tạo/cập nhật cấu trúc bảng khi ứng dụng khởi động.

## Kiểm thử

```sh
./mvnw test
```

## Cấu trúc SQL

- `src/main/resources/schema.sql` là schema được ứng dụng hiện tại khởi tạo và sử dụng.
- `sieuthi_backup.sql` là bản sao lưu của schema cũ. Bản sao lưu có bảng `phieu_chi` theo cấu trúc cũ nhưng không có `phieu_thu`; dữ liệu phiếu chi trong bản sao lưu đang rỗng. Phiếu thu hiện tại được lấy từ dữ liệu đơn hàng; schema ứng dụng bổ sung liên kết `phieu_chi` với `don_hang` cho nghiệp vụ hoàn tiền.
=======
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
>>>>>>> 8f25c0857ec3b944bef6f138affee32ce6bba332
