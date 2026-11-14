package vn.edu.sieuthi.sieuthi;

import java.util.List;

public record phieuNhapMoi(
    String nhaCungCap,
    String ghiChu,
    List<dongPhieuNhapMoi> matHang
) {
    public record dongPhieuNhapMoi(int maHang, Integer soLuong, Long giaNhap) {
    }
}
