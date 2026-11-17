const state = {
    trangMatHang: 0,
    kichThuocTrang: 10,
    gioHang: new Map(),
    sanPhamBanHang: [],
    dongPhieuNhap: new Map(),
    sanPhamNhapKho: [],
    khachHang: [],
    tyLeGiamVip: 10,
    hoaDonHienTai: null,
    giaoDichHienTai: null,
    taiKhoan: null
};

const dinhDangTien = soTien => `${Number(soTien).toLocaleString('vi-VN')} đ`;
const layTrangThai = matHang => {
    if (matHang.tonKho <= 0) return { ma: 'het-hang', ten: 'Hết hàng', lop: 'out' };
    if (matHang.tonKho < matHang.tonToiThieu) return { ma: 'sap-het', ten: 'Sắp hết', lop: 'low' };
    return { ma: 'con-hang', ten: 'Còn hàng', lop: 'good' };
};

// Một hàm dùng chung để đọc JSON và báo lỗi API rõ ràng cho từng màn hình.
async function goiApi(url, tuyChon = {}) {
    if (location.protocol === 'file:') {
        throw new Error('Bạn đang mở file HTML trực tiếp nên ứng dụng không thể lưu dữ liệu. Hãy chạy ./mvnw spring-boot:run rồi mở http://localhost:8080.');
    }
    const tuyChonApi = { ...tuyChon };
    const phuongThuc = (tuyChonApi.method || 'GET').toUpperCase();
    if (!['GET', 'HEAD', 'OPTIONS'].includes(phuongThuc)) {
        const token = document.cookie.split('; ')
            .find(cookie => cookie.startsWith('XSRF-TOKEN='))
            ?.slice('XSRF-TOKEN='.length);
        tuyChonApi.headers = {
            ...tuyChonApi.headers,
            ...(token ? { 'X-XSRF-TOKEN': decodeURIComponent(token) } : {})
        };
    }
    let phanHoi;
    try {
        phanHoi = await fetch(url, tuyChonApi);
    } catch (loi) {
        throw new Error('Không kết nối được máy chủ. Hãy chạy ứng dụng Spring Boot và mở trang tại http://localhost:8080.');
    }
    if (!phanHoi.ok) {
        const laJson = phanHoi.headers.get('content-type')?.includes('json');
        const noiDung = laJson ? await phanHoi.json().catch(() => null) : null;
        if (phanHoi.status === 404 && !laJson) {
            throw new Error('Không tìm thấy API. Hãy chạy ứng dụng bằng ./mvnw spring-boot:run và mở http://localhost:8080, không mở file HTML hoặc máy chủ tĩnh.');
        }
        const loiApi = new Error(noiDung?.detail || noiDung?.message || noiDung?.error || `Lỗi máy chủ (${phanHoi.status}).`);
        loiApi.status = phanHoi.status;
        throw loiApi;
    }
    if (phanHoi.status === 204) return null;
    return phanHoi.json();
}

function hienThiLoi(id, loi) {
    const phanTu = document.getElementById(id);
    phanTu.className = 'form-message error';
    phanTu.textContent = loi.message;
}

function taoTheTrangThai(matHang) {
    const trangThai = layTrangThai(matHang);
    const the = document.createElement('span');
    the.className = `badge ${trangThai.lop}`;
    the.textContent = trangThai.ten;
    return the;
}

function chuyenManHinh(tenManHinh) {
    if (!state.taiKhoan) return;
    if (state.taiKhoan.vaiTro !== 'MANAGER' && ['tong-quan', 'mat-hang', 'kho'].includes(tenManHinh)) {
        tenManHinh = 'ban-hang';
    }
    document.querySelectorAll('.view').forEach(view => view.classList.toggle('active', view.id === `view-${tenManHinh}`));
    document.querySelectorAll('.nav-button').forEach(button => button.classList.toggle('active', button.dataset.view === tenManHinh));
    if (tenManHinh === 'tong-quan') taiTongQuan();
    if (tenManHinh === 'ban-hang') Promise.all([taiSanPhamBanHang(), taiKhachHang()]);
    if (tenManHinh === 'nhap-kho') taiMatHangNhapKho();
    if (tenManHinh === 'mat-hang') taiDanhSachMatHang();
    if (tenManHinh === 'kho') taiTonKho();
    if (tenManHinh === 'khach-hang') {
        taiKhachHang();
        if (state.taiKhoan.vaiTro === 'MANAGER') taiCauHinhVip();
    }
}

function apDungQuyen(taiKhoan) {
    state.taiKhoan = taiKhoan;
    const laQuanLy = taiKhoan.vaiTro === 'MANAGER';
    document.querySelectorAll('[data-manager-only]').forEach(phanTu => {
        phanTu.hidden = !laQuanLy;
        phanTu.setAttribute('aria-hidden', String(!laQuanLy));
    });
    document.getElementById('taiKhoanHienTai').textContent =
        `${taiKhoan.hoTen} · ${laQuanLy ? 'Quản lý' : 'Nhân viên'}`;
    document.querySelector('.account-bar').hidden = false;
    document.body.classList.remove('auth-loading');
    chuyenManHinh(laQuanLy ? 'tong-quan' : 'ban-hang');
}

async function dangXuat() {
    try {
        const { token } = await goiApi('/api/auth/csrf');
        document.getElementById('tokenDangXuat').value = token;
    } catch (loi) {
        const thongBao = document.getElementById('thongBaoThanhToan');
        thongBao.textContent = `Không đăng xuất được: ${loi.message}`;
        thongBao.className = 'form-message error';
        return;
    }
    document.getElementById('dangXuat').disabled = true;
}

function taoThamSoLoc(tuKhoaId, danhMucId, tinhTrangId) {
    const thamSo = new URLSearchParams();
    const tuKhoa = document.getElementById(tuKhoaId)?.value.trim();
    const danhMuc = document.getElementById(danhMucId)?.value;
    const tinhTrang = tinhTrangId ? document.getElementById(tinhTrangId).value : '';
    if (tuKhoa) thamSo.set('tuKhoa', tuKhoa);
    if (danhMuc) thamSo.set('danhMuc', danhMuc);
    if (tinhTrang) thamSo.set('tinhTrang', tinhTrang);
    return thamSo;
}

function capNhatTruongKyBaoCao() {
    const loai = document.getElementById('loaiBaoCao').value;
    document.getElementById('nhomKyNgay').hidden = loai !== 'ngay';
    document.getElementById('nhomKyThang').hidden = loai !== 'thang';
    document.getElementById('nhomKyNam').hidden = loai !== 'nam';
}

async function taiBaoCaoThuChi() {
    const loai = document.getElementById('loaiBaoCao').value;
    const idKy = { ngay: 'kyBaoCaoNgay', thang: 'kyBaoCaoThang', nam: 'kyBaoCaoNam' }[loai];
    const ky = document.getElementById(idKy).value;
    const thongBao = document.getElementById('thongBaoBaoCaoThuChi');
    thongBao.className = 'form-message';
    thongBao.textContent = 'Đang tải báo cáo...';
    try {
        const thamSo = new URLSearchParams({ loai, ky });
        const baoCao = await goiApi(`/api/bao-cao/thu-chi?${thamSo}`);
        document.getElementById('baoCaoTongThu').textContent = dinhDangTien(baoCao.tongThu);
        document.getElementById('baoCaoTongChi').textContent = dinhDangTien(baoCao.tongChi);
        document.getElementById('baoCaoThuChiRong').textContent = dinhDangTien(baoCao.thuChiRong);

        const dinhDangNgay = ngay => new Date(`${ngay}T00:00:00`).toLocaleDateString('vi-VN');
        const tuNgay = new Date(`${baoCao.tuNgay}T00:00:00`);
        const denNgay = new Date(`${baoCao.denNgay}T00:00:00`);
        const moTaKy = loai === 'ngay'
            ? `Tuần ${dinhDangNgay(baoCao.tuNgay)} – ${dinhDangNgay(baoCao.denNgay)}`
            : loai === 'thang'
                ? `12 tháng năm ${ky}`
                : `5 năm ${tuNgay.getFullYear()} – ${denNgay.getFullYear()}`;
        document.getElementById('kyBaoCaoHienThi').textContent = `Kỳ báo cáo: ${moTaKy}`;

        const bieuDo = document.getElementById('bieuDoDoanhThu');
        bieuDo.replaceChildren();
        const mucLonNhat = Math.max(
            1,
            ...baoCao.chiTiet.flatMap(muc => [muc.thu, muc.chi])
        );
        for (const muc of baoCao.chiTiet) {
            const ngayKy = new Date(`${muc.ky}T00:00:00`);
            const nhanKy = loai === 'nam'
                ? String(ngayKy.getFullYear())
                : loai === 'thang'
                    ? ngayKy.toLocaleDateString('vi-VN', { month: 'short' }).replace('.', '')
                    : ngayKy.toLocaleDateString('vi-VN', { day: '2-digit' });
            const cot = document.createElement('div');
            cot.className = 'chart-day';
            const moTaCot = loai === 'ngay'
                ? ngayKy.toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit' })
                : nhanKy;
            cot.setAttribute('aria-label', `${moTaCot}: thu ${dinhDangTien(muc.thu)}, chi ${dinhDangTien(muc.chi)}`);
            const capCot = document.createElement('div');
            capCot.className = 'chart-pair';
            for (const [loaiGiaoDich, giaTri] of [['income', muc.thu], ['expense', muc.chi]]) {
                const thanh = document.createElement('span');
                thanh.className = `chart-bar ${loaiGiaoDich === 'expense' ? 'expense' : ''}`;
                thanh.style.height = giaTri > 0 ? `${Math.max(4, giaTri / mucLonNhat * 132)}px` : '0';
                thanh.title = `${loaiGiaoDich === 'income' ? 'Thu' : 'Chi'}: ${dinhDangTien(giaTri)}`;
                capCot.append(thanh);
            }
            const nhan = document.createElement('span');
            nhan.className = 'chart-label';
            nhan.textContent = nhanKy;
            nhan.title = moTaCot;
            cot.append(capCot, nhan);
            bieuDo.append(cot);
        }
        const coGiaoDich = baoCao.tongThu > 0 || baoCao.tongChi > 0;
        thongBao.textContent = coGiaoDich ? '' : 'Kỳ này chưa có giao dịch thu hoặc chi.';
    } catch (loi) {
        thongBao.className = 'form-message error';
        thongBao.textContent = `Không tải được báo cáo: ${loi.message}`;
        document.getElementById('bieuDoDoanhThu').replaceChildren();
        document.getElementById('kyBaoCaoHienThi').textContent = '';
        for (const id of ['baoCaoTongThu', 'baoCaoTongChi', 'baoCaoThuChiRong']) {
            document.getElementById(id).textContent = '—';
        }
    }
}

async function taiDanhMuc() {
    const danhSach = await goiApi('/api/danh-muc');
    for (const id of ['locDanhMucBanHang', 'locDanhMucMatHang', 'locDanhMucKho']) {
        const select = document.getElementById(id);
        const luaChonCu = select.value;
        select.options.length = 1;
        for (const danhMuc of danhSach) {
            const option = document.createElement('option');
            option.value = danhMuc;
            option.textContent = danhMuc;
            select.append(option);
        }
        select.value = luaChonCu;
    }
    const danhMucForm = document.querySelector('[name="danhMuc"]');
    danhMucForm.options.length = 1;
    for (const danhMuc of danhSach) {
        const option = document.createElement('option');
        option.value = danhMuc;
        option.textContent = danhMuc;
        danhMucForm.append(option);
    }
    if (!danhSach.includes('Khác')) {
        const option = document.createElement('option');
        option.value = 'Khác';
        option.textContent = 'Khác';
        danhMucForm.append(option);
    }
}

async function taiKhachHang() {
    const thongBao = document.getElementById('thongBaoKhachHang');
    try {
        state.khachHang = await goiApi('/api/khach-hang');
        const luaChonSoDienThoai = document.getElementById('danhSachSoDienThoai');
        luaChonSoDienThoai.replaceChildren();
        const luaChonKhachHangCapThe = document.getElementById('khachHangCapTheVip');
        luaChonKhachHangCapThe.replaceChildren();
        const luaChonMacDinh = document.createElement('option');
        luaChonMacDinh.value = '';
        luaChonMacDinh.textContent = 'Chọn khách hàng';
        luaChonKhachHangCapThe.append(luaChonMacDinh);
        const bang = document.getElementById('bangKhachHang');
        bang.replaceChildren();
        for (const khach of state.khachHang) {
            if (khach.soDienThoai) {
                const option = document.createElement('option');
                option.value = khach.soDienThoai;
                option.label = `${khach.tenKhachHang}${khach.laVip ? ' · VIP' : ''}`;
                luaChonSoDienThoai.append(option);
            }
            const optionCapThe = document.createElement('option');
            optionCapThe.value = khach.maKhachHang;
            optionCapThe.textContent = `${khach.tenKhachHang}${khach.soDienThoai ? ` · ${khach.soDienThoai}` : ''}`;
            luaChonKhachHangCapThe.append(optionCapThe);

            const dong = document.createElement('tr');
            for (const giaTri of [khach.tenKhachHang, khach.soDienThoai || '—']) {
                const o = document.createElement('td');
                o.textContent = giaTri;
                dong.append(o);
            }
            const oTheVip = document.createElement('td');
            if (khach.theVip.length === 0) {
                oTheVip.textContent = 'Chưa có thẻ';
            }
            for (const the of khach.theVip) {
                const dongThe = document.createElement('div');
                dongThe.className = 'vip-card-line';
                const moTaThe = document.createElement('span');
                const tyLe = the.tyLeGiam == null ? state.tyLeGiamVip : Number(the.tyLeGiam);
                moTaThe.textContent = `${the.maThe} · ${tyLe}% · ${the.soLanConLai}/${the.soLanToiDa} lượt · ${the.dangHoatDong ? 'Đang hoạt động' : 'Đang khóa'}`;
                const nutTrangThai = document.createElement('button');
                nutTrangThai.type = 'button';
                nutTrangThai.className = 'text-button customer-action';
                nutTrangThai.dataset.action = 'card-status';
                nutTrangThai.dataset.cardId = the.maThe;
                nutTrangThai.dataset.active = String(the.dangHoatDong);
                nutTrangThai.textContent = the.dangHoatDong ? 'Khóa thẻ' : 'Mở thẻ';
                dongThe.append(moTaThe, nutTrangThai);
                oTheVip.append(dongThe);
            }
            dong.append(oTheVip);
            const thaoTac = document.createElement('td');
            const nutSua = document.createElement('button');
            nutSua.type = 'button';
            nutSua.className = 'text-button customer-action';
            nutSua.dataset.action = 'edit';
            nutSua.dataset.customerId = khach.maKhachHang;
            nutSua.textContent = 'Sửa';
            thaoTac.append(nutSua);
            dong.append(thaoTac);
            bang.append(dong);
        }
        document.getElementById('khachHangTrong').hidden = state.khachHang.length > 0;
        thongBao.textContent = '';
        capNhatThanhToan();
    } catch (loi) {
        thongBao.className = 'form-message error';
        thongBao.textContent = `Không tải được khách hàng: ${loi.message}`;
    }
}

async function taiCauHinhVip() {
    const thongBao = document.getElementById('thongBaoCauHinhVip');
    try {
        const cauHinh = await goiApi('/api/cai-dat/vip');
        state.tyLeGiamVip = Number(cauHinh.tyLeGiam);
        document.getElementById('tyLeGiamVip').value = state.tyLeGiamVip;
        capNhatThanhToan();
    } catch (loi) {
        thongBao.className = 'form-message error';
        thongBao.textContent = `Không tải được cấu hình VIP: ${loi.message}`;
    }
}

function batDauSuaKhachHang(khach) {
    document.getElementById('maKhachHangForm').value = khach.maKhachHang;
    document.getElementById('tenKhachHangMoi').value = khach.tenKhachHang;
    document.getElementById('soDienThoaiMoi').value = khach.soDienThoai || '';
    document.getElementById('nutLuuKhachHang').textContent = 'Cập nhật khách hàng';
    document.getElementById('huySuaKhachHang').hidden = false;
    document.getElementById('tenKhachHangMoi').focus();
}

function ketThucSuaKhachHang() {
    document.getElementById('formKhachHang').reset();
    document.getElementById('maKhachHangForm').value = '';
    document.getElementById('nutLuuKhachHang').textContent = 'Lưu khách hàng';
    document.getElementById('huySuaKhachHang').hidden = true;
}

async function luuKhachHang(suKien) {
    suKien.preventDefault();
    const thongBao = document.getElementById('thongBaoKhachHang');
    const maKhachHang = document.getElementById('maKhachHangForm').value;
    const khachHang = {
        tenKhachHang: document.getElementById('tenKhachHangMoi').value.trim(),
        soDienThoai: document.getElementById('soDienThoaiMoi').value.trim()
    };
    thongBao.className = 'form-message';
    thongBao.textContent = maKhachHang ? 'Đang cập nhật khách hàng...' : 'Đang đăng ký khách hàng...';
    try {
        const daLuu = await goiApi(maKhachHang ? `/api/khach-hang/${maKhachHang}` : '/api/khach-hang', {
            method: maKhachHang ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(khachHang)
        });
        ketThucSuaKhachHang();
        await taiKhachHang();
        thongBao.className = 'form-message success';
        thongBao.textContent = `Đã lưu khách hàng ${daLuu.tenKhachHang}.`;
    } catch (loi) {
        thongBao.className = 'form-message error';
        thongBao.textContent = `Không lưu được khách hàng: ${loi.message}`;
    }
}

async function capTheVip(suKien) {
    suKien.preventDefault();
    const thongBao = document.getElementById('thongBaoCapTheVip');
    const maKhachHang = document.getElementById('khachHangCapTheVip').value;
    const soLanToiDa = Number(document.getElementById('soLanToiDaVip').value);
    thongBao.className = 'form-message';
    thongBao.textContent = 'Đang cấp thẻ VIP...';
    try {
        const the = await goiApi(`/api/khach-hang/${maKhachHang}/the-vip`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ soLanToiDa })
        });
        document.getElementById('formCapTheVip').reset();
        thongBao.className = 'form-message success';
        thongBao.textContent = `Đã cấp thẻ ${the.maThe} cho khách hàng.`;
        await taiKhachHang();
    } catch (loi) {
        thongBao.className = 'form-message error';
        thongBao.textContent = `Không cấp được thẻ VIP: ${loi.message}`;
    }
}

async function luuCauHinhVip(suKien) {
    suKien.preventDefault();
    const thongBao = document.getElementById('thongBaoCauHinhVip');
    thongBao.className = 'form-message';
    thongBao.textContent = 'Đang lưu cấu hình...';
    try {
        const cauHinh = await goiApi('/api/cai-dat/vip', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ tyLeGiam: Number(document.getElementById('tyLeGiamVip').value) })
        });
        state.tyLeGiamVip = Number(cauHinh.tyLeGiam);
        document.getElementById('tyLeGiamVip').value = state.tyLeGiamVip;
        thongBao.className = 'form-message success';
        thongBao.textContent = `Đã lưu mức giảm mặc định ${state.tyLeGiamVip}% cho thẻ mới.`;
        capNhatThanhToan();
    } catch (loi) {
        thongBao.className = 'form-message error';
        thongBao.textContent = `Không lưu được cấu hình: ${loi.message}`;
    }
}

async function taiTongQuan() {
    const thongBao = document.getElementById('danhSachSapHet');
    try {
        const tongQuan = await goiApi('/api/tong-quan');
        document.getElementById('doanhThuHomNay').textContent = dinhDangTien(tongQuan.doanhThuHomNay);
        document.getElementById('chiPhiNhapHomNay').textContent = dinhDangTien(tongQuan.chiPhiNhapHomNay);
        document.getElementById('thuChiRongHomNay').textContent = dinhDangTien(tongQuan.thuChiRongHomNay);
        document.getElementById('soDonHomNay').textContent = tongQuan.soDonHomNay;
        document.getElementById('tongMatHang').textContent = tongQuan.tongMatHang;
        document.getElementById('matHangSapHet').textContent = tongQuan.matHangSapHet;
        document.getElementById('tongMatHangKho').textContent = tongQuan.tongMatHang;
        document.getElementById('matHangSapHetKho').textContent = tongQuan.matHangSapHet;
        document.getElementById('giaTriTonKho').textContent = dinhDangTien(tongQuan.giaTriTonKho);

        const [sapHet, hetHang] = await Promise.all([
            goiApi('/api/san-pham?tinhTrang=sap-het&kichThuoc=50'),
            goiApi('/api/san-pham?tinhTrang=het-hang&kichThuoc=50')
        ]);
        thongBao.replaceChildren();
        for (const matHang of [...hetHang.danhSach, ...sapHet.danhSach].slice(0, 6)) {
            const dong = document.createElement('div');
            dong.className = 'simple-list-row';
            const ten = document.createElement('span');
            ten.textContent = `${matHang.maSanPham} · ${matHang.tenHang}`;
            const ton = document.createElement('strong');
            ton.textContent = `${matHang.tonKho} ${matHang.donViTinh}`;
            dong.append(ten, ton);
            thongBao.append(dong);
        }
        if (!thongBao.childElementCount) thongBao.textContent = 'Không có mặt hàng sắp hết.';

        const bangDon = document.getElementById('donHangGanDay');
        bangDon.replaceChildren();
        for (const don of tongQuan.donHangGanDay) {
            const dong = document.createElement('tr');
            for (const noiDung of [
                `#${don.maDonHang}`,
                don.tenKhachHang,
                new Date(don.thoiGianTao).toLocaleString('vi-VN'),
                don.phuongThucThanhToan,
                dinhDangTien(don.tongTien)
            ]) {
                const o = document.createElement('td');
                o.textContent = noiDung;
                dong.append(o);
            }
            const oChiTiet = document.createElement('td');
            oChiTiet.append(taoNutXemChiTiet('don-hang', don.maDonHang));
            dong.append(oChiTiet);
            bangDon.append(dong);
        }
        document.getElementById('thongBaoDonGanDay').textContent = tongQuan.donHangGanDay.length ? '' : 'Chưa có đơn hàng.';

        const bangPhieuNhap = document.getElementById('phieuNhapGanDay');
        bangPhieuNhap.replaceChildren();
        for (const phieu of tongQuan.phieuNhapGanDay) {
            const dong = document.createElement('tr');
            for (const noiDung of [
                `#${phieu.maPhieuNhap}`,
                phieu.nhaCungCap,
                new Date(phieu.thoiGianNhap).toLocaleString('vi-VN'),
                dinhDangTien(phieu.tongTien)
            ]) {
                const o = document.createElement('td');
                o.textContent = noiDung;
                dong.append(o);
            }
            const oChiTiet = document.createElement('td');
            oChiTiet.append(taoNutXemChiTiet('phieu-nhap', phieu.maPhieuNhap));
            dong.append(oChiTiet);
            bangPhieuNhap.append(dong);
        }
        document.getElementById('thongBaoPhieuNhapGanDay').textContent =
            tongQuan.phieuNhapGanDay.length ? '' : 'Chưa có phiếu nhập.';
    } catch (loi) {
        for (const id of ['doanhThuHomNay', 'chiPhiNhapHomNay', 'thuChiRongHomNay', 'soDonHomNay', 'tongMatHang', 'matHangSapHet']) {
            document.getElementById(id).textContent = '—';
        }
        thongBao.textContent = `Không tải được dữ liệu tổng quan: ${loi.message}`;
        thongBao.replaceChildren(Object.assign(document.createElement('p'), {
            textContent: `Không tải được dữ liệu tổng quan: ${loi.message}`
        }));
        document.getElementById('donHangGanDay').replaceChildren();
        document.getElementById('thongBaoDonGanDay').textContent = 'Không tải được đơn hàng gần đây.';
        document.getElementById('phieuNhapGanDay').replaceChildren();
        document.getElementById('thongBaoPhieuNhapGanDay').textContent = 'Không tải được phiếu nhập gần đây.';
        for (const id of ['tongMatHangKho', 'matHangSapHetKho', 'giaTriTonKho']) {
            document.getElementById(id).textContent = '—';
        }
    }
}

function taoNutXemChiTiet(loai, ma) {
    const nut = document.createElement('button');
    nut.type = 'button';
    nut.className = 'text-button giao-dich-chi-tiet';
    nut.dataset.type = loai;
    nut.dataset.id = ma;
    nut.textContent = 'Xem chi tiết';
    return nut;
}

function taoKhoiThongTin(cacTruong) {
    const luoi = document.createElement('div');
    luoi.className = 'transaction-meta-grid';
    for (const [nhan, giaTri] of cacTruong) {
        const the = document.createElement('div');
        the.className = 'transaction-meta-card';
        const tenTruong = document.createElement('span');
        tenTruong.className = 'transaction-meta-label';
        tenTruong.textContent = nhan;
        const giaTriTruong = document.createElement('strong');
        giaTriTruong.className = 'transaction-meta-value';
        giaTriTruong.textContent = giaTri;
        the.append(tenTruong, giaTriTruong);
        luoi.append(the);
    }
    return luoi;
}

function taoBangChiTiet(cacDong, loai) {
    const muc = document.createElement('section');
    muc.className = 'transaction-items-section';
    const dauMuc = document.createElement('div');
    dauMuc.className = 'transaction-section-heading';
    const tieuDe = document.createElement('h3');
    tieuDe.textContent = 'Mặt hàng';
    const soMatHang = document.createElement('span');
    soMatHang.className = 'transaction-item-count';
    soMatHang.textContent = `${cacDong.length} sản phẩm`;
    dauMuc.append(tieuDe, soMatHang);
    const khung = document.createElement('div');
    khung.className = 'table-wrap transaction-table-wrap';
    const bang = document.createElement('table');
    bang.className = 'transaction-table';
    const dauBang = document.createElement('thead');
    const dongTieuDe = document.createElement('tr');
    for (const nhan of ['Mặt hàng', 'Số lượng', loai === 'don-hang' ? 'Đơn giá' : 'Giá nhập', 'Thành tiền']) {
        const o = document.createElement('th');
        o.textContent = nhan;
        dongTieuDe.append(o);
    }
    dauBang.append(dongTieuDe);
    const thanBang = document.createElement('tbody');
    for (const matHang of cacDong) {
        const dong = document.createElement('tr');
        const matHangCell = document.createElement('td');
        matHangCell.className = 'transaction-product-cell';
        const tenMatHang = document.createElement('strong');
        tenMatHang.textContent = matHang.tenHang;
        const maMatHang = document.createElement('small');
        maMatHang.textContent = `${matHang.maSanPham} · ${matHang.donViTinh}`;
        matHangCell.append(tenMatHang, maMatHang);
        dong.append(matHangCell);
        const soLuong = document.createElement('td');
        soLuong.className = 'transaction-number-cell';
        soLuong.textContent = matHang.soLuong;
        dong.append(soLuong);
        const donGia = document.createElement('td');
        donGia.className = 'transaction-number-cell';
        donGia.textContent = dinhDangTien(loai === 'don-hang' ? matHang.donGia : matHang.giaNhap);
        dong.append(donGia);
        const thanhTien = document.createElement('td');
        thanhTien.className = 'transaction-amount-cell';
        thanhTien.textContent = dinhDangTien(matHang.thanhTien);
        dong.append(thanhTien);
        thanBang.append(dong);
    }
    bang.append(dauBang, thanBang);
    khung.append(bang);
    muc.append(dauMuc, khung);
    return muc;
}

function taoTongKetGiaoDich(cacDong, dongChinh) {
    const tongKet = document.createElement('section');
    tongKet.className = 'transaction-totals';
    for (const [nhan, giaTri, laChinh = false] of cacDong) {
        const dong = document.createElement('div');
        dong.className = `transaction-total-row${laChinh ? ' primary' : ''}`;
        const tenTruong = document.createElement('span');
        tenTruong.textContent = nhan;
        const giaTriTruong = document.createElement('strong');
        giaTriTruong.textContent = dinhDangTien(giaTri);
        dong.append(tenTruong, giaTriTruong);
        tongKet.append(dong);
    }
    const ketQua = document.createElement('div');
    ketQua.className = 'transaction-grand-total';
    const nhanKetQua = document.createElement('span');
    nhanKetQua.textContent = dongChinh[0];
    const giaTriKetQua = document.createElement('strong');
    giaTriKetQua.textContent = dinhDangTien(dongChinh[1]);
    ketQua.append(nhanKetQua, giaTriKetQua);
    tongKet.append(ketQua);
    return tongKet;
}

function hienThiChiTietDonHang(donHang) {
    const noiDung = document.getElementById('noiDungChiTietGiaoDich');
    noiDung.replaceChildren();
    noiDung.append(taoKhoiThongTin([
        ['Khách hàng', donHang.tenKhachHang],
        ['Mã khách hàng', donHang.maKhachHang ?? 'Khách lẻ'],
        ['Thẻ VIP', donHang.maThe || 'Không sử dụng'],
        ['Thanh toán', donHang.phuongThucThanhToan]
    ]));
    noiDung.append(taoBangChiTiet(donHang.matHang, 'don-hang'));
    noiDung.append(taoTongKetGiaoDich([
        ['Tiền hàng', donHang.tienHang],
        [`Giảm VIP (${donHang.tyLeGiamVip}%)`, -donHang.giamGiaVip],
        ['Giảm thêm', -donHang.giamGiaThuCong],
        ['Khách đưa', donHang.tienKhachDua],
        ['Tiền thừa', donHang.tienThua]
    ], ['Khách cần trả', donHang.tongTien]));
}

function hienThiChiTietPhieuNhap(phieuNhap) {
    const noiDung = document.getElementById('noiDungChiTietGiaoDich');
    noiDung.replaceChildren();
    noiDung.append(taoKhoiThongTin([
        ['Nhà cung cấp', phieuNhap.nhaCungCap],
        ['Số mặt hàng', `${phieuNhap.matHang.length} sản phẩm`],
        ['Ghi chú', phieuNhap.ghiChu || 'Không có']
    ]));
    noiDung.append(taoBangChiTiet(phieuNhap.matHang, 'phieu-nhap'));
    noiDung.append(taoTongKetGiaoDich([], ['Tổng tiền nhập', phieuNhap.tongTien]));
}

async function moChiTietGiaoDich(loai, ma) {
    const laDonHang = loai === 'don-hang';
    const tieuDe = document.getElementById('tieuDeChiTietGiaoDich');
    const loaiGiaoDich = document.getElementById('loaiChiTietGiaoDich');
    const ngayGiaoDich = document.getElementById('ngayChiTietGiaoDich');
    const noiDung = document.getElementById('noiDungChiTietGiaoDich');
    const thongBao = document.getElementById('thongBaoChiTietGiaoDich');
    const nutInPhieuThu = document.getElementById('nutInChiTietGiaoDich');
    const nutLapPhieuChi = document.getElementById('nutLapPhieuChiTuChiTiet');
    state.giaoDichHienTai = null;
    nutInPhieuThu.hidden = true;
    nutLapPhieuChi.hidden = true;
    loaiGiaoDich.textContent = laDonHang ? 'CHI TIẾT HÓA ĐƠN' : 'CHI TIẾT PHIẾU NHẬP';
    tieuDe.textContent = `${laDonHang ? 'Hóa đơn' : 'Phiếu nhập'} #${ma}`;
    ngayGiaoDich.textContent = '';
    noiDung.replaceChildren();
    thongBao.className = 'form-message';
    thongBao.textContent = 'Đang tải chi tiết...';
    document.getElementById('hopThoaiChiTietGiaoDich').showModal();
    try {
        const giaoDich = await goiApi(`/api/${loai}/${ma}`);
        if (laDonHang) {
            state.giaoDichHienTai = giaoDich;
            hienThiChiTietDonHang(giaoDich);
            ngayGiaoDich.textContent = new Date(giaoDich.thoiGianTao).toLocaleString('vi-VN');
            nutInPhieuThu.hidden = false;
            nutLapPhieuChi.hidden = false;
        } else {
            hienThiChiTietPhieuNhap(giaoDich);
            ngayGiaoDich.textContent = new Date(giaoDich.thoiGianNhap).toLocaleString('vi-VN');
        }
        thongBao.textContent = giaoDich.matHang.length ? '' : 'Giao dịch này chưa có dòng mặt hàng chi tiết.';
    } catch (loi) {
        thongBao.className = 'form-message error';
        thongBao.textContent = `Không tải được chi tiết giao dịch: ${loi.message}`;
    }
}

async function taiMatHangNhapKho() {
    const danhSach = document.getElementById('chonMatHangNhap');
    try {
        const thamSo = new URLSearchParams({ kichThuoc: '100' });
        const tuKhoa = document.getElementById('tuKhoaNhapKho').value.trim();
        if (tuKhoa) thamSo.set('tuKhoa', tuKhoa);
        const ketQua = await goiApi(`/api/san-pham?${thamSo}`);
        state.sanPhamNhapKho = ketQua.danhSach;
        const luaChonCu = danhSach.value;
        danhSach.replaceChildren(new Option('Chọn mặt hàng', ''));
        for (const matHang of ketQua.danhSach) {
            const luaChon = new Option(
                `${matHang.maSanPham} · ${matHang.tenHang} (tồn ${matHang.tonKho})`,
                matHang.maHang
            );
            danhSach.append(luaChon);
        }
        if (state.sanPhamNhapKho.some(matHang => String(matHang.maHang) === luaChonCu)) {
            danhSach.value = luaChonCu;
        }
        capNhatGiaNhapMacDinh();
    } catch (loi) {
        hienThiLoi('thongBaoNhapKho', loi);
    }
}

function capNhatGiaNhapMacDinh() {
    const maHang = Number(document.getElementById('chonMatHangNhap').value);
    const matHang = state.sanPhamNhapKho.find(sanPham => sanPham.maHang === maHang);
    document.getElementById('giaNhapDonVi').value = matHang?.giaNhap > 0 ? matHang.giaNhap : '';
}

function themDongVaoPhieuNhap() {
    const maHang = Number(document.getElementById('chonMatHangNhap').value);
    const matHang = state.sanPhamNhapKho.find(sanPham => sanPham.maHang === maHang);
    const oSoLuong = document.getElementById('soLuongNhap');
    const oGiaNhap = document.getElementById('giaNhapDonVi');
    const soLuong = Number(oSoLuong.value);
    const giaNhap = Number(oGiaNhap.value);
    const thongBao = document.getElementById('thongBaoNhapKho');
    thongBao.className = 'form-message';
    oSoLuong.setAttribute('aria-invalid', 'false');
    oGiaNhap.setAttribute('aria-invalid', 'false');
    if (!matHang) {
        thongBao.textContent = 'Vui lòng chọn mặt hàng.';
        return;
    }
    if (!oSoLuong.value.trim() || !Number.isSafeInteger(soLuong) || soLuong <= 0) {
        oSoLuong.setAttribute('aria-invalid', 'true');
        thongBao.textContent = 'Vui lòng nhập số lượng nguyên lớn hơn 0.';
        oSoLuong.focus();
        return;
    }
    if (!oGiaNhap.value.trim() || !Number.isSafeInteger(giaNhap) || giaNhap < 0) {
        oGiaNhap.setAttribute('aria-invalid', 'true');
        thongBao.textContent = 'Vui lòng nhập giá nhập là số nguyên không âm. Có thể nhập 0 nếu đó là giá thực tế.';
        oGiaNhap.focus();
        return;
    }
    if (!Number.isSafeInteger(giaNhap * soLuong)) {
        thongBao.textContent = 'Thành tiền của mặt hàng vượt quá giới hạn cho phép.';
        return;
    }
    if (state.dongPhieuNhap.has(maHang)) {
        thongBao.textContent = 'Mặt hàng đã có trong phiếu; hãy xóa dòng cũ trước khi thêm lại.';
        return;
    }
    state.dongPhieuNhap.set(maHang, { ...matHang, soLuong, giaNhap });
    thongBao.textContent = '';
    hienThiDongPhieuNhap();
}

function hienThiDongPhieuNhap() {
    const bang = document.getElementById('dongPhieuNhap');
    bang.replaceChildren();
    let tongTien = 0;
    for (const [maHang, dongNhap] of state.dongPhieuNhap) {
        const thanhTien = dongNhap.giaNhap * dongNhap.soLuong;
        tongTien += thanhTien;
        const dong = document.createElement('tr');
        const ten = document.createElement('td');
        ten.textContent = `${dongNhap.maSanPham} · ${dongNhap.tenHang}`;
        const soLuong = document.createElement('td');
        soLuong.textContent = `${dongNhap.soLuong} ${dongNhap.donViTinh}`;
        const giaNhap = document.createElement('td');
        giaNhap.textContent = dinhDangTien(dongNhap.giaNhap);
        const thanhTienO = document.createElement('td');
        thanhTienO.textContent = dinhDangTien(thanhTien);
        const thaoTac = document.createElement('td');
        const nutXoa = document.createElement('button');
        nutXoa.type = 'button';
        nutXoa.className = 'remove-item';
        nutXoa.textContent = 'Xóa';
        nutXoa.setAttribute('aria-label', `Xóa ${dongNhap.tenHang} khỏi phiếu nhập`);
        nutXoa.title = 'Xóa mặt hàng này khỏi phiếu nhập';
        nutXoa.addEventListener('click', () => {
            state.dongPhieuNhap.delete(maHang);
            hienThiDongPhieuNhap();
        });
        thaoTac.append(nutXoa);
        dong.append(ten, soLuong, giaNhap, thanhTienO, thaoTac);
        bang.append(dong);
    }
    document.getElementById('phieuNhapTrong').hidden = state.dongPhieuNhap.size > 0;
    document.getElementById('tongTienNhap').textContent = dinhDangTien(tongTien);
}

async function luuPhieuNhap() {
    const thongBao = document.getElementById('thongBaoNhapKho');
    thongBao.className = 'form-message';
    const nhaCungCap = document.getElementById('nhaCungCap').value.trim();
    if (!nhaCungCap) {
        thongBao.textContent = 'Vui lòng nhập tên nhà cung cấp.';
        return;
    }
    if (!state.dongPhieuNhap.size) {
        thongBao.textContent = 'Thêm ít nhất một mặt hàng vào phiếu nhập.';
        return;
    }
    const phieuNhap = {
        nhaCungCap,
        ghiChu: document.getElementById('ghiChuNhap').value.trim(),
        matHang: Array.from(state.dongPhieuNhap, ([maHang, dong]) => ({
            maHang,
            soLuong: dong.soLuong,
            giaNhap: dong.giaNhap
        }))
    };
    thongBao.textContent = 'Đang lưu phiếu nhập...';
    let phieuDaLuu;
    try {
        phieuDaLuu = await goiApi('/api/phieu-nhap', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(phieuNhap)
        });
    } catch (loi) {
        hienThiLoi('thongBaoNhapKho', loi);
        return;
    }

    state.dongPhieuNhap.clear();
    document.getElementById('nhaCungCap').value = '';
    document.getElementById('ghiChuNhap').value = '';
    hienThiDongPhieuNhap();
    thongBao.className = 'form-message success';
    thongBao.textContent = `Đã lưu phiếu nhập #${phieuDaLuu.maPhieuNhap}.`;
    await Promise.all([taiTongQuan(), taiBaoCaoThuChi(), taiTonKho(), taiDanhSachMatHang(), taiMatHangNhapKho()]);
}

async function taiDanhSachMatHang() {
    const thamSo = taoThamSoLoc('tuKhoaMatHang', 'locDanhMucMatHang', 'locTinhTrangMatHang');
    thamSo.set('trang', state.trangMatHang);
    thamSo.set('kichThuoc', state.kichThuocTrang);
    const thongBao = document.getElementById('thongBaoMatHang');
    try {
        const ketQua = await goiApi(`/api/san-pham?${thamSo}`);
        const bang = document.getElementById('bangMatHang');
        bang.replaceChildren();
        for (const matHang of ketQua.danhSach) {
            const dong = document.createElement('tr');
            for (const noiDung of [
                matHang.maSanPham,
                matHang.tenHang,
                matHang.danhMuc,
                dinhDangTien(matHang.donGia),
                `${matHang.tonKho} ${matHang.donViTinh}`,
                matHang.tonToiThieu
            ]) {
                const o = document.createElement('td');
                o.textContent = noiDung;
                dong.append(o);
            }
            const oTrangThai = document.createElement('td');
            oTrangThai.append(taoTheTrangThai(matHang));
            dong.append(oTrangThai);
            bang.append(dong);
        }
        thongBao.textContent = ketQua.tongSoMatHang ? `Tìm thấy ${ketQua.tongSoMatHang} mặt hàng.` : 'Không tìm thấy mặt hàng phù hợp.';
        document.getElementById('thongTinTrang').textContent = `Trang ${ketQua.tongSoTrang ? ketQua.trang + 1 : 0} / ${ketQua.tongSoTrang}`;
        document.getElementById('trangTruoc').disabled = ketQua.trang === 0;
        document.getElementById('trangSau').disabled = ketQua.tongSoTrang === 0 || ketQua.trang + 1 >= ketQua.tongSoTrang;
    } catch (loi) {
        document.getElementById('bangMatHang').replaceChildren();
        thongBao.textContent = `Không tải được danh sách: ${loi.message}`;
    }
}

async function taiTonKho() {
    const thamSo = taoThamSoLoc('tuKhoaKho', 'locDanhMucKho', 'locTinhTrangKho');
    thamSo.set('kichThuoc', '100');
    const thongBao = document.getElementById('thongBaoKho');
    try {
        const ketQua = await goiApi(`/api/san-pham?${thamSo}`);
        const bang = document.getElementById('bangTonKho');
        bang.replaceChildren();
        for (const matHang of ketQua.danhSach) {
            const dong = document.createElement('tr');
            for (const noiDung of [
                matHang.maSanPham,
                matHang.tenHang,
                matHang.danhMuc,
                `${matHang.tonKho} ${matHang.donViTinh}`,
                matHang.tonToiThieu
            ]) {
                const o = document.createElement('td');
                o.textContent = noiDung;
                dong.append(o);
            }
            const oTienDo = document.createElement('td');
            const tienDo = document.createElement('div');
            tienDo.className = 'stock-meter';
            const phanTram = matHang.tonToiThieu > 0
                ? Math.min(100, matHang.tonKho / matHang.tonToiThieu * 100)
                : matHang.tonKho > 0 ? 100 : 0;
            const muc = document.createElement('span');
            muc.style.width = `${phanTram}%`;
            tienDo.append(muc);
            oTienDo.append(tienDo);
            dong.append(oTienDo);
            const oTrangThai = document.createElement('td');
            oTrangThai.append(taoTheTrangThai(matHang));
            dong.append(oTrangThai);
            bang.append(dong);
        }
        thongBao.textContent = ketQua.tongSoMatHang ? `Hiển thị ${ketQua.danhSach.length} / ${ketQua.tongSoMatHang} mặt hàng.` : 'Không có mặt hàng phù hợp với bộ lọc.';
    } catch (loi) {
        document.getElementById('bangTonKho').replaceChildren();
        thongBao.textContent = `Không tải được tồn kho: ${loi.message}`;
    }
}

async function taiSanPhamBanHang() {
    const thamSo = taoThamSoLoc('tuKhoaBanHang', 'locDanhMucBanHang');
    thamSo.set('tinhTrang', 'con-hang');
    thamSo.set('kichThuoc', '100');
    const danhSach = document.getElementById('sanPhamBanHang');
    try {
        const ketQua = await goiApi(`/api/san-pham?${thamSo}`);
        state.sanPhamBanHang = ketQua.danhSach;
        danhSach.replaceChildren();
        for (const matHang of ketQua.danhSach) {
            const dong = document.createElement('div');
            dong.className = 'product-option';
            const moTa = document.createElement('div');
            const ten = document.createElement('strong');
            ten.textContent = `${matHang.maSanPham} · ${matHang.tenHang}`;
            const chiTiet = document.createElement('small');
            chiTiet.textContent = `${dinhDangTien(matHang.donGia)} · Còn ${matHang.tonKho} ${matHang.donViTinh}`;
            moTa.append(ten, chiTiet);
            const nutThem = document.createElement('button');
            nutThem.className = 'button secondary';
            nutThem.textContent = 'Thêm';
            nutThem.addEventListener('click', () => themVaoGio(matHang));
            dong.append(moTa, nutThem);
            danhSach.append(dong);
        }
        if (!ketQua.danhSach.length) danhSach.textContent = 'Không tìm thấy mặt hàng còn tồn.';
    } catch (loi) {
        danhSach.textContent = `Không tải được mặt hàng: ${loi.message}`;
    }
}

function themVaoGio(matHang) {
    const dangCo = state.gioHang.get(matHang.maHang);
    if (dangCo && dangCo.soLuong >= matHang.tonKho) {
        document.getElementById('thongBaoThanhToan').textContent = 'Số lượng trong đơn đã đạt mức tồn kho.';
        return;
    }
    state.gioHang.set(matHang.maHang, { ...matHang, soLuong: (dangCo?.soLuong || 0) + 1 });
    document.getElementById('thongBaoThanhToan').textContent = '';
    hienThiGioHang();
}

function hienThiGioHang() {
    const bang = document.getElementById('gioHang');
    bang.replaceChildren();
    let tienHang = 0;
    for (const [maHang, matHang] of state.gioHang) {
        tienHang += matHang.donGia * matHang.soLuong;
        const dong = document.createElement('tr');
        const ten = document.createElement('td');
        ten.textContent = matHang.tenHang;
        const oSoLuong = document.createElement('td');
        const soLuong = document.createElement('input');
        soLuong.className = 'cart-quantity';
        soLuong.type = 'number';
        soLuong.min = '1';
        soLuong.max = matHang.tonKho;
        soLuong.value = matHang.soLuong;
        soLuong.setAttribute('aria-label', `Số lượng ${matHang.tenHang}`);
        soLuong.addEventListener('change', () => {
            const soLuongMoi = Math.max(1, Math.min(matHang.tonKho, Number(soLuong.value) || 1));
            state.gioHang.set(maHang, { ...matHang, soLuong: soLuongMoi });
            hienThiGioHang();
        });
        oSoLuong.append(soLuong);
        const thanhTien = document.createElement('td');
        thanhTien.textContent = dinhDangTien(matHang.donGia * matHang.soLuong);
        const oXoa = document.createElement('td');
        const nutXoa = document.createElement('button');
        nutXoa.className = 'remove-item';
        nutXoa.type = 'button';
        nutXoa.textContent = 'Xóa';
        nutXoa.addEventListener('click', () => {
            state.gioHang.delete(maHang);
            hienThiGioHang();
        });
        oXoa.append(nutXoa);
        dong.append(ten, oSoLuong, thanhTien, oXoa);
        bang.append(dong);
    }
    document.getElementById('gioHangTrong').hidden = state.gioHang.size > 0;
    document.getElementById('tienHang').textContent = dinhDangTien(tienHang);
    capNhatThanhToan(tienHang);
}

function capNhatThanhToan(tienHang = Array.from(state.gioHang.values()).reduce((tong, mh) => tong + mh.donGia * mh.soLuong, 0)) {
    const soDienThoai = document.getElementById('soDienThoaiThanhVien').value.trim();
    const thanhVien = state.khachHang.find(khach => khach.soDienThoai === soDienThoai);
    const theVip = thanhVien?.theVip
        .filter(the => the.coTheSuDung)
        .sort((a, b) => Number(b.tyLeGiam ?? state.tyLeGiamVip) - Number(a.tyLeGiam ?? state.tyLeGiamVip)
            || a.maThe.localeCompare(b.maThe))[0];
    const tyLeGiam = theVip ? Number(theVip.tyLeGiam ?? state.tyLeGiamVip) : 0;
    const mucGiamVip = theVip ? Math.round(tienHang * tyLeGiam / 100) : 0;
    const giamGiaNhap = Math.max(0, Number(document.getElementById('giamGia').value) || 0);
    const giamGiaThuCong = Math.min(giamGiaNhap, Math.max(0, tienHang - mucGiamVip));
    const tongTien = Math.max(0, tienHang - mucGiamVip - giamGiaThuCong);
    const tienKhachDua = Math.max(0, Number(document.getElementById('tienKhachDua').value) || 0);
    document.getElementById('tienGiamVip').textContent = dinhDangTien(mucGiamVip);
    document.getElementById('tienGiamThuCong').textContent = dinhDangTien(giamGiaThuCong);
    document.getElementById('tongTien').textContent = dinhDangTien(tongTien);
    const tienThua = tienKhachDua - tongTien;
    document.getElementById('tienThua').textContent = tienThua < 0
        ? `Còn thiếu ${dinhDangTien(Math.abs(tienThua))}`
        : dinhDangTien(tienThua);
    document.getElementById('thongTinThanhVien').textContent = thanhVien
        ? theVip
            ? `${thanhVien.tenKhachHang} dùng thẻ ${theVip.maThe} · giảm ${tyLeGiam}% · còn ${theVip.soLanConLai} lượt`
            : thanhVien.theVip.length === 0
                ? `${thanhVien.tenKhachHang} chưa được cấp thẻ VIP.`
                : thanhVien.theVip.some(the => the.dangHoatDong)
                    ? `${thanhVien.tenKhachHang} đã hết lượt thẻ VIP; đơn này không được giảm VIP.`
                    : `${thanhVien.tenKhachHang} không có thẻ VIP đang hoạt động.`
        : soDienThoai ? 'Số điện thoại chưa đăng ký thành viên; không áp dụng giảm giá VIP.'
            : 'Nhập số điện thoại đã đăng ký để kiểm tra thẻ VIP.';
}

async function xacNhanThanhToan() {
    const thongBao = document.getElementById('thongBaoThanhToan');
    thongBao.className = 'form-message';
    if (!state.gioHang.size) {
        thongBao.textContent = 'Vui lòng thêm ít nhất một mặt hàng.';
        return;
    }
    const donHang = {
        tenKhachHang: document.getElementById('tenKhachHang').value.trim(),
        giamGia: Number(document.getElementById('giamGia').value) || 0,
        tienKhachDua: Number(document.getElementById('tienKhachDua').value) || 0,
        phuongThucThanhToan: document.getElementById('phuongThucThanhToan').value,
        matHang: Array.from(state.gioHang, ([maHang, matHang]) => ({ maHang, soLuong: matHang.soLuong })),
        soDienThoai: document.getElementById('soDienThoaiThanhVien').value.trim()
    };
    thongBao.textContent = 'Đang lưu đơn hàng...';
    try {
        const hoaDon = await goiApi('/api/don-hang', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(donHang)
        });
        hienThiHoaDon(hoaDon);
        state.gioHang.clear();
        document.getElementById('tenKhachHang').value = '';
        document.getElementById('soDienThoaiThanhVien').value = '';
        document.getElementById('giamGia').value = '0';
        document.getElementById('tienKhachDua').value = '0';
        hienThiGioHang();
        thongBao.textContent = '';
        document.getElementById('hopThoaiHoaDon').showModal();
        await Promise.all([taiTongQuan(), taiBaoCaoThuChi(), taiDanhSachMatHang(), taiTonKho(), taiSanPhamBanHang(), taiKhachHang()]);
    } catch (loi) {
        hienThiLoi('thongBaoThanhToan', loi);
    }
}

function hienThiHoaDon(hoaDon) {
    state.hoaDonHienTai = hoaDon;
    const noiDung = document.getElementById('noiDungHoaDon');
    noiDung.replaceChildren();
    for (const [nhan, giaTri] of [
        ['Mã đơn hàng', `#${hoaDon.maDonHang}`],
        ['Khách hàng', hoaDon.tenKhachHang],
        ['Thẻ VIP', hoaDon.maThe || 'Không sử dụng'],
        ['Tiền hàng', dinhDangTien(hoaDon.tienHang)],
        ['Giảm VIP', `${hoaDon.tyLeGiamVip}% · ${dinhDangTien(hoaDon.giamGiaVip)}`],
        ['Giảm thêm', dinhDangTien(hoaDon.giamGiaThuCong)],
        ['Tổng giảm', dinhDangTien(hoaDon.giamGia)],
        ['Tổng thanh toán', dinhDangTien(hoaDon.tongTien)],
        ['Tiền thừa', dinhDangTien(hoaDon.tienThua)]
    ]) {
        const dong = document.createElement('p');
        dong.textContent = `${nhan}: ${giaTri}`;
        noiDung.append(dong);
    }
    document.getElementById('thongBaoHoaDon').textContent = '';
}

function themDongThongTinIn(danhSach, nhan, giaTri) {
    const dong = document.createElement('div');
    const ten = document.createElement('dt');
    ten.textContent = nhan;
    const noiDung = document.createElement('dd');
    noiDung.textContent = giaTri;
    dong.append(ten, noiDung);
    danhSach.append(dong);
}

function taoBangPhieuIn(matHang) {
    if (!matHang?.length) return null;
    const bang = document.createElement('table');
    const dau = document.createElement('thead');
    const dongTieuDe = document.createElement('tr');
    for (const nhan of ['Mặt hàng', 'SL', 'Đơn giá', 'Thành tiền']) {
        const o = document.createElement('th');
        o.textContent = nhan;
        dongTieuDe.append(o);
    }
    dau.append(dongTieuDe);
    const than = document.createElement('tbody');
    for (const matHangDong of matHang) {
        const dong = document.createElement('tr');
        const ten = document.createElement('td');
        ten.textContent = `${matHangDong.tenHang} (${matHangDong.maSanPham})`;
        const soLuong = document.createElement('td');
        soLuong.textContent = `${matHangDong.soLuong} ${matHangDong.donViTinh}`;
        const donGia = document.createElement('td');
        donGia.textContent = dinhDangTien(matHangDong.donGia);
        const thanhTien = document.createElement('td');
        thanhTien.textContent = dinhDangTien(matHangDong.thanhTien);
        dong.append(ten, soLuong, donGia, thanhTien);
        than.append(dong);
    }
    bang.append(dau, than);
    return bang;
}

function chuanBiPhieuIn(tieuDe, thongTin, matHang, cacDongTongTien) {
    const khuVucIn = document.getElementById('printArea');
    khuVucIn.replaceChildren();
    const phieu = document.createElement('article');
    phieu.className = 'print-document';
    const tieuDeCuaHang = document.createElement('p');
    tieuDeCuaHang.className = 'print-store-name';
    tieuDeCuaHang.textContent = 'SIÊU THỊ';
    const tieuDePhieu = document.createElement('h1');
    tieuDePhieu.textContent = tieuDe;
    phieu.append(tieuDeCuaHang, tieuDePhieu);

    const danhSachThongTin = document.createElement('dl');
    danhSachThongTin.className = 'print-meta';
    for (const [nhan, giaTri] of thongTin) {
        themDongThongTinIn(danhSachThongTin, nhan, giaTri);
    }
    phieu.append(danhSachThongTin);

    const bang = taoBangPhieuIn(matHang);
    if (bang) phieu.append(bang);

    const tongKet = document.createElement('dl');
    tongKet.className = 'print-totals';
    for (const [nhan, giaTri] of cacDongTongTien) {
        themDongThongTinIn(tongKet, nhan, giaTri);
    }
    phieu.append(tongKet);

    const camOn = document.createElement('p');
    camOn.className = 'print-thanks';
    camOn.textContent = 'Cảm ơn quý khách và hẹn gặp lại!';
    phieu.append(camOn);
    khuVucIn.append(phieu);
}

async function inPhieuThu(donHang = state.hoaDonHienTai) {
    const thongBao = document.getElementById('thongBaoHoaDon');
    const maDonHang = typeof donHang === 'number' ? donHang : donHang?.maDonHang;
    thongBao.className = 'form-message';
    try {
        const chiTiet = typeof donHang === 'object' && donHang?.matHang
            ? donHang
            : await goiApi(`/api/don-hang/${maDonHang}`);
        chuanBiPhieuIn('PHIẾU THU / HÓA ĐƠN BÁN LẺ', [
            ['Mã đơn hàng', `#${chiTiet.maDonHang}`],
            ['Ngày lập', new Date(chiTiet.thoiGianTao).toLocaleString('vi-VN')],
            ['Khách hàng', chiTiet.tenKhachHang],
            ['Phương thức thanh toán', chiTiet.phuongThucThanhToan]
        ], chiTiet.matHang, [
            ['Tiền hàng', dinhDangTien(chiTiet.tienHang)],
            ['Tổng giảm giá', dinhDangTien(chiTiet.giamGia)],
            ['Khách cần trả', dinhDangTien(chiTiet.tongTien)],
            ['Khách đã đưa', dinhDangTien(chiTiet.tienKhachDua)],
            ['Tiền thừa', dinhDangTien(chiTiet.tienThua)]
        ]);
        window.print();
        thongBao.textContent = '';
    } catch (loi) {
        thongBao.className = 'form-message error';
        thongBao.textContent = `Không in được phiếu thu: ${loi.message}`;
    }
}

function moFormPhieuChi(donHang) {
    state.hoaDonHienTai = donHang;
    document.getElementById('formPhieuChi').reset();
    document.getElementById('thongTinDonHoanTien').textContent =
        `Đơn hàng #${donHang.maDonHang} · ${donHang.tenKhachHang} · Tổng thanh toán ${dinhDangTien(donHang.tongTien)}.`;
    const soTienHoan = document.getElementById('soTienHoan');
    soTienHoan.max = donHang.tongTien;
    soTienHoan.value = donHang.tongTien > 0 ? donHang.tongTien : '';
    document.getElementById('thongBaoPhieuChi').textContent = '';
    if (document.getElementById('hopThoaiHoaDon').open) {
        document.getElementById('hopThoaiHoaDon').close();
    }
    if (document.getElementById('hopThoaiChiTietGiaoDich').open) {
        document.getElementById('hopThoaiChiTietGiaoDich').close();
    }
    document.getElementById('hopThoaiPhieuChi').showModal();
}

async function luuPhieuChi(suKien) {
    suKien.preventDefault();
    const thongBao = document.getElementById('thongBaoPhieuChi');
    const donHang = state.hoaDonHienTai;
    thongBao.className = 'form-message';
    thongBao.textContent = 'Đang lưu phiếu chi...';
    let phieuChi;
    try {
        phieuChi = await goiApi('/api/phieu-chi', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                maDonHang: donHang.maDonHang,
                soTien: Number(document.getElementById('soTienHoan').value),
                lyDo: document.getElementById('lyDoHoanTien').value.trim()
            })
        });
    } catch (loi) {
        thongBao.className = 'form-message error';
        thongBao.textContent = `Không lưu được phiếu chi: ${loi.message}`;
        return;
    }

    document.getElementById('hopThoaiPhieuChi').close();
    try {
        chuanBiPhieuIn('PHIẾU CHI HOÀN TIỀN', [
            ['Mã phiếu chi', `#${phieuChi.maPhieuChi}`],
            ['Mã đơn hàng', `#${phieuChi.maDonHang}`],
            ['Ngày chi', new Date(phieuChi.ngayChi).toLocaleString('vi-VN')],
            ['Khách hàng', phieuChi.hoTenKhach],
            ['Lý do', phieuChi.lyDo]
        ], [], [['Số tiền hoàn', dinhDangTien(phieuChi.soTien)]]);
        window.print();
    } catch (loi) {
        const thongBaoThanhToan = document.getElementById('thongBaoThanhToan');
        thongBaoThanhToan.className = 'form-message error';
        thongBaoThanhToan.textContent =
            `Đã lưu phiếu chi #${phieuChi.maPhieuChi} nhưng không tạo được bản in: ${loi.message}`;
        return;
    }
    const thongBaoThanhToan = document.getElementById('thongBaoThanhToan');
    thongBaoThanhToan.className = 'form-message success';
    thongBaoThanhToan.textContent =
        `Đã lưu phiếu chi #${phieuChi.maPhieuChi}. Nếu hộp thoại in không tự mở, hãy thử in lại bằng Ctrl+P.`;
    await Promise.all([taiTongQuan(), taiBaoCaoThuChi()]);
}

async function taoTaiKhoan(suKien) {
    suKien.preventDefault();
    const thongBao = document.getElementById('thongBaoTaiKhoan');
    thongBao.className = 'form-message';
    thongBao.textContent = 'Đang tạo tài khoản...';
    try {
        const ketQua = await goiApi('/api/auth/accounts', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                hoTen: document.getElementById('hoTenTaiKhoan').value.trim(),
                tenDangNhap: document.getElementById('tenDangNhapTaiKhoan').value.trim(),
                matKhau: document.getElementById('matKhauTaiKhoan').value,
                vaiTro: document.getElementById('vaiTroTaiKhoan').value
            })
        });
        document.getElementById('formTaiKhoan').reset();
        thongBao.className = 'form-message success';
        thongBao.textContent = ketQua.message;
    } catch (loi) {
        thongBao.className = 'form-message error';
        thongBao.textContent = `Không tạo được tài khoản: ${loi.message}`;
    }
}

async function luuMatHang(suKien) {
    suKien.preventDefault();
    const form = suKien.currentTarget;
    const duLieu = new FormData(form);
    const matHang = Object.fromEntries(duLieu.entries());
    for (const truong of ['giaNhap', 'donGia', 'tonKho', 'tonToiThieu']) {
        matHang[truong] = Number(matHang[truong]);
    }
    const thongBao = document.getElementById('thongBaoFormMatHang');
    thongBao.className = 'form-message';
    thongBao.textContent = 'Đang lưu mặt hàng...';
    try {
        await goiApi('/api/mat-hang', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(matHang)
        });
    } catch (loi) {
        hienThiLoi('thongBaoFormMatHang', loi);
        return;
    }
    form.reset();
    thongBao.textContent = 'Đã thêm mặt hàng vào danh mục.';
    thongBao.className = 'form-message success';
    await taiDanhMuc().catch(loi => {
        thongBao.textContent = `Đã thêm mặt hàng nhưng không tải lại danh mục được: ${loi.message}`;
    });
    window.setTimeout(() => document.getElementById('hopThoaiMatHang').close(), 500);
    await Promise.all([taiDanhSachMatHang(), taiTonKho(), taiTongQuan()]);
}

function xuatBaoCaoKho() {
    const thamSo = taoThamSoLoc('tuKhoaKho', 'locDanhMucKho', 'locTinhTrangKho');
    window.location.href = `/api/bao-cao/kho.csv?${thamSo}`;
}

document.querySelectorAll('.nav-button, [data-open]').forEach(nut => {
    nut.addEventListener('click', () => chuyenManHinh(nut.dataset.view || nut.dataset.open));
});

document.getElementById('taiLaiTongQuan').addEventListener('click', () => Promise.all([taiTongQuan(), taiBaoCaoThuChi()]));
for (const id of ['donHangGanDay', 'phieuNhapGanDay']) {
    document.getElementById(id).addEventListener('click', event => {
        const nut = event.target.closest('.giao-dich-chi-tiet');
        if (nut) moChiTietGiaoDich(nut.dataset.type, nut.dataset.id);
    });
}
document.getElementById('loaiBaoCao').addEventListener('change', capNhatTruongKyBaoCao);
document.getElementById('formBaoCaoThuChi').addEventListener('submit', event => {
    event.preventDefault();
    taiBaoCaoThuChi();
});
document.getElementById('taiLaiMatHang').addEventListener('click', taiDanhSachMatHang);
document.getElementById('taiLaiKho').addEventListener('click', taiTonKho);
document.getElementById('tuKhoaMatHang').addEventListener('input', () => { state.trangMatHang = 0; taiDanhSachMatHang(); });
document.getElementById('locDanhMucMatHang').addEventListener('change', () => { state.trangMatHang = 0; taiDanhSachMatHang(); });
document.getElementById('locTinhTrangMatHang').addEventListener('change', () => { state.trangMatHang = 0; taiDanhSachMatHang(); });
document.getElementById('tuKhoaKho').addEventListener('input', taiTonKho);
document.getElementById('locDanhMucKho').addEventListener('change', taiTonKho);
document.getElementById('locTinhTrangKho').addEventListener('change', taiTonKho);
document.getElementById('tuKhoaBanHang').addEventListener('input', taiSanPhamBanHang);
document.getElementById('locDanhMucBanHang').addEventListener('change', taiSanPhamBanHang);
document.getElementById('trangTruoc').addEventListener('click', () => { state.trangMatHang--; taiDanhSachMatHang(); });
document.getElementById('trangSau').addEventListener('click', () => { state.trangMatHang++; taiDanhSachMatHang(); });
document.getElementById('giamGia').addEventListener('input', () => capNhatThanhToan());
document.getElementById('soDienThoaiThanhVien').addEventListener('input', () => capNhatThanhToan());
document.getElementById('tienKhachDua').addEventListener('input', () => capNhatThanhToan());
document.getElementById('xacNhanThanhToan').addEventListener('click', xacNhanThanhToan);
document.getElementById('nutInPhieuThu').addEventListener('click', () => inPhieuThu());
document.getElementById('nutLapPhieuChi').addEventListener('click', () => {
    if (state.hoaDonHienTai) moFormPhieuChi(state.hoaDonHienTai);
});
document.getElementById('formPhieuChi').addEventListener('submit', luuPhieuChi);
for (const id of ['dongFormPhieuChi', 'huyFormPhieuChi']) {
    document.getElementById(id).addEventListener('click', () => document.getElementById('hopThoaiPhieuChi').close());
}
document.getElementById('nutInChiTietGiaoDich').addEventListener('click', () => {
    if (state.giaoDichHienTai) inPhieuThu(state.giaoDichHienTai);
});
document.getElementById('nutLapPhieuChiTuChiTiet').addEventListener('click', () => {
    if (state.giaoDichHienTai) moFormPhieuChi(state.giaoDichHienTai);
});
document.getElementById('tuKhoaNhapKho').addEventListener('input', taiMatHangNhapKho);
document.getElementById('chonMatHangNhap').addEventListener('change', capNhatGiaNhapMacDinh);
document.getElementById('themDongNhap').addEventListener('click', themDongVaoPhieuNhap);
document.getElementById('luuPhieuNhap').addEventListener('click', luuPhieuNhap);
document.getElementById('formMatHang').addEventListener('submit', luuMatHang);
document.getElementById('formKhachHang').addEventListener('submit', luuKhachHang);
document.getElementById('formTaiKhoan').addEventListener('submit', taoTaiKhoan);
document.getElementById('formDangXuat').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    await dangXuat();
    if (document.getElementById('tokenDangXuat').value) form.submit();
});
document.getElementById('formCapTheVip').addEventListener('submit', capTheVip);
document.getElementById('formCauHinhVip').addEventListener('submit', luuCauHinhVip);
document.getElementById('huySuaKhachHang').addEventListener('click', ketThucSuaKhachHang);
document.getElementById('bangKhachHang').addEventListener('click', async event => {
    const nut = event.target.closest('.customer-action');
    if (!nut) return;
    if (nut.dataset.action === 'edit') {
        const khach = state.khachHang.find(item => item.maKhachHang === Number(nut.dataset.customerId));
        if (!khach) return;
        batDauSuaKhachHang(khach);
        return;
    }
    try {
        await goiApi(`/api/the-vip/${encodeURIComponent(nut.dataset.cardId)}/trang-thai`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ dangHoatDong: nut.dataset.active !== 'true' })
        });
        await taiKhachHang();
    } catch (loi) {
        const thongBao = document.getElementById('thongBaoKhachHang');
        thongBao.className = 'form-message error';
        thongBao.textContent = `Không cập nhật được thẻ VIP: ${loi.message}`;
    }
});
document.getElementById('xuatBaoCao').addEventListener('click', xuatBaoCaoKho);
document.getElementById('moFormMatHang').addEventListener('click', () => {
    document.getElementById('thongBaoFormMatHang').textContent = '';
    document.getElementById('hopThoaiMatHang').showModal();
});
document.getElementById('dongFormMatHang').addEventListener('click', () => document.getElementById('hopThoaiMatHang').close());
document.getElementById('huyFormMatHang').addEventListener('click', () => document.getElementById('hopThoaiMatHang').close());

const ngayHienTai = new Date();
const ngayBaoCao = `${ngayHienTai.getFullYear()}-${String(ngayHienTai.getMonth() + 1).padStart(2, '0')}-${String(ngayHienTai.getDate()).padStart(2, '0')}`;
document.getElementById('kyBaoCaoNgay').value = ngayBaoCao;
document.getElementById('kyBaoCaoThang').value = ngayHienTai.getFullYear();
document.getElementById('kyBaoCaoNam').value = ngayHienTai.getFullYear();
capNhatTruongKyBaoCao();

async function khoiTaoUngDung() {
    try {
        const taiKhoan = await goiApi('/api/auth/me');
        await goiApi('/api/auth/csrf');
        apDungQuyen(taiKhoan);
        const laQuanLy = taiKhoan.vaiTro === 'MANAGER';
        const viecCanTai = [taiDanhMuc(), taiKhachHang()];
        if (laQuanLy) viecCanTai.push(taiBaoCaoThuChi(), taiCauHinhVip());
        await Promise.all(viecCanTai);
    } catch (loi) {
        if (loi.status === 401) {
            window.location.replace('/login.html');
            return;
        }
        document.body.classList.remove('auth-loading');
        document.getElementById('thongBaoThanhToan').textContent =
            `Không khởi tạo được giao diện: ${loi.message}`;
    }
}

khoiTaoUngDung();
