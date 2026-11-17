package vn.edu.sieuthi.sieuthi.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import vn.edu.sieuthi.sieuthi.service.TaiKhoanService;

@RestController
public class TaiKhoanController {
    private final TaiKhoanService taiKhoanService;

    public TaiKhoanController(TaiKhoanService taiKhoanService) {
        this.taiKhoanService = taiKhoanService;
    }

    @GetMapping("/api/auth/csrf")
    public Map<String, String> layTokenCsrf(CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken());
    }

    @GetMapping("/api/auth/me")
    public Map<String, Object> taiKhoanHienTai(Authentication authentication) {
        return taiKhoanService.thongTin(authentication.getName());
    }

    @PostMapping("/api/auth/accounts")
    public Map<String, String> taoTaiKhoan(@RequestBody TaiKhoanMoi taiKhoan) {
        if (taiKhoan == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thông tin tài khoản không hợp lệ.");
        }
        taiKhoanService.taoTaiKhoan(
            taiKhoan.tenDangNhap(),
            taiKhoan.matKhau(),
            taiKhoan.hoTen(),
            taiKhoan.vaiTro()
        );
        return Map.of("message", "Đã tạo tài khoản " + taiKhoan.tenDangNhap().trim() + ".");
    }

    public record TaiKhoanMoi(String tenDangNhap, String matKhau, String hoTen, String vaiTro) {
    }
}
