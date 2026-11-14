package vn.edu.sieuthi.sieuthi;

import java.util.List;

public record DanhSachMatHang(
    List<mathang> danhSach,
    int trang,
    int kichThuoc,
    long tongSoMatHang,
    int tongSoTrang
) {
}
