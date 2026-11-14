package vn.edu.sieuthi.sieuthi;

import java.math.BigDecimal;
import java.util.Locale;

public record TheVip(
    String maThe,
    BigDecimal tyLeGiam,
    int soLanToiDa,
    int soLanConLai,
    String trangThai,
    boolean dangHoatDong,
    boolean coTheSuDung
) {
    public TheVip(
        String maThe,
        BigDecimal tyLeGiam,
        int soLanToiDa,
        int soLanConLai,
        String trangThai
    ) {
        this(
            maThe,
            tyLeGiam,
            soLanToiDa,
            soLanConLai,
            trangThai,
            trangThaiDangHoatDong(trangThai),
            trangThaiDangHoatDong(trangThai) && soLanConLai > 0
        );
    }

    private static boolean trangThaiDangHoatDong(String trangThai) {
        if (trangThai == null) {
            return false;
        }
        String trangThaiChuanHoa = trangThai.trim().toLowerCase(Locale.ROOT);
        return trangThaiChuanHoa.equals("hoat_dong")
            || trangThaiChuanHoa.equals("hoạt động")
            || trangThaiChuanHoa.equals("đang hoạt động")
            || trangThaiChuanHoa.equals("active");
    }
}
