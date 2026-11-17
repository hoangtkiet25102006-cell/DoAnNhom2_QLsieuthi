package vn.edu.sieuthi.sieuthi.config;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class TaiKhoanMacDinh implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final String tenQuanLy;
    private final String matKhauQuanLy;
    private final String tenNhanVien;
    private final String matKhauNhanVien;

    public TaiKhoanMacDinh(
        JdbcTemplate jdbc,
        PasswordEncoder passwordEncoder,
        @Value("${app.auth.manager.username:}") String tenQuanLy,
        @Value("${app.auth.manager.password:}") String matKhauQuanLy,
        @Value("${app.auth.employee.username:}") String tenNhanVien,
        @Value("${app.auth.employee.password:}") String matKhauNhanVien
    ) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.tenQuanLy = tenQuanLy;
        this.matKhauQuanLy = matKhauQuanLy;
        this.tenNhanVien = tenNhanVien;
        this.matKhauNhanVien = matKhauNhanVien;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        taoTaiKhoanNeuDuCauHinh(tenQuanLy, matKhauQuanLy, "Quản lý", "MANAGER", "APP_AUTH_MANAGER");
        taoTaiKhoanNeuDuCauHinh(tenNhanVien, matKhauNhanVien, "Nhân viên", "EMPLOYEE", "APP_AUTH_EMPLOYEE");
    }

    private void taoTaiKhoanNeuDuCauHinh(
        String tenDangNhap,
        String matKhau,
        String hoTen,
        String vaiTro,
        String tenBienMoiTruong
    ) {
        if (tenDangNhap.isBlank() && matKhau.isBlank()) return;
        if (tenDangNhap.isBlank() || matKhau.isBlank()) {
            throw new IllegalStateException("Cần cấu hình cả username và password trong " + tenBienMoiTruong + ".");
        }
        String ten = tenDangNhap.trim().toLowerCase(Locale.ROOT);
        if (!ten.matches("[A-Za-z0-9._-]{3,60}") || matKhau.length() < 8 || matKhau.length() > 72) {
            throw new IllegalStateException(
                "Tài khoản cấu hình " + tenBienMoiTruong + " không hợp lệ (username 3-60 ký tự, password 8-72 ký tự)."
            );
        }
        jdbc.update("""
            INSERT INTO tai_khoan (ten_dang_nhap, mat_khau_ma_hoa, ho_ten, vai_tro)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (ten_dang_nhap) DO NOTHING
            """, ten, passwordEncoder.encode(matKhau), hoTen, vaiTro);
    }
}
