package vn.edu.sieuthi.sieuthi.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.sieuthi.sieuthi.PhieuNhapDaTao;
import vn.edu.sieuthi.sieuthi.phieuNhapMoi;

@Service
public class NhapKhoService {
    private final JdbcTemplate jdbc;

    public NhapKhoService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public PhieuNhapDaTao taoPhieuNhap(phieuNhapMoi phieuNhap) {
        if (phieuNhap == null || phieuNhap.nhaCungCap() == null || phieuNhap.nhaCungCap().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập tên nhà cung cấp.");
        }
        if (phieuNhap.nhaCungCap().trim().length() > 150) {
            throw new IllegalArgumentException("Tên nhà cung cấp không được dài quá 150 ký tự.");
        }
        if (phieuNhap.ghiChu() != null && phieuNhap.ghiChu().length() > 2000) {
            throw new IllegalArgumentException("Ghi chú không được dài quá 2000 ký tự.");
        }
        if (phieuNhap.matHang() == null || phieuNhap.matHang().isEmpty()) {
            throw new IllegalArgumentException("Phiếu nhập cần có ít nhất một mặt hàng.");
        }

        Set<Integer> maMatHang = new HashSet<>();
        for (phieuNhapMoi.dongPhieuNhapMoi dong : phieuNhap.matHang()) {
            if (dong == null || dong.maHang() <= 0
                || dong.soLuong() == null || dong.soLuong() <= 0
                || dong.giaNhap() == null || dong.giaNhap() < 0) {
                throw new IllegalArgumentException("Số lượng phải lớn hơn 0 và giá nhập không được âm.");
            }
            if (!maMatHang.add(dong.maHang())) {
                throw new IllegalArgumentException("Một mặt hàng chỉ được xuất hiện một lần trong phiếu nhập.");
            }
        }

        long tongTien = 0;
        for (phieuNhapMoi.dongPhieuNhapMoi dong : phieuNhap.matHang().stream()
            .sorted(Comparator.comparingInt(phieuNhapMoi.dongPhieuNhapMoi::maHang))
            .toList()) {
            List<Map<String, Object>> ketQua = jdbc.queryForList("""
                SELECT ton_kho, ten_hang
                FROM mat_hang
                WHERE ma_hang = ?
                FOR UPDATE
                """, dong.maHang());
            if (ketQua.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy mặt hàng mã " + dong.maHang() + ".");
            }
            long tonSauNhap = ((Number) ketQua.get(0).get("ton_kho")).longValue() + dong.soLuong();
            if (tonSauNhap > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Số lượng tồn của mặt hàng vượt quá giới hạn cho phép.");
            }
            try {
                tongTien = Math.addExact(
                    tongTien,
                    Math.multiplyExact(dong.giaNhap(), (long) dong.soLuong())
                );
            } catch (ArithmeticException loi) {
                throw new IllegalArgumentException("Tổng tiền phiếu nhập vượt quá giới hạn cho phép.");
            }
        }

        Integer maPhieuNhap = jdbc.queryForObject("""
            INSERT INTO phieu_nhap (nha_cung_cap, tong_tien, ghi_chu)
            VALUES (?, ?, ?)
            RETURNING ma_phieu_nhap
            """,
            Integer.class,
            phieuNhap.nhaCungCap().trim(),
            tongTien,
            phieuNhap.ghiChu() == null ? "" : phieuNhap.ghiChu().trim()
        );

        for (phieuNhapMoi.dongPhieuNhapMoi dong : phieuNhap.matHang()) {
            jdbc.update("""
                UPDATE mat_hang
                SET ton_kho = ton_kho + ?, gia_nhap = ?
                WHERE ma_hang = ?
                """, dong.soLuong(), dong.giaNhap(), dong.maHang());
            jdbc.update("""
                INSERT INTO chi_tiet_phieu_nhap (ma_phieu_nhap, ma_hang, so_luong, gia_nhap)
                VALUES (?, ?, ?, ?)
                """, maPhieuNhap, dong.maHang(), dong.soLuong(), dong.giaNhap());
        }

        return new PhieuNhapDaTao(
            maPhieuNhap,
            phieuNhap.nhaCungCap().trim(),
            tongTien,
            phieuNhap.matHang().size(),
            jdbc.queryForObject(
                "SELECT thoi_gian_nhap::text FROM phieu_nhap WHERE ma_phieu_nhap = ?",
                String.class,
                maPhieuNhap
            )
        );
    }
}
