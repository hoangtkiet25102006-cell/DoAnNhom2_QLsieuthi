package vn.edu.sieuthi.sieuthi;

import java.util.List;

public record ChiTietPhieuNhap(
    int maPhieuNhap,
    String nhaCungCap,
    String ghiChu,
    long tongTien,
    String thoiGianNhap,
    List<Dong> matHang
) {
    public record Dong(
        String maSanPham,
        String tenHang,
        String donViTinh,
        int soLuong,
        long giaNhap,
        long thanhTien
    ) {
    }
}
