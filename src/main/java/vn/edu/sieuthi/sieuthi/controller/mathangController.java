package vn.edu.sieuthi.sieuthi.controller;

import java.util.List;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import vn.edu.sieuthi.sieuthi.DanhSachMatHang;
import vn.edu.sieuthi.sieuthi.BaoCaoThuChi;
import vn.edu.sieuthi.sieuthi.ChiTietDonHang;
import vn.edu.sieuthi.sieuthi.ChiTietPhieuNhap;
import vn.edu.sieuthi.sieuthi.DuLieuTongQuan;
import vn.edu.sieuthi.sieuthi.DonHangDaTao;
import vn.edu.sieuthi.sieuthi.donHangMoi;
import vn.edu.sieuthi.sieuthi.mathang;
import vn.edu.sieuthi.sieuthi.mathangmoi;
import vn.edu.sieuthi.sieuthi.PhieuNhapDaTao;
import vn.edu.sieuthi.sieuthi.phieuNhapMoi;
import vn.edu.sieuthi.sieuthi.repository.matHangRepository;
import vn.edu.sieuthi.sieuthi.service.NhapKhoService;
import vn.edu.sieuthi.sieuthi.service.ThanhToanService;

@RestController 
public class mathangController {
    private final matHangRepository rePo;
    private final ThanhToanService thanhToanService;
    private final NhapKhoService nhapKhoService;

    public mathangController(
        matHangRepository rePo,
        ThanhToanService thanhToanService,
        NhapKhoService nhapKhoService
    ){
        this.rePo = rePo;
        this.thanhToanService = thanhToanService;
        this.nhapKhoService = nhapKhoService;
    }

    // Controller nhận yêu cầu HTTP và chuyển việc truy cập dữ liệu cho Repository.
    @GetMapping("/api/mat-hang")
    public List<mathang> danhsach() {
        return rePo.getAll();
    }
    @GetMapping("/api/mat-hang/{ma}")
    public ResponseEntity<mathang> chiTiet(@PathVariable int ma){
        return rePo.timTheoMa(ma).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @PostMapping("/api/mat-hang")
    public ResponseEntity<?> them(@RequestBody mathangmoi m){
        kiemTraMatHangMoi(m);
        int ma = rePo.them(m);
        mathang daTao = rePo.timTheoMa(ma).orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED).body(daTao);
    }

    @GetMapping("/api/san-pham")
    public DanhSachMatHang timKiem(
        @RequestParam(required = false) String tuKhoa,
        @RequestParam(required = false) String danhMuc,
        @RequestParam(required = false) String tinhTrang,
        @RequestParam(defaultValue = "0") int trang,
        @RequestParam(defaultValue = "10") int kichThuoc
    ) {
        if (trang < 0 || kichThuoc < 1 || kichThuoc > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trang hoặc kích thước trang không hợp lệ.");
        }
        if (!tinhTrangHopLe(tinhTrang)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tình trạng lọc không hợp lệ.");
        }
        return rePo.timKiem(tuKhoa, danhMuc, tinhTrang, trang, kichThuoc);
    }

    @GetMapping("/api/danh-muc")
    public List<String> layDanhMuc() {
        return rePo.layDanhMuc();
    }

    @GetMapping("/api/tong-quan")
    public DuLieuTongQuan layTongQuan() {
        return rePo.layTongQuan();
    }

    @GetMapping("/api/bao-cao/thu-chi")
    public BaoCaoThuChi layBaoCaoThuChi(
        @RequestParam(defaultValue = "ngay") String loai,
        @RequestParam(required = false) String ky
    ) {
        try {
            LocalDate tuNgay;
            LocalDate denNgay;
            switch (loai) {
                case "ngay" -> {
                    LocalDate ngay = ky == null ? LocalDate.now() : LocalDate.parse(ky);
                    tuNgay = ngay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    denNgay = tuNgay.plusWeeks(1);
                }
                case "thang" -> {
                    Year nam = ky == null ? Year.now() : parseNamBaoCao(ky);
                    if (nam.getValue() < 1 || nam.getValue() > 9998) {
                        throw new DateTimeException("Năm báo cáo không hợp lệ.");
                    }
                    tuNgay = nam.atDay(1);
                    denNgay = nam.plusYears(1).atDay(1);
                }
                case "nam" -> {
                    Year nam = ky == null ? Year.now() : Year.parse(ky);
                    if (nam.getValue() < 5 || nam.getValue() > 9998) {
                        throw new DateTimeException("Khoảng báo cáo 5 năm không hợp lệ.");
                    }
                    tuNgay = nam.minusYears(4).atDay(1);
                    denNgay = nam.plusYears(1).atDay(1);
                }
                default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Loại báo cáo phải là ngay, thang hoặc nam."
                );
            }
            return rePo.layBaoCaoThuChi(loai, tuNgay, denNgay);
        } catch (DateTimeException | ArithmeticException loiNgay) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Kỳ báo cáo không hợp lệ; hãy nhập đúng định dạng ngày, tháng hoặc năm."
            );
        }
    }

    private static Year parseNamBaoCao(String ky) {
        try {
            return Year.parse(ky);
        } catch (DateTimeException loiNam) {
            try {
                return Year.from(YearMonth.parse(ky));
            } catch (DateTimeException loiThang) {
                throw loiNam;
            }
        }
    }

    @PostMapping("/api/don-hang")
    public ResponseEntity<DonHangDaTao> thanhToan(@RequestBody donHangMoi donHang) {
        return ResponseEntity.status(HttpStatus.CREATED).body(thanhToanService.thanhToan(donHang));
    }

    @PostMapping("/api/phieu-nhap")
    public ResponseEntity<PhieuNhapDaTao> taoPhieuNhap(@RequestBody phieuNhapMoi phieuNhap) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nhapKhoService.taoPhieuNhap(phieuNhap));
    }

    @GetMapping("/api/don-hang/{maDonHang}")
    public ResponseEntity<ChiTietDonHang> chiTietDonHang(@PathVariable int maDonHang) {
        return rePo.chiTietDonHang(maDonHang)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/phieu-nhap/{maPhieuNhap}")
    public ResponseEntity<ChiTietPhieuNhap> chiTietPhieuNhap(@PathVariable int maPhieuNhap) {
        return rePo.chiTietPhieuNhap(maPhieuNhap)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/bao-cao/kho.csv")
    public ResponseEntity<String> xuatBaoCaoKho(
        @RequestParam(required = false) String tuKhoa,
        @RequestParam(required = false) String danhMuc,
        @RequestParam(required = false) String tinhTrang
    ) {
        if (!tinhTrangHopLe(tinhTrang)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tình trạng lọc không hợp lệ.");
        }
        StringBuilder csv = new StringBuilder("\uFEFFMã,Tên mặt hàng,Danh mục,Đơn vị,Đơn giá,Tồn kho,Tồn tối thiểu,Tình trạng\n");
        for (mathang matHang : rePo.layTonKho(tuKhoa, danhMuc, tinhTrang)) {
            String tinhTrangKho = matHang.tonKho() == 0
                ? "Hết hàng"
                : matHang.tonKho() < matHang.tonToiThieu() ? "Sắp hết" : "Còn hàng";
            csv.append(csv(matHang.maSanPham())).append(',')
                .append(csv(matHang.tenHang())).append(',')
                .append(csv(matHang.danhMuc())).append(',')
                .append(csv(matHang.donViTinh())).append(',')
                .append(matHang.donGia()).append(',')
                .append(matHang.tonKho()).append(',')
                .append(matHang.tonToiThieu()).append(',')
                .append(csv(tinhTrangKho)).append('\n');
        }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bao-cao-ton-kho.csv\"")
            .body(csv.toString());
    }

    private static void kiemTraMatHangMoi(mathangmoi m) {
        if (m == null
            || m.maSanPham() == null || m.maSanPham().isBlank()
            || m.tenHang() == null || m.tenHang().isBlank()
            || m.danhMuc() == null || m.danhMuc().isBlank()
            || m.donViTinh() == null || m.donViTinh().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Mã, tên mặt hàng, danh mục và đơn vị tính là bắt buộc."
            );
        }
        if (m.maSanPham().length() > 40
            || m.maVach() != null && m.maVach().length() > 80
            || m.tenHang().length() > 150
            || m.danhMuc().length() > 100
            || m.donViTinh().length() > 40
            || m.thuongHieu() != null && m.thuongHieu().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Một số thông tin vượt quá độ dài cho phép.");
        }
        if (m.giaNhap() < 0 || m.donGia() <= 0 || m.tonKho() < 0 || m.tonToiThieu() < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Giá nhập và số lượng không được âm; đơn giá phải lớn hơn 0."
            );
        }
    }

    private static boolean tinhTrangHopLe(String tinhTrang) {
        return tinhTrang == null || tinhTrang.isBlank()
            || tinhTrang.equals("con-hang")
            || tinhTrang.equals("sap-het")
            || tinhTrang.equals("het-hang");
    }

    private static String csv(String giaTri) {
        String anToan = giaTri == null ? "" : giaTri.replace("\"", "\"\"");
        if (!anToan.isEmpty() && "=+-@\t\r".indexOf(anToan.charAt(0)) >= 0) {
            anToan = "'" + anToan;
        }
        return "\"" + anToan + "\"";
    }
}
