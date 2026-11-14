package vn.edu.sieuthi.sieuthi;

import java.util.List;

public record KhachHang(
    int maKhachHang,
    String tenKhachHang,
    String soDienThoai,
    List<TheVip> theVip,
    boolean laVip
) {
    public KhachHang(int maKhachHang, String tenKhachHang, String soDienThoai, List<TheVip> theVip) {
        this(maKhachHang, tenKhachHang, soDienThoai, theVip, theVip.stream().anyMatch(TheVip::coTheSuDung));
    }
}
