package vn.edu.sieuthi.sieuthi;

import java.time.LocalDate;
import java.util.List;

public record DuLieuTongQuan(
    int tongMatHang,
    int matHangSapHet,
    long giaTriTonKho,
    long doanhThuHomNay,
    long chiPhiNhapHomNay,
    long thuChiRongHomNay,
    int soDonHomNay,
    List<ThuChiTheoNgay> doanhThuTuan,
    List<PhieuNhapGanDay> phieuNhapGanDay,
    List<DonHangGanDay> donHangGanDay
) {
    public record ThuChiTheoNgay(LocalDate ngay, long doanhThu, long chiPhiNhap, long chiHoanTien) {
    }

    public record PhieuNhapGanDay(
        int maPhieuNhap,
        String nhaCungCap,
        long tongTien,
        String thoiGianNhap
    ) {
    }

    public record DonHangGanDay(
        int maDonHang,
        String tenKhachHang,
        long tongTien,
        String phuongThucThanhToan,
        String thoiGianTao
    ) {
    }
}
