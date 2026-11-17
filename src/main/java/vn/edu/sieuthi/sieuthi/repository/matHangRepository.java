package vn.edu.sieuthi.sieuthi.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import vn.edu.sieuthi.sieuthi.DanhSachMatHang;
import vn.edu.sieuthi.sieuthi.BaoCaoThuChi;
import vn.edu.sieuthi.sieuthi.ChiTietDonHang;
import vn.edu.sieuthi.sieuthi.ChiTietPhieuNhap;
import vn.edu.sieuthi.sieuthi.DuLieuTongQuan;
import vn.edu.sieuthi.sieuthi.mathang;
import vn.edu.sieuthi.sieuthi.mathangmoi;

@Repository
public class matHangRepository {
    private static final String COT_MAT_HANG = """
        ma_hang, ma_san_pham, ma_vach, ten_hang, danh_muc, don_vi_tinh, thuong_hieu,
        mo_ta, gia_nhap, don_gia, ton_kho, ton_toi_thieu, dang_kinh_doanh
        """;

    private static final RowMapper<mathang> MAPPER_MAT_HANG = (rs, row) -> taoMatHang(rs);

    private final JdbcTemplate jdbc;

    public matHangRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<mathang> getAll() {
        return jdbc.query("SELECT " + COT_MAT_HANG + " FROM mat_hang ORDER BY ma_hang", MAPPER_MAT_HANG);
    }

    public Optional<mathang> timTheoMa(int maHang) {
        List<mathang> ketQua = jdbc.query(
            "SELECT " + COT_MAT_HANG + " FROM mat_hang WHERE ma_hang = ?",
            MAPPER_MAT_HANG,
            maHang
        );
        return ketQua.stream().findFirst();
    }

    public Optional<ChiTietDonHang> chiTietDonHang(int maDonHang) {
        List<Map<String, Object>> ketQua = jdbc.queryForList("""
            SELECT ma_don_hang, ten_khach_hang, ma_kh, ma_the, tien_hang,
                   giam_gia_vip, giam_gia_thu_cong, giam_gia, ty_le_giam_vip,
                   tong_tien, tien_khach_dua, tien_thua, phuong_thuc_thanh_toan,
                   thoi_gian_tao
            FROM don_hang
            WHERE ma_don_hang = ?
            """, maDonHang);
        if (ketQua.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> donHang = ketQua.get(0);
        Number maKhachHang = (Number) donHang.get("ma_kh");
        return Optional.of(new ChiTietDonHang(
            ((Number) donHang.get("ma_don_hang")).intValue(),
            (String) donHang.get("ten_khach_hang"),
            maKhachHang == null ? null : maKhachHang.intValue(),
            (String) donHang.get("ma_the"),
            ((Number) donHang.get("tien_hang")).longValue(),
            ((Number) donHang.get("giam_gia_vip")).longValue(),
            ((Number) donHang.get("giam_gia_thu_cong")).longValue(),
            ((Number) donHang.get("giam_gia")).longValue(),
            (BigDecimal) donHang.get("ty_le_giam_vip"),
            ((Number) donHang.get("tong_tien")).longValue(),
            ((Number) donHang.get("tien_khach_dua")).longValue(),
            ((Number) donHang.get("tien_thua")).longValue(),
            (String) donHang.get("phuong_thuc_thanh_toan"),
            ((Timestamp) donHang.get("thoi_gian_tao")).toInstant().toString(),
            chiTietMatHangDonHang(maDonHang)
        ));
    }

    public Optional<ChiTietPhieuNhap> chiTietPhieuNhap(int maPhieuNhap) {
        List<Map<String, Object>> ketQua = jdbc.queryForList("""
            SELECT ma_phieu_nhap, nha_cung_cap, ghi_chu, tong_tien, thoi_gian_nhap
            FROM phieu_nhap
            WHERE ma_phieu_nhap = ?
            """, maPhieuNhap);
        if (ketQua.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> phieuNhap = ketQua.get(0);
        return Optional.of(new ChiTietPhieuNhap(
            ((Number) phieuNhap.get("ma_phieu_nhap")).intValue(),
            (String) phieuNhap.get("nha_cung_cap"),
            (String) phieuNhap.get("ghi_chu"),
            ((Number) phieuNhap.get("tong_tien")).longValue(),
            ((Timestamp) phieuNhap.get("thoi_gian_nhap")).toInstant().toString(),
            chiTietMatHangPhieuNhap(maPhieuNhap)
        ));
    }

    private List<ChiTietDonHang.Dong> chiTietMatHangDonHang(int maDonHang) {
        return jdbc.query("""
            SELECT h.ma_san_pham, h.ten_hang, h.don_vi_tinh, c.so_luong, c.don_gia,
                   c.don_gia::numeric * c.so_luong AS thanh_tien
            FROM chi_tiet_don_hang c
            JOIN mat_hang h ON h.ma_hang = c.ma_hang
            WHERE c.ma_don_hang = ?
            ORDER BY c.ma_chi_tiet
            """, (rs, row) -> new ChiTietDonHang.Dong(
                rs.getString("ma_san_pham"),
                rs.getString("ten_hang"),
                rs.getString("don_vi_tinh"),
                rs.getInt("so_luong"),
                rs.getLong("don_gia"),
                rs.getBigDecimal("thanh_tien").longValueExact()
            ), maDonHang);
    }

    private List<ChiTietPhieuNhap.Dong> chiTietMatHangPhieuNhap(int maPhieuNhap) {
        return jdbc.query("""
            SELECT h.ma_san_pham, h.ten_hang, h.don_vi_tinh, c.so_luong, c.gia_nhap,
                   c.gia_nhap::numeric * c.so_luong AS thanh_tien
            FROM chi_tiet_phieu_nhap c
            JOIN mat_hang h ON h.ma_hang = c.ma_hang
            WHERE c.ma_phieu_nhap = ?
            ORDER BY c.ma_chi_tiet
            """, (rs, row) -> new ChiTietPhieuNhap.Dong(
                rs.getString("ma_san_pham"),
                rs.getString("ten_hang"),
                rs.getString("don_vi_tinh"),
                rs.getInt("so_luong"),
                rs.getLong("gia_nhap"),
                rs.getBigDecimal("thanh_tien").longValueExact()
            ), maPhieuNhap);
    }

    public DanhSachMatHang timKiem(String tuKhoa, String danhMuc, String tinhTrang, int trang, int kichThuoc) {
        StringBuilder dieuKien = new StringBuilder(" WHERE dang_kinh_doanh = TRUE");
        List<Object> thamSo = new ArrayList<>();

        if (tuKhoa != null && !tuKhoa.isBlank()) {
            dieuKien.append(" AND (LOWER(ma_san_pham) LIKE LOWER(?) OR LOWER(COALESCE(ma_vach, '')) LIKE LOWER(?) OR LOWER(ten_hang) LIKE LOWER(?))");
            String tuKhoaTim = "%" + tuKhoa.trim() + "%";
            thamSo.add(tuKhoaTim);
            thamSo.add(tuKhoaTim);
            thamSo.add(tuKhoaTim);
        }
        if (danhMuc != null && !danhMuc.isBlank()) {
            dieuKien.append(" AND danh_muc = ?");
            thamSo.add(danhMuc);
        }
        themDieuKienTonKho(dieuKien, tinhTrang);

        Long tongSo = jdbc.queryForObject(
            "SELECT COUNT(*) FROM mat_hang" + dieuKien,
            Long.class,
            thamSo.toArray()
        );

        List<Object> thamSoTrang = new ArrayList<>(thamSo);
        thamSoTrang.add(kichThuoc);
        thamSoTrang.add(trang * kichThuoc);
        List<mathang> danhSach = jdbc.query(
            "SELECT " + COT_MAT_HANG + " FROM mat_hang" + dieuKien
                + " ORDER BY ma_hang LIMIT ? OFFSET ?",
            MAPPER_MAT_HANG,
            thamSoTrang.toArray()
        );
        long tongMatHang = tongSo == null ? 0 : tongSo;
        int tongSoTrang = (int) Math.ceil((double) tongMatHang / kichThuoc);
        return new DanhSachMatHang(danhSach, trang, kichThuoc, tongMatHang, tongSoTrang);
    }

    public List<String> layDanhMuc() {
        return jdbc.queryForList(
            "SELECT DISTINCT danh_muc FROM mat_hang WHERE dang_kinh_doanh = TRUE ORDER BY danh_muc",
            String.class
        );
    }

    public int them(mathangmoi m) {
        String sql = """
            INSERT INTO mat_hang (
                ma_san_pham, ma_vach, ten_hang, danh_muc, don_vi_tinh, thuong_hieu, mo_ta,
                gia_nhap, don_gia, ton_kho, ton_toi_thieu
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING ma_hang
            """;
        return jdbc.queryForObject(
            sql,
            Integer.class,
            m.maSanPham().trim(),
            lamNullNeuRong(m.maVach()),
            m.tenHang().trim(),
            m.danhMuc().trim(),
            m.donViTinh().trim(),
            lamRongNeuNull(m.thuongHieu()),
            lamRongNeuNull(m.moTa()),
            m.giaNhap(),
            m.donGia(),
            m.tonKho(),
            m.tonToiThieu()
        );
    }

    public DuLieuTongQuan layTongQuan() {
        var chiSo = jdbc.queryForMap("""
            SELECT
                COUNT(*) FILTER (WHERE dang_kinh_doanh = TRUE) AS tong_mat_hang,
                COUNT(*) FILTER (
                    WHERE dang_kinh_doanh = TRUE
                    AND (ton_kho = 0 OR ton_kho < ton_toi_thieu)
                ) AS mat_hang_sap_het,
                COALESCE(SUM(gia_nhap::numeric * ton_kho) FILTER (WHERE dang_kinh_doanh = TRUE), 0)
                    AS gia_tri_ton_kho
            FROM mat_hang
            """);

        var giaoDichHomNay = jdbc.queryForMap("""
            SELECT COALESCE(SUM(tong_tien), 0) AS doanh_thu, COUNT(*) AS so_don
            FROM don_hang
            WHERE thoi_gian_tao >= CURRENT_DATE
              AND thoi_gian_tao < CURRENT_DATE + INTERVAL '1 day'
            """);

        var chiPhiNhapHomNay = jdbc.queryForObject("""
            SELECT COALESCE(SUM(tong_tien), 0)
            FROM phieu_nhap
            WHERE thoi_gian_nhap >= CURRENT_DATE
              AND thoi_gian_nhap < CURRENT_DATE + INTERVAL '1 day'
            """, Long.class);

        var chiHoanTienHomNay = jdbc.queryForObject("""
            SELECT COALESCE(SUM(so_tien), 0)
            FROM phieu_chi
            WHERE ngay_chi >= CURRENT_DATE
              AND ngay_chi < CURRENT_DATE + INTERVAL '1 day'
            """, Long.class);

        List<DuLieuTongQuan.ThuChiTheoNgay> doanhThuTuan = jdbc.query("""
            SELECT ngay::date AS ngay,
                COALESCE((
                    SELECT SUM(don_hang.tong_tien)
                    FROM don_hang
                    WHERE don_hang.thoi_gian_tao >= ngay
                      AND don_hang.thoi_gian_tao < ngay + INTERVAL '1 day'
                ), 0) AS doanh_thu,
                COALESCE((
                    SELECT SUM(phieu_nhap.tong_tien)
                    FROM phieu_nhap
                    WHERE phieu_nhap.thoi_gian_nhap >= ngay
                      AND phieu_nhap.thoi_gian_nhap < ngay + INTERVAL '1 day'
                ), 0) AS chi_phi_nhap,
                COALESCE((
                    SELECT SUM(phieu_chi.so_tien)
                    FROM phieu_chi
                    WHERE phieu_chi.ngay_chi >= ngay
                      AND phieu_chi.ngay_chi < ngay + INTERVAL '1 day'
                ), 0) AS chi_hoan_tien
            FROM generate_series(CURRENT_DATE - 6, CURRENT_DATE, INTERVAL '1 day') AS cac_ngay(ngay)
            ORDER BY ngay
            """, (rs, row) -> new DuLieuTongQuan.ThuChiTheoNgay(
                rs.getDate("ngay").toLocalDate(),
                rs.getLong("doanh_thu"),
                rs.getLong("chi_phi_nhap"),
                rs.getLong("chi_hoan_tien")
            ));

        List<DuLieuTongQuan.PhieuNhapGanDay> phieuNhapGanDay = jdbc.query("""
            SELECT ma_phieu_nhap, nha_cung_cap, tong_tien, thoi_gian_nhap
            FROM phieu_nhap
            ORDER BY thoi_gian_nhap DESC
            LIMIT 5
            """, (rs, row) -> new DuLieuTongQuan.PhieuNhapGanDay(
                rs.getInt("ma_phieu_nhap"),
                rs.getString("nha_cung_cap"),
                rs.getLong("tong_tien"),
                rs.getTimestamp("thoi_gian_nhap").toInstant().toString()
            ));

        List<DuLieuTongQuan.DonHangGanDay> donHangGanDay = jdbc.query("""
            SELECT ma_don_hang, ten_khach_hang, tong_tien, phuong_thuc_thanh_toan, thoi_gian_tao
            FROM don_hang
            ORDER BY thoi_gian_tao DESC
            LIMIT 5
            """, (rs, row) -> new DuLieuTongQuan.DonHangGanDay(
                rs.getInt("ma_don_hang"),
                rs.getString("ten_khach_hang"),
                rs.getLong("tong_tien"),
                rs.getString("phuong_thuc_thanh_toan"),
                rs.getTimestamp("thoi_gian_tao").toInstant().toString()
            ));

        return new DuLieuTongQuan(
            ((Number) chiSo.get("tong_mat_hang")).intValue(),
            ((Number) chiSo.get("mat_hang_sap_het")).intValue(),
            ((Number) chiSo.get("gia_tri_ton_kho")).longValue(),
            ((Number) giaoDichHomNay.get("doanh_thu")).longValue(),
            chiPhiNhapHomNay == null ? 0 : chiPhiNhapHomNay,
            ((Number) giaoDichHomNay.get("doanh_thu")).longValue()
                - (chiPhiNhapHomNay == null ? 0 : chiPhiNhapHomNay)
                - (chiHoanTienHomNay == null ? 0 : chiHoanTienHomNay),
            ((Number) giaoDichHomNay.get("so_don")).intValue(),
            doanhThuTuan,
            phieuNhapGanDay,
            donHangGanDay
        );
    }

    public BaoCaoThuChi layBaoCaoThuChi(String loai, LocalDate tuNgay, LocalDate denNgay) {
        String taoCacKy;
        String gomTheoKyDonHang;
        String gomTheoKyPhieuNhap;
        String gomTheoKyPhieuChi;
        switch (loai) {
            case "ngay" -> {
                taoCacKy = "generate_series(?::date, ?::date - 1, INTERVAL '1 day')::date";
                gomTheoKyDonHang = "thoi_gian_tao::date";
                gomTheoKyPhieuNhap = "thoi_gian_nhap::date";
                gomTheoKyPhieuChi = "ngay_chi::date";
            }
            case "thang" -> {
                taoCacKy = "generate_series(date_trunc('month', ?::date), date_trunc('month', (?::date - 1)), INTERVAL '1 month')::date";
                gomTheoKyDonHang = "date_trunc('month', thoi_gian_tao)::date";
                gomTheoKyPhieuNhap = "date_trunc('month', thoi_gian_nhap)::date";
                gomTheoKyPhieuChi = "date_trunc('month', ngay_chi)::date";
            }
            case "nam" -> {
                taoCacKy = "generate_series(date_trunc('year', ?::date), date_trunc('year', (?::date - 1)), INTERVAL '1 year')::date";
                gomTheoKyDonHang = "date_trunc('year', thoi_gian_tao)::date";
                gomTheoKyPhieuNhap = "date_trunc('year', thoi_gian_nhap)::date";
                gomTheoKyPhieuChi = "date_trunc('year', ngay_chi)::date";
            }
            default -> throw new IllegalArgumentException("Loại báo cáo không hợp lệ.");
        }
        List<BaoCaoThuChi.DongBaoCao> chiTiet = jdbc.query("""
            WITH cac_ky AS (
                SELECT %s AS ky
            ),
            doanh_thu AS (
                SELECT %s AS ky, SUM(tong_tien) AS tong
                FROM don_hang
                WHERE thoi_gian_tao >= ? AND thoi_gian_tao < ?
                GROUP BY 1
            ),
            chi_phi_nhap AS (
                SELECT %s AS ky, SUM(tong_tien) AS tong
                FROM phieu_nhap
                WHERE thoi_gian_nhap >= ? AND thoi_gian_nhap < ?
                GROUP BY 1
            ),
            chi_hoan_tien AS (
                SELECT %s AS ky, SUM(so_tien) AS tong
                FROM phieu_chi
                WHERE ngay_chi >= ? AND ngay_chi < ?
                GROUP BY 1
            )
            SELECT cac_ky.ky,
                COALESCE(doanh_thu.tong, 0) AS thu,
                COALESCE(chi_phi_nhap.tong, 0) + COALESCE(chi_hoan_tien.tong, 0) AS chi
            FROM cac_ky
            LEFT JOIN doanh_thu USING (ky)
            LEFT JOIN chi_phi_nhap USING (ky)
            LEFT JOIN chi_hoan_tien USING (ky)
            ORDER BY cac_ky.ky
            """.formatted(taoCacKy, gomTheoKyDonHang, gomTheoKyPhieuNhap, gomTheoKyPhieuChi),
            (rs, row) -> {
                long thu = rs.getLong("thu");
                long chi = rs.getLong("chi");
                return new BaoCaoThuChi.DongBaoCao(
                    rs.getDate("ky").toLocalDate(),
                    thu,
                    chi,
                    thu - chi
                );
            },
            tuNgay,
            denNgay,
            tuNgay,
            denNgay,
            tuNgay,
            denNgay,
            tuNgay,
            denNgay
        );
        long tongThu = chiTiet.stream().mapToLong(BaoCaoThuChi.DongBaoCao::thu).sum();
        long tongChi = chiTiet.stream().mapToLong(BaoCaoThuChi.DongBaoCao::chi).sum();
        return new BaoCaoThuChi(loai, tuNgay, denNgay.minusDays(1), tongThu, tongChi, tongThu - tongChi, chiTiet);
    }

    public List<mathang> layTonKho(String tuKhoa, String danhMuc, String tinhTrang) {
        StringBuilder dieuKien = new StringBuilder(" WHERE dang_kinh_doanh = TRUE");
        List<Object> thamSo = new ArrayList<>();
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            dieuKien.append(" AND (LOWER(ma_san_pham) LIKE LOWER(?) OR LOWER(COALESCE(ma_vach, '')) LIKE LOWER(?) OR LOWER(ten_hang) LIKE LOWER(?))");
            String tuKhoaTim = "%" + tuKhoa.trim() + "%";
            thamSo.add(tuKhoaTim);
            thamSo.add(tuKhoaTim);
            thamSo.add(tuKhoaTim);
        }
        if (danhMuc != null && !danhMuc.isBlank()) {
            dieuKien.append(" AND danh_muc = ?");
            thamSo.add(danhMuc);
        }
        themDieuKienTonKho(dieuKien, tinhTrang);
        return jdbc.query(
            "SELECT " + COT_MAT_HANG + " FROM mat_hang" + dieuKien + " ORDER BY ma_hang",
            MAPPER_MAT_HANG,
            thamSo.toArray()
        );
    }

    private static void themDieuKienTonKho(StringBuilder dieuKien, String tinhTrang) {
        if ("het-hang".equals(tinhTrang)) {
            dieuKien.append(" AND ton_kho = 0");
        } else if ("sap-het".equals(tinhTrang)) {
            dieuKien.append(" AND ton_kho > 0 AND ton_kho < ton_toi_thieu");
        } else if ("con-hang".equals(tinhTrang)) {
            dieuKien.append(" AND ton_kho >= ton_toi_thieu AND ton_kho > 0");
        }
    }

    private static String lamRongNeuNull(String giaTri) {
        return giaTri == null ? "" : giaTri.trim();
    }

    private static String lamNullNeuRong(String giaTri) {
        String ketQua = lamRongNeuNull(giaTri);
        return ketQua.isEmpty() ? null : ketQua;
    }

    private static mathang taoMatHang(ResultSet rs) throws SQLException {
        return new mathang(
            rs.getInt("ma_hang"),
            rs.getString("ma_san_pham"),
            rs.getString("ma_vach"),
            rs.getString("ten_hang"),
            rs.getString("danh_muc"),
            rs.getString("don_vi_tinh"),
            rs.getString("thuong_hieu"),
            rs.getString("mo_ta"),
            rs.getLong("gia_nhap"),
            rs.getLong("don_gia"),
            rs.getInt("ton_kho"),
            rs.getInt("ton_toi_thieu"),
            rs.getBoolean("dang_kinh_doanh")
        );
    }
}
