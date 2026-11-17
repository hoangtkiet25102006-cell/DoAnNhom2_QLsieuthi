package vn.edu.sieuthi.sieuthi.service;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.sieuthi.sieuthi.PhieuChiDaTao;
import vn.edu.sieuthi.sieuthi.PhieuChiMoi;

@Service
public class HoanTienService {
    private final JdbcTemplate jdbc;

    public HoanTienService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public PhieuChiDaTao taoPhieuChi(PhieuChiMoi phieuChi) {
        if (phieuChi == null || phieuChi.maDonHang() <= 0) {
            throw new IllegalArgumentException("Vui lòng chọn đơn hàng cần hoàn tiền.");
        }
        if (phieuChi.soTien() == null || phieuChi.soTien() <= 0) {
            throw new IllegalArgumentException("Số tiền hoàn phải lớn hơn 0.");
        }
        if (phieuChi.lyDo() == null || phieuChi.lyDo().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do hoàn tiền.");
        }
        String lyDo = phieuChi.lyDo().trim();
        if (lyDo.length() > 200) {
            throw new IllegalArgumentException("Lý do hoàn tiền không được dài quá 200 ký tự.");
        }

        List<Map<String, Object>> ketQua = jdbc.queryForList("""
            SELECT ten_khach_hang, tong_tien
            FROM don_hang
            WHERE ma_don_hang = ?
            FOR UPDATE
            """, phieuChi.maDonHang());
        if (ketQua.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng cần hoàn tiền.");
        }

        Map<String, Object> donHang = ketQua.get(0);
        long tongTienDonHang = ((Number) donHang.get("tong_tien")).longValue();
        Long daHoan = jdbc.queryForObject("""
            SELECT COALESCE(SUM(so_tien), 0)::bigint
            FROM phieu_chi
            WHERE ma_don_hang = ?
            """, Long.class, phieuChi.maDonHang());
        long soTienDaHoan = daHoan == null ? 0 : daHoan;
        if (phieuChi.soTien() > tongTienDonHang - soTienDaHoan) {
            throw new IllegalArgumentException(
                "Số tiền hoàn vượt quá số tiền còn lại của đơn hàng ("
                    + (tongTienDonHang - soTienDaHoan) + " đồng)."
            );
        }

        String tenKhach = (String) donHang.get("ten_khach_hang");
        Integer maPhieuChi = jdbc.queryForObject("""
            INSERT INTO phieu_chi (ma_don_hang, ngay_chi, ly_do, ho_ten_khach, so_tien)
            VALUES (?, CURRENT_TIMESTAMP, ?, ?, ?)
            RETURNING ma_phieu_chi
            """,
            Integer.class,
            phieuChi.maDonHang(),
            lyDo,
            tenKhach,
            phieuChi.soTien()
        );
        String ngayChi = jdbc.queryForObject(
            "SELECT ngay_chi::text FROM phieu_chi WHERE ma_phieu_chi = ?",
            String.class,
            maPhieuChi
        );
        return new PhieuChiDaTao(
            maPhieuChi,
            phieuChi.maDonHang(),
            ngayChi,
            lyDo,
            tenKhach,
            phieuChi.soTien()
        );
    }
}
