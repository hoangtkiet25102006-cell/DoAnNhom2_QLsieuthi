package vn.edu.sieuthi.sieuthi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.sieuthi.sieuthi.DonHangDaTao;
import vn.edu.sieuthi.sieuthi.donHangMoi;

@Service
public class ThanhToanService {
    private static final Set<String> PHUONG_THUC_HOP_LE = Set.of("tien-mat", "the", "chuyen-khoan");

    private final JdbcTemplate jdbc;

    public ThanhToanService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public DonHangDaTao thanhToan(donHangMoi donHang) {
        if (donHang == null || donHang.matHang() == null || donHang.matHang().isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng cần có ít nhất một mặt hàng.");
        }
        if (donHang.giamGia() < 0 || donHang.tienKhachDua() < 0) {
            throw new IllegalArgumentException("Giảm giá và số tiền khách đưa không được âm.");
        }
        if (!PHUONG_THUC_HOP_LE.contains(donHang.phuongThucThanhToan())) {
            throw new IllegalArgumentException("Vui lòng chọn phương thức thanh toán hợp lệ.");
        }

        Map<Integer, Integer> soLuongTheoMatHang = new TreeMap<>();
        for (donHangMoi.dongHangMoi dong : donHang.matHang()) {
            if (dong == null || dong.maHang() <= 0 || dong.soLuong() <= 0) {
                throw new IllegalArgumentException("Mỗi mặt hàng phải có mã và số lượng lớn hơn 0.");
            }
            try {
                soLuongTheoMatHang.merge(dong.maHang(), dong.soLuong(), Math::addExact);
            } catch (ArithmeticException loi) {
                throw new IllegalArgumentException("Số lượng mặt hàng vượt quá giới hạn cho phép.");
            }
        }

        // Khóa từng dòng kho trong giao dịch để hai quầy không bán vượt tồn cùng lúc.
        long tienHang = 0;
        for (Map.Entry<Integer, Integer> dong : soLuongTheoMatHang.entrySet()) {
            List<Map<String, Object>> ketQua = jdbc.queryForList("""
                SELECT don_gia, ton_kho, ten_hang
                FROM mat_hang
                WHERE ma_hang = ? AND dang_kinh_doanh = TRUE
                FOR UPDATE
                """, dong.getKey());
            if (ketQua.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy mặt hàng mã " + dong.getKey() + ".");
            }

            Map<String, Object> matHang = ketQua.get(0);
            int tonKho = ((Number) matHang.get("ton_kho")).intValue();
            if (dong.getValue() > tonKho) {
                throw new IllegalArgumentException(
                    "Mặt hàng \"" + matHang.get("ten_hang") + "\" chỉ còn " + tonKho + " sản phẩm."
                );
            }
            long thanhTien;
            try {
                thanhTien = Math.multiplyExact(
                    ((Number) matHang.get("don_gia")).longValue(),
                    dong.getValue().longValue()
                );
                tienHang = Math.addExact(tienHang, thanhTien);
            } catch (ArithmeticException loi) {
                throw new IllegalArgumentException("Tổng tiền vượt quá giới hạn cho phép.");
            }
        }
        String soDienThoai = donHang.soDienThoai() == null || donHang.soDienThoai().isBlank()
            ? null
            : donHang.soDienThoai().trim();
        Integer maKhachHang = null;
        String maThe = null;
        String tenKhachHang = donHang.tenKhachHang() == null || donHang.tenKhachHang().isBlank()
            ? "Khách lẻ"
            : donHang.tenKhachHang().trim();
        BigDecimal tyLeGiamVip = BigDecimal.ZERO;
        if (soDienThoai != null) {
            List<Map<String, Object>> khachHang = jdbc.queryForList("""
                SELECT ma_kh, ho_ten
                FROM khach_hang
                WHERE so_dien_thoai = ?
                FOR SHARE
                """, soDienThoai);
            if (!khachHang.isEmpty()) {
                Map<String, Object> thanhVien = khachHang.get(0);
                maKhachHang = ((Number) thanhVien.get("ma_kh")).intValue();
                tenKhachHang = (String) thanhVien.get("ho_ten");
                BigDecimal mucGiamMacDinh = jdbc.queryForObject(
                    "SELECT ty_le_giam FROM cau_hinh_vip WHERE id = 1",
                    BigDecimal.class
                );
                List<Map<String, Object>> theVip = jdbc.queryForList("""
                    SELECT ma_the, COALESCE(ti_le_giam, ?) AS ti_le_giam
                    FROM the_vip
                    WHERE ma_kh = ?
                      AND so_lan_con_lai > 0
                      AND LOWER(BTRIM(trang_thai)) IN ('hoat_dong', 'hoạt động', 'đang hoạt động', 'active')
                    ORDER BY COALESCE(ti_le_giam, ?) DESC, ma_the
                    LIMIT 1
                    FOR UPDATE
                    """, mucGiamMacDinh, maKhachHang, mucGiamMacDinh);
                if (!theVip.isEmpty()) {
                    Map<String, Object> theDuocChon = theVip.get(0);
                    maThe = (String) theDuocChon.get("ma_the");
                    tyLeGiamVip = (BigDecimal) theDuocChon.get("ti_le_giam");
                    if (tyLeGiamVip.compareTo(BigDecimal.ZERO) < 0
                        || tyLeGiamVip.compareTo(BigDecimal.valueOf(100)) > 0) {
                        throw new IllegalStateException("Tỷ lệ giảm trên thẻ VIP phải từ 0 đến 100%.");
                    }
                }
            }
        }

        long giamGiaVip = maThe != null
            ? BigDecimal.valueOf(tienHang)
                .multiply(tyLeGiamVip)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .longValueExact()
            : 0;
        long giamGiaThuCong = Math.min(donHang.giamGia(), tienHang - giamGiaVip);
        long tongGiamGia = Math.addExact(giamGiaVip, giamGiaThuCong);
        long tongTien = tienHang - tongGiamGia;
        if (donHang.tienKhachDua() < tongTien) {
            throw new IllegalArgumentException(
                "Khách còn thiếu " + (tongTien - donHang.tienKhachDua()) + " đồng."
            );
        }
        long tienThua = donHang.tienKhachDua() - tongTien;

        Integer maDonHang = jdbc.queryForObject("""
            INSERT INTO don_hang (
                ten_khach_hang, ma_kh, ma_the, tien_hang, giam_gia,
                giam_gia_thu_cong, giam_gia_vip, ty_le_giam_vip, tong_tien,
                tien_khach_dua, tien_thua, phuong_thuc_thanh_toan
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING ma_don_hang
            """,
            Integer.class,
            tenKhachHang,
            maKhachHang,
            maThe,
            tienHang,
            tongGiamGia,
            giamGiaThuCong,
            giamGiaVip,
            tyLeGiamVip,
            tongTien,
            donHang.tienKhachDua(),
            tienThua,
            donHang.phuongThucThanhToan()
        );

        if (maThe != null) {
            int soLuotDaTru = jdbc.update("""
                UPDATE the_vip
                SET so_lan_con_lai = so_lan_con_lai - 1
                WHERE ma_the = ?
                  AND so_lan_con_lai > 0
                  AND LOWER(BTRIM(trang_thai)) IN ('hoat_dong', 'hoạt động', 'đang hoạt động', 'active')
                """, maThe);
            if (soLuotDaTru != 1) {
                throw new IllegalArgumentException("Thẻ VIP vừa hết lượt hoặc ngừng hoạt động. Vui lòng kiểm tra lại đơn.");
            }
        }

        for (Map.Entry<Integer, Integer> dong : soLuongTheoMatHang.entrySet()) {
            int soDongCapNhat = jdbc.update(
                "UPDATE mat_hang SET ton_kho = ton_kho - ? WHERE ma_hang = ? AND ton_kho >= ?",
                dong.getValue(),
                dong.getKey(),
                dong.getValue()
            );
            if (soDongCapNhat != 1) {
                throw new IllegalArgumentException("Tồn kho vừa thay đổi. Vui lòng tải lại đơn hàng.");
            }

            jdbc.update("""
                INSERT INTO chi_tiet_don_hang (ma_don_hang, ma_hang, so_luong, don_gia)
                SELECT ?, ma_hang, ?, don_gia FROM mat_hang WHERE ma_hang = ?
                """,
                maDonHang,
                dong.getValue(),
                dong.getKey()
            );
        }

        return new DonHangDaTao(
            maDonHang,
            tenKhachHang,
            soDienThoai,
            maThe,
            tienHang,
            tongGiamGia,
            giamGiaThuCong,
            giamGiaVip,
            tyLeGiamVip,
            tongTien,
            donHang.tienKhachDua(),
            tienThua,
            donHang.phuongThucThanhToan()
        );
    }
}
