package vn.edu.sieuthi.sieuthi;

import java.time.LocalDate;
import java.util.List;

public record BaoCaoThuChi(
    String loai,
    LocalDate tuNgay,
    LocalDate denNgay,
    long tongThu,
    long tongChi,
    long thuChiRong,
    List<DongBaoCao> chiTiet
) {
    public record DongBaoCao(LocalDate ky, long thu, long chi, long thuChiRong) {
    }
}
