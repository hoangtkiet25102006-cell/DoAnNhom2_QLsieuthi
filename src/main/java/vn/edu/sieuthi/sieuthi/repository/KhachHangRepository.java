package vn.edu.sieuthi.sieuthi.repository;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.sieuthi.sieuthi.CauHinhVip;
import vn.edu.sieuthi.sieuthi.KhachHang;
import vn.edu.sieuthi.sieuthi.KhachHangMoi;
import vn.edu.sieuthi.sieuthi.TheVip;

@Repository
public class KhachHangRepository {
    private static final SecureRandom BO_TAO_SO_NGAU_NHIEN = new SecureRandom();
    private static final char[] KY_TU_MA_THE = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private final JdbcTemplate jdbc;

    public KhachHangRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<KhachHang> layDanhSach() {
        return docDanhSach("", new Object[0]);
    }

    public Optional<KhachHang> timTheoSoDienThoai(String soDienThoai) {
        return docDanhSach("WHERE k.so_dien_thoai = ?", soDienThoai).stream().findFirst();
    }

    public boolean tonTaiKhachHang(int maKhachHang) {
        Boolean tonTai = jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM khach_hang WHERE ma_kh = ?)",
            Boolean.class,
            maKhachHang
        );
        return Boolean.TRUE.equals(tonTai);
    }

    @Transactional
    public KhachHang tao(KhachHangMoi khachHang) {
        Integer maKhachHang = jdbc.queryForObject("""
            INSERT INTO khach_hang (ho_ten, so_dien_thoai)
            VALUES (?, ?)
            RETURNING ma_kh
            """, Integer.class, khachHang.tenKhachHang().trim(), khachHang.soDienThoai().trim());
        return timTheoMaKhachHang(maKhachHang)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy khách hàng vừa tạo."));
    }

    public Optional<KhachHang> capNhat(int maKhachHang, KhachHangMoi khachHang) {
        int soDongCapNhat = jdbc.update("""
            UPDATE khach_hang
            SET ho_ten = ?, so_dien_thoai = ?
            WHERE ma_kh = ?
            """, khachHang.tenKhachHang().trim(), khachHang.soDienThoai().trim(), maKhachHang);
        return soDongCapNhat == 0 ? Optional.empty() : timTheoMaKhachHang(maKhachHang);
    }

    @Transactional
    public TheVip taoTheVip(int maKhachHang, int soLanToiDa) {
        BigDecimal tyLeGiam = layCauHinhVip().tyLeGiam();
        return jdbc.queryForObject("""
            INSERT INTO the_vip (
                ma_kh, ma_the, ti_le_giam, so_lan_toi_da, so_lan_con_lai, trang_thai
            ) VALUES (?, ?, ?, ?, ?, 'hoat_dong')
            RETURNING ma_the, ti_le_giam, so_lan_toi_da, so_lan_con_lai, trang_thai
            """, KhachHangRepository::mapTheVip,
            maKhachHang, taoMaThe(), tyLeGiam, soLanToiDa, soLanToiDa);
    }

    public Optional<TheVip> capNhatTrangThaiTheVip(String maThe, boolean dangHoatDong) {
        List<TheVip> ketQua = jdbc.query("""
            UPDATE the_vip
            SET trang_thai = ?
            WHERE ma_the = ?
            RETURNING ma_the, ti_le_giam, so_lan_toi_da, so_lan_con_lai, trang_thai
            """, KhachHangRepository::mapTheVip,
            dangHoatDong ? "hoat_dong" : "tam_khoa",
            maThe);
        return ketQua.stream().findFirst();
    }

    public CauHinhVip layCauHinhVip() {
        return jdbc.queryForObject(
            "SELECT ty_le_giam FROM cau_hinh_vip WHERE id = 1",
            (rs, row) -> new CauHinhVip(rs.getBigDecimal("ty_le_giam"))
        );
    }

    public CauHinhVip capNhatTyLeGiam(BigDecimal tyLeGiam) {
        return jdbc.queryForObject("""
            UPDATE cau_hinh_vip
            SET ty_le_giam = ?, cap_nhat_luc = CURRENT_TIMESTAMP
            WHERE id = 1
            RETURNING ty_le_giam
            """, (rs, row) -> new CauHinhVip(rs.getBigDecimal("ty_le_giam")), tyLeGiam);
    }

    public Optional<KhachHang> timTheoMaKhachHang(int maKhachHang) {
        return docDanhSach("WHERE k.ma_kh = ?", maKhachHang).stream().findFirst();
    }

    private List<KhachHang> docDanhSach(String dieuKien, Object... thamSo) {
        List<Map<String, Object>> cacDong = jdbc.queryForList("""
            SELECT k.ma_kh, k.ho_ten, k.so_dien_thoai,
                   v.ma_the, v.ti_le_giam, v.so_lan_toi_da, v.so_lan_con_lai, v.trang_thai
            FROM khach_hang k
            LEFT JOIN the_vip v ON v.ma_kh = k.ma_kh
            """ + (dieuKien.isEmpty() ? "" : dieuKien + " ") + """
            ORDER BY k.ho_ten, k.ma_kh, v.ma_the
            """, thamSo);

        Map<Integer, KhachHangTam> khachHangTheoMa = new LinkedHashMap<>();
        for (Map<String, Object> dong : cacDong) {
            int maKhachHang = ((Number) dong.get("ma_kh")).intValue();
            KhachHangTam khachHang = khachHangTheoMa.computeIfAbsent(maKhachHang, ignored ->
                new KhachHangTam(
                    maKhachHang,
                    (String) dong.get("ho_ten"),
                    (String) dong.get("so_dien_thoai")
                )
            );
            String maThe = (String) dong.get("ma_the");
            if (maThe != null) {
                khachHang.theVip.add(new TheVip(
                    maThe,
                    (BigDecimal) dong.get("ti_le_giam"),
                    ((Number) dong.get("so_lan_toi_da")).intValue(),
                    ((Number) dong.get("so_lan_con_lai")).intValue(),
                    (String) dong.get("trang_thai")
                ));
            }
        }

        return khachHangTheoMa.values().stream()
            .map(khachHang -> new KhachHang(
                khachHang.maKhachHang,
                khachHang.tenKhachHang,
                khachHang.soDienThoai,
                List.copyOf(khachHang.theVip)
            ))
            .toList();
    }

    private static TheVip mapTheVip(ResultSet rs, int row) throws SQLException {
        return new TheVip(
            rs.getString("ma_the"),
            rs.getBigDecimal("ti_le_giam"),
            rs.getInt("so_lan_toi_da"),
            rs.getInt("so_lan_con_lai"),
            rs.getString("trang_thai")
        );
    }

    private static String taoMaThe() {
        StringBuilder maThe = new StringBuilder("V");
        for (int i = 0; i < 9; i++) {
            maThe.append(KY_TU_MA_THE[BO_TAO_SO_NGAU_NHIEN.nextInt(KY_TU_MA_THE.length)]);
        }
        return maThe.toString();
    }

    private static final class KhachHangTam {
        private final int maKhachHang;
        private final String tenKhachHang;
        private final String soDienThoai;
        private final List<TheVip> theVip = new ArrayList<>();

        private KhachHangTam(int maKhachHang, String tenKhachHang, String soDienThoai) {
            this.maKhachHang = maKhachHang;
            this.tenKhachHang = tenKhachHang;
            this.soDienThoai = soDienThoai;
        }
    }
}
