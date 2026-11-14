package vn.edu.sieuthi.sieuthi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import vn.edu.sieuthi.sieuthi.PhieuNhapDaTao;
import vn.edu.sieuthi.sieuthi.phieuNhapMoi;

class NhapKhoServiceTests {
    @Test
    void taoPhieuNhapTinhTienVaLuuTungMatHang() {
        FakeJdbcTemplate jdbc = new FakeJdbcTemplate();
        NhapKhoService service = new NhapKhoService(jdbc);
        phieuNhapMoi phieu = new phieuNhapMoi(
            "Nhà cung cấp A",
            "Nhập hàng tuần",
            List.of(
                new phieuNhapMoi.dongPhieuNhapMoi(1, 2, 1200L),
                new phieuNhapMoi.dongPhieuNhapMoi(2, 3, 500L)
            )
        );

        PhieuNhapDaTao ketQua = service.taoPhieuNhap(phieu);

        assertEquals(7, ketQua.maPhieuNhap());
        assertEquals(3900, ketQua.tongTien());
        assertEquals(2, ketQua.soMatHang());
        assertEquals(4, jdbc.soLanCapNhat);
        assertEquals(1, jdbc.soLanTaoPhieu);
    }

    @Test
    void tuChoiPhieuNhapKhongCoNhaCungCapVaKhongGhiDuLieu() {
        FakeJdbcTemplate jdbc = new FakeJdbcTemplate();
        NhapKhoService service = new NhapKhoService(jdbc);

        assertThrows(
            IllegalArgumentException.class,
            () -> service.taoPhieuNhap(new phieuNhapMoi(" ", "", List.of(
                new phieuNhapMoi.dongPhieuNhapMoi(1, 1, 1000L)
            )))
        );

        assertEquals(0, jdbc.soLanCapNhat);
        assertEquals(0, jdbc.soLanTaoPhieu);
    }

    @Test
    void tuChoiMatHangTrungMaTrongCungPhieu() {
        FakeJdbcTemplate jdbc = new FakeJdbcTemplate();
        NhapKhoService service = new NhapKhoService(jdbc);
        phieuNhapMoi phieu = new phieuNhapMoi("Nhà cung cấp A", "", List.of(
            new phieuNhapMoi.dongPhieuNhapMoi(1, 1, 1000L),
            new phieuNhapMoi.dongPhieuNhapMoi(1, 2, 900L)
        ));

        assertThrows(IllegalArgumentException.class, () -> service.taoPhieuNhap(phieu));

        assertEquals(0, jdbc.soLanCapNhat);
        assertEquals(0, jdbc.soLanTaoPhieu);
    }

    @Test
    void tuChoiDongPhieuNhapBiThieuSoLuongHoacGiaNhap() {
        List<phieuNhapMoi> phieuNhapThieuDuLieu = List.of(
            new phieuNhapMoi("Nhà cung cấp A", "", List.of(
                new phieuNhapMoi.dongPhieuNhapMoi(1, null, 1000L)
            )),
            new phieuNhapMoi("Nhà cung cấp A", "", List.of(
                new phieuNhapMoi.dongPhieuNhapMoi(1, 1, null)
            ))
        );

        for (phieuNhapMoi phieu : phieuNhapThieuDuLieu) {
            FakeJdbcTemplate jdbc = new FakeJdbcTemplate();
            NhapKhoService service = new NhapKhoService(jdbc);

            assertThrows(IllegalArgumentException.class, () -> service.taoPhieuNhap(phieu));
            assertEquals(0, jdbc.soLanCapNhat);
            assertEquals(0, jdbc.soLanTaoPhieu);
        }
    }

    private static final class FakeJdbcTemplate extends JdbcTemplate {
        private int soLanCapNhat;
        private int soLanTaoPhieu;

        @Override
        public List<Map<String, Object>> queryForList(String sql, Object... args) {
            return List.of(Map.of("ton_kho", 5, "ten_hang", "Mặt hàng thử"));
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            if (sql.contains("RETURNING ma_phieu_nhap")) {
                soLanTaoPhieu++;
                return requiredType.cast(7);
            }
            if (sql.contains("thoi_gian_nhap::text")) {
                return requiredType.cast("2026-11-14 11:00:00+07");
            }
            throw new AssertionError("Câu truy vấn không mong đợi.");
        }

        @Override
        public int update(String sql, Object... args) {
            soLanCapNhat++;
            return 1;
        }
    }
}
