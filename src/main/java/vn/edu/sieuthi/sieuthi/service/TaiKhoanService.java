package vn.edu.sieuthi.sieuthi.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaiKhoanService implements UserDetailsService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public TaiKhoanService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String tenDangNhap) throws UsernameNotFoundException {
        List<Map<String, Object>> ketQua = jdbc.queryForList("""
            SELECT ten_dang_nhap, mat_khau_ma_hoa, vai_tro, dang_hoat_dong
            FROM tai_khoan
            WHERE LOWER(ten_dang_nhap) = LOWER(?)
            """, tenDangNhap);
        if (ketQua.isEmpty()) {
            throw new UsernameNotFoundException("Không tìm thấy tài khoản.");
        }
        Map<String, Object> taiKhoan = ketQua.get(0);
        return User.withUsername((String) taiKhoan.get("ten_dang_nhap"))
            .password((String) taiKhoan.get("mat_khau_ma_hoa"))
            .roles((String) taiKhoan.get("vai_tro"))
            .disabled(!((Boolean) taiKhoan.get("dang_hoat_dong")))
            .build();
    }

    @Transactional
    public void taoTaiKhoan(String tenDangNhap, String matKhau, String hoTen, String vaiTro) {
        String ten = tenDangNhap == null ? "" : tenDangNhap.trim().toLowerCase(Locale.ROOT);
        String tenHienThi = hoTen == null ? "" : hoTen.trim();
        if (!ten.matches("[A-Za-z0-9._-]{3,60}")) {
            throw new IllegalArgumentException("Tên đăng nhập phải từ 3 đến 60 ký tự (chữ, số, ., _, -).");
        }
        if (matKhau == null || matKhau.length() < 8 || matKhau.length() > 72) {
            throw new IllegalArgumentException("Mật khẩu phải có từ 8 đến 72 ký tự.");
        }
        if (tenHienThi.isBlank() || tenHienThi.length() > 150) {
            throw new IllegalArgumentException("Họ tên phải có từ 1 đến 150 ký tự.");
        }
        if (!"MANAGER".equals(vaiTro) && !"EMPLOYEE".equals(vaiTro)) {
            throw new IllegalArgumentException("Vai trò tài khoản không hợp lệ.");
        }
        try {
            jdbc.update("""
                INSERT INTO tai_khoan (ten_dang_nhap, mat_khau_ma_hoa, ho_ten, vai_tro)
                VALUES (?, ?, ?, ?)
                """, ten, passwordEncoder.encode(matKhau), tenHienThi, vaiTro);
        } catch (DuplicateKeyException loi) {
            throw new IllegalArgumentException("Tên đăng nhập đã được sử dụng.");
        }
    }

    public Map<String, Object> thongTin(String tenDangNhap) {
        return jdbc.queryForMap("""
            SELECT ten_dang_nhap AS "tenDangNhap", ho_ten AS "hoTen", vai_tro AS "vaiTro"
            FROM tai_khoan
            WHERE LOWER(ten_dang_nhap) = LOWER(?)
            """, tenDangNhap);
    }
}
