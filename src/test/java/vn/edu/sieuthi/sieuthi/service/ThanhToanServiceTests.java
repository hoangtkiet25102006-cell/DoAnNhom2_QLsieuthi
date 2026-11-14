package vn.edu.sieuthi.sieuthi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import vn.edu.sieuthi.sieuthi.DonHangDaTao;
import vn.edu.sieuthi.sieuthi.donHangMoi;

class ThanhToanServiceTests {
    @Test
    void apDungGiamVipVaGiamThuCongTrongGioiHanTienHang() {
        FakeJdbcTemplate jdbc = new FakeJdbcTemplate(true);
        ThanhToanService service = new ThanhToanService(jdbc);
        donHangMoi donHang = new donHangMoi(
            "",
            500,
            1500,
            "tien-mat",
            List.of(new donHangMoi.dongHangMoi(1, 2)),
            "0900000000"
        );

        DonHangDaTao ketQua = service.thanhToan(donHang);

        assertEquals(2000, ketQua.tienHang());
        assertEquals(200, ketQua.giamGiaVip());
        assertEquals(500, ketQua.giamGiaThuCong());
        assertEquals(700, ketQua.giamGia());
        assertEquals(1300, ketQua.tongTien());
        assertEquals(200, ketQua.tienThua());
        assertEquals("Khách VIP", ketQua.tenKhachHang());
        assertEquals("V123456789", ketQua.maThe());
        assertEquals(new BigDecimal("10.00"), ketQua.tyLeGiamVip());
        assertEquals(9, jdbc.thamSoDonHang[1]);
        assertEquals("V123456789", jdbc.thamSoDonHang[2]);
        assertEquals(200L, ((Number) jdbc.thamSoDonHang[6]).longValue());
        assertEquals(1300L, ((Number) jdbc.thamSoDonHang[8]).longValue());
        assertEquals(1, jdbc.soLanTruLuotVip);
    }

    @Test
    void gioiHanGiamThuCongNeuLonHonPhanTienHangConLaiSauGiamVip() {
        FakeJdbcTemplate jdbc = new FakeJdbcTemplate(true);
        ThanhToanService service = new ThanhToanService(jdbc);

        DonHangDaTao ketQua = service.thanhToan(new donHangMoi(
            "",
            5000,
            0,
            "tien-mat",
            List.of(new donHangMoi.dongHangMoi(1, 2)),
            "0900000000"
        ));

        assertEquals(200, ketQua.giamGiaVip());
        assertEquals(1800, ketQua.giamGiaThuCong());
        assertEquals(2000, ketQua.giamGia());
        assertEquals(0, ketQua.tongTien());
        assertEquals(1, jdbc.soLanTruLuotVip);
    }

    @Test
    void soDienThoaiChuaDangKyChiDuocApDungGiamGiaThuCong() {
        FakeJdbcTemplate jdbc = new FakeJdbcTemplate(false);
        ThanhToanService service = new ThanhToanService(jdbc);

        DonHangDaTao ketQua = service.thanhToan(new donHangMoi(
            "Khách mới",
            100,
            1900,
            "tien-mat",
            List.of(new donHangMoi.dongHangMoi(1, 2)),
            "0911111111"
        ));

        assertEquals(0, ketQua.giamGiaVip());
        assertEquals(100, ketQua.giamGiaThuCong());
        assertEquals(1900, ketQua.tongTien());
        assertEquals(0, jdbc.soLanDocCauHinhVip);
    }

    @Test
    void khachCoTheVipHetLuotKhongDuocGiamVaKhongBiTruThemLuot() {
        FakeJdbcTemplate jdbc = new FakeJdbcTemplate(true, false);
        ThanhToanService service = new ThanhToanService(jdbc);

        DonHangDaTao ketQua = service.thanhToan(new donHangMoi(
            "",
            0,
            2000,
            "tien-mat",
            List.of(new donHangMoi.dongHangMoi(1, 2)),
            "0900000000"
        ));

        assertEquals(0, ketQua.giamGiaVip());
        assertEquals(0, ketQua.giamGia());
        assertEquals("Khách VIP", ketQua.tenKhachHang());
        assertNull(ketQua.maThe());
        assertEquals(0, jdbc.soLanTruLuotVip);
    }

    private static final class FakeJdbcTemplate extends JdbcTemplate {
        private final boolean coKhachHang;
        private final boolean coTheVip;
        private Object[] thamSoDonHang;
        private int soLanDocCauHinhVip;
        private int soLanTruLuotVip;

        private FakeJdbcTemplate(boolean coTheVip) {
            this(coTheVip, coTheVip);
        }

        private FakeJdbcTemplate(boolean coKhachHang, boolean coTheVip) {
            this.coKhachHang = coKhachHang;
            this.coTheVip = coTheVip;
        }

        @Override
        public List<Map<String, Object>> queryForList(String sql, Object... args) {
            if (sql.contains("FROM khach_hang")) {
                return coKhachHang
                    ? List.of(Map.of("ma_kh", 9, "ho_ten", "Khách VIP"))
                    : List.of();
            }
            if (sql.contains("FROM the_vip")) {
                return coTheVip
                    ? List.of(Map.of("ma_the", "V123456789", "ti_le_giam", new BigDecimal("10.00")))
                    : List.of();
            }
            return List.of(Map.of("don_gia", 1000L, "ton_kho", 5, "ten_hang", "Mặt hàng thử"));
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            if (sql.contains("RETURNING ma_don_hang")) {
                thamSoDonHang = args;
                return requiredType.cast(7);
            }
            throw new AssertionError("Câu truy vấn không mong đợi.");
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType) {
            if (sql.contains("SELECT ty_le_giam")) {
                soLanDocCauHinhVip++;
                return requiredType.cast(new BigDecimal("10.00"));
            }
            throw new AssertionError("Câu truy vấn không mong đợi.");
        }

        @Override
        public int update(String sql, Object... args) {
            if (sql.contains("UPDATE the_vip")) {
                soLanTruLuotVip++;
            }
            return 1;
        }
    }
}
