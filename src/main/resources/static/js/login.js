async function layTokenCsrf() {
    const phanHoi = await fetch('/api/auth/csrf');
    if (!phanHoi.ok) throw new Error('Không lấy được mã bảo mật để đăng nhập.');
    const duLieu = await phanHoi.json();
    return duLieu.token;
}

document.getElementById('formDangNhap').addEventListener('submit', async event => {
    event.preventDefault();
    const formElement = event.currentTarget;
    const nutDangNhap = formElement.querySelector('button[type="submit"]');
    const thongBao = document.getElementById('thongBaoDangNhap');
    thongBao.className = 'login-message';
    thongBao.textContent = 'Đang đăng nhập...';
    nutDangNhap.disabled = true;
    nutDangNhap.querySelector('span').textContent = 'Đang xác thực...';
    try {
        const form = new URLSearchParams(new FormData(formElement));
        const token = await layTokenCsrf();
        const phanHoi = await fetch('/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-XSRF-TOKEN': token
            },
            body: form
        });
        if (!phanHoi.ok) {
            const loi = await phanHoi.json().catch(() => null);
            throw new Error(loi?.message || 'Đăng nhập không thành công.');
        }
        window.location.replace('/');
    } catch (loi) {
        thongBao.className = 'login-message error';
        thongBao.textContent = loi instanceof TypeError
            ? 'Không kết nối được máy chủ. Hãy kiểm tra ứng dụng đang chạy tại http://localhost:8080 rồi thử lại.'
            : loi.message || 'Đăng nhập chưa thành công. Vui lòng thử lại.';
        nutDangNhap.disabled = false;
        nutDangNhap.querySelector('span').textContent = 'Đăng nhập';
    }
});
