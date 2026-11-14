package vn.edu.sieuthi.sieuthi;

import java.math.BigDecimal;

public record DonHangDaTao(
    int maDonHang,
    String tenKhachHang,
    String soDienThoai,
    String maThe,
    long tienHang,
    long giamGia,
    long giamGiaThuCong,
    long giamGiaVip,
    BigDecimal tyLeGiamVip,
    long tongTien,
    long tienKhachDua,
    long tienThua,
    String phuongThucThanhToan
) {
}
