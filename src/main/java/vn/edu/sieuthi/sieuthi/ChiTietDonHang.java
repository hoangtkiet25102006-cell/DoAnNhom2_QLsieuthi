package vn.edu.sieuthi.sieuthi;

import java.math.BigDecimal;
import java.util.List;

public record ChiTietDonHang(
    int maDonHang,
    String tenKhachHang,
    Integer maKhachHang,
    String maThe,
    long tienHang,
    long giamGiaVip,
    long giamGiaThuCong,
    long giamGia,
    BigDecimal tyLeGiamVip,
    long tongTien,
    long tienKhachDua,
    long tienThua,
    String phuongThucThanhToan,
    String thoiGianTao,
    List<Dong> matHang
) {
    public record Dong(
        String maSanPham,
        String tenHang,
        String donViTinh,
        int soLuong,
        long donGia,
        long thanhTien
    ) {
    }
}
