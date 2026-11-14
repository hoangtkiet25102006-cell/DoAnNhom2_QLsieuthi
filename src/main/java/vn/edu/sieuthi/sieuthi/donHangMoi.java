package vn.edu.sieuthi.sieuthi;

import java.util.List;

public record donHangMoi(
    String tenKhachHang,
    long giamGia,
    long tienKhachDua,
    String phuongThucThanhToan,
    List<dongHangMoi> matHang,
    String soDienThoai
) {
    public record dongHangMoi(int maHang, int soLuong) {
    }
}
