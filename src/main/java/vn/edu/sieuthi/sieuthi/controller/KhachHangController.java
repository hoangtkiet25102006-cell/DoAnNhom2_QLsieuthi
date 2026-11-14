package vn.edu.sieuthi.sieuthi.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import vn.edu.sieuthi.sieuthi.CauHinhVip;
import vn.edu.sieuthi.sieuthi.KhachHang;
import vn.edu.sieuthi.sieuthi.KhachHangMoi;
import vn.edu.sieuthi.sieuthi.TheVip;
import vn.edu.sieuthi.sieuthi.repository.KhachHangRepository;

@RestController
public class KhachHangController {
    private final KhachHangRepository repository;

    public KhachHangController(KhachHangRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/khach-hang")
    public List<KhachHang> layDanhSach() {
        return repository.layDanhSach();
    }

    @GetMapping("/api/khach-hang/tra-cuu")
    public KhachHang traCuu(@RequestParam String soDienThoai) {
        return repository.timTheoSoDienThoai(soDienThoai.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng theo số điện thoại."));
    }

    @PostMapping("/api/khach-hang")
    public KhachHang tao(@RequestBody KhachHangMoi khachHang) {
        kiemTraKhachHang(khachHang);
        try {
            return repository.tao(khachHang);
        } catch (DuplicateKeyException loi) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Số điện thoại này đã được đăng ký.");
        }
    }

    @PostMapping("/api/khach-hang/{maKhachHang}/the-vip")
    public TheVip taoTheVip(
        @PathVariable int maKhachHang,
        @RequestBody TheVipMoi theVipMoi
    ) {
        if (maKhachHang <= 0 || theVipMoi == null || theVipMoi.soLanToiDa() == null
            || theVipMoi.soLanToiDa() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượt tối đa của thẻ phải lớn hơn 0.");
        }
        if (!repository.tonTaiKhachHang(maKhachHang)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng.");
        }
        try {
            return repository.taoTheVip(maKhachHang, theVipMoi.soLanToiDa());
        } catch (DuplicateKeyException loi) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể cấp mã thẻ VIP, vui lòng thử lại.");
        }
    }

    @PutMapping("/api/khach-hang/{maKhachHang}")
    public KhachHang capNhat(@PathVariable int maKhachHang, @RequestBody KhachHangMoi khachHang) {
        if (maKhachHang <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã khách hàng không hợp lệ.");
        }
        kiemTraKhachHang(khachHang);
        try {
            return repository.capNhat(maKhachHang, khachHang)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng."));
        } catch (DuplicateKeyException loi) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Số điện thoại này đã được đăng ký.");
        }
    }

    @PutMapping("/api/the-vip/{maThe}/trang-thai")
    public TheVip capNhatTrangThaiTheVip(
        @PathVariable String maThe,
        @RequestBody TrangThaiTheVipMoi trangThai
    ) {
        if (maThe == null || maThe.isBlank() || trangThai == null || trangThai.dangHoatDong() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái thẻ VIP không hợp lệ.");
        }
        return repository.capNhatTrangThaiTheVip(maThe, trangThai.dangHoatDong())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thẻ VIP."));
    }

    @GetMapping("/api/cai-dat/vip")
    public CauHinhVip layCauHinhVip() {
        return repository.layCauHinhVip();
    }

    @PutMapping("/api/cai-dat/vip")
    public CauHinhVip capNhatCauHinhVip(@RequestBody CauHinhVip cauHinh) {
        BigDecimal tyLe = cauHinh == null ? null : cauHinh.tyLeGiam();
        if (tyLe == null || tyLe.compareTo(BigDecimal.ZERO) < 0 || tyLe.compareTo(BigDecimal.valueOf(100)) > 0
            || tyLe.stripTrailingZeros().scale() > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tỷ lệ giảm VIP phải là số nguyên từ 0 đến 100%.");
        }
        return repository.capNhatTyLeGiam(tyLe);
    }

    public record TheVipMoi(Integer soLanToiDa) {
    }

    public record TrangThaiTheVipMoi(Boolean dangHoatDong) {
    }

    private static void kiemTraKhachHang(KhachHangMoi khachHang) {
        if (khachHang == null
            || khachHang.tenKhachHang() == null || khachHang.tenKhachHang().isBlank()
            || khachHang.soDienThoai() == null || khachHang.soDienThoai().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên khách hàng và số điện thoại là bắt buộc.");
        }
        if (khachHang.tenKhachHang().trim().length() > 150
            || khachHang.soDienThoai().trim().length() > 15) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên hoặc số điện thoại vượt quá độ dài cho phép.");
        }
    }
}
