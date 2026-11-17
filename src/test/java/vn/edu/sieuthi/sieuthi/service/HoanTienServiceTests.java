package vn.edu.sieuthi.sieuthi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import vn.edu.sieuthi.sieuthi.PhieuChiDaTao;
import vn.edu.sieuthi.sieuthi.PhieuChiMoi;

class HoanTienServiceTests {
    @Test
    void taoPhieuChiGanVoiDonHangVaGhiDuLieuDaLuu() {
        FakeJdbcTemplate jdbc = new FakeJdbcTemplate(true, 200);
        HoanTienService service = new HoanTienService(jdbc);

        PhieuChiDaTao ketQua = service.taoPhieuChi(new PhieuChiMoi(7, "  Khách trả hàng  ", 800L));

        assertEquals(11, ketQua.maPhieuChi());
        assertEquals(7, ketQua.maDonHang());
        assertEquals("Khách VIP", ketQua.hoTenKhach());
        assertEquals("Khách trả hàng", ketQua.lyDo());
        assertEquals(800, ketQua.soTien());
        assertEquals(1, jdbc.soLanTaoPhieu);
        assertEquals(4, jdbc.thamSoTaoPhieu.length);
        assertEquals("Khách trả hàng", jdbc.thamSoTaoPhieu[1]);
        assertEquals(800L, jdbc.thamSoTaoPhieu[3]);
    }

    @Test
    void tuChoiHoanQuaTongTienConLaiVaKhongTaoPhieu() {
        FakeJdbcTemplate jdbc = new FakeJdbcTemplate(true, 200);
        HoanTienService service = new HoanTienService(jdbc);

        IllegalArgumentException loi = assertThrows(
            IllegalArgumentException.class,
            () -> service.taoPhieuChi(new PhieuChiMoi(7, "Hoàn tiền", 801L))
        );

        assertEquals("Số tiền hoàn vượt quá số tiền còn lại của đơn hàng (800 đồng).", loi.getMessage());
        assertEquals(0, jdbc.soLanTaoPhieu);
    }

    @Test
    void tuChoiDonHangKhongTonTai() {
        FakeJdbcTemplate jdbc = new FakeJdbcTemplate(false, 0);
        HoanTienService service = new HoanTienService(jdbc);

        assertThrows(
            IllegalArgumentException.class,
            () -> service.taoPhieuChi(new PhieuChiMoi(99, "Hoàn tiền", 100L))
        );

        assertEquals(0, jdbc.soLanTaoPhieu);
    }

    private static final class FakeJdbcTemplate extends JdbcTemplate {
        private final boolean coDonHang;
        private final long daHoan;
        private int soLanTaoPhieu;
        private Object[] thamSoTaoPhieu;

        private FakeJdbcTemplate(boolean coDonHang, long daHoan) {
            this.coDonHang = coDonHang;
            this.daHoan = daHoan;
        }

        @Override
        public List<Map<String, Object>> queryForList(String sql, Object... args) {
            if (sql.contains("FROM don_hang")) {
                return coDonHang ? List.of(Map.of("ten_khach_hang", "Khách VIP", "tong_tien", 1000L)) : List.of();
            }
            throw new AssertionError("Câu truy vấn không mong đợi.");
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            if (sql.contains("SUM(so_tien)")) {
                return requiredType.cast(daHoan);
            }
            if (sql.contains("RETURNING ma_phieu_chi")) {
                thamSoTaoPhieu = args;
                soLanTaoPhieu++;
                return requiredType.cast(11);
            }
            if (sql.contains("ngay_chi::text")) {
                return requiredType.cast("2026-11-16 14:00:00+07");
            }
            throw new AssertionError("Câu truy vấn không mong đợi.");
        }
    }
}
