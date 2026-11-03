
document.addEventListener("DOMContentLoaded", function () {

    const form = document.getElementById("quenMatKhauForm");

    if (!form) return;

    const email = document.getElementById("email");
    const soDienThoai = document.getElementById("soDienThoai");
    const matKhauMoi = document.getElementById("matKhauMoi");
    const xacNhan = document.getElementById("xacNhanMatKhau");

    const loiEmail = document.getElementById("loiEmail");
    const loiSDT = document.getElementById("loiSoDienThoai");
    const loiMatKhau = document.getElementById("loiMatKhauMoi");
    const loiXacNhan = document.getElementById("loiXacNhanMatKhau");

    function hienLoi(input, vungLoi, thongBao) {
        input.classList.add("is-invalid");
        input.setAttribute("aria-invalid", "true");
        vungLoi.textContent = thongBao;
    }

    function xoaLoi(input, vungLoi) {
        input.classList.remove("is-invalid");
        input.removeAttribute("aria-invalid");
        vungLoi.textContent = "";
    }

    function kiemTraEmail() {
        const giaTri = email.value.trim();

        xoaLoi(email, loiEmail);

        if (giaTri === "") {
            hienLoi(email, loiEmail,
                "Vui lòng nhập email.");
            return false;
        }

        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(giaTri)) {
            hienLoi(email, loiEmail,
                "Vui lòng nhập địa chỉ email hợp lệ.");
            return false;
        }

        return true;
    }

    function kiemTraSDT() {
        const giaTri = soDienThoai.value.trim();

        xoaLoi(soDienThoai, loiSDT);

        if (giaTri === "") {
            hienLoi(soDienThoai, loiSDT,
                "Vui lòng nhập số điện thoại.");
            return false;
        }

        if (!/^0[0-9]{9,10}$/.test(giaTri)) {
            hienLoi(soDienThoai, loiSDT,
                "Số điện thoại phải gồm 10 hoặc 11 chữ số và bắt đầu bằng số 0.");
            return false;
        }

        return true;
    }

    function kiemTraMatKhau() {
        const giaTri = matKhauMoi.value;

        xoaLoi(matKhauMoi, loiMatKhau);

        if (giaTri === "") {
            hienLoi(matKhauMoi, loiMatKhau,
                "Vui lòng nhập mật khẩu mới.");
            return false;
        }

        // Phai trung voi quy tac o server: 8-72 ky tu ASCII, khong dau, khong khoang trang
        if (!/^[\x21-\x7E]{8,72}$/.test(giaTri)) {
            hienLoi(matKhauMoi, loiMatKhau,
                "Mật khẩu phải có từ 8 đến 72 ký tự, không dấu và không có khoảng trắng.");
            return false;
        }

        return true;
    }

    function kiemTraXacNhan() {
        const giaTri = xacNhan.value;

        xoaLoi(xacNhan, loiXacNhan);

        if (giaTri === "") {
            hienLoi(xacNhan, loiXacNhan,
                "Vui lòng xác nhận mật khẩu mới.");
            return false;
        }

        if (giaTri !== matKhauMoi.value) {
            hienLoi(xacNhan, loiXacNhan,
                "Xác nhận mật khẩu không khớp.");
            return false;
        }

        return true;
    }

    // Kiem tra tat ca truong khi nhan nut
    form.addEventListener("submit", function (event) {

        const emailHopLe = kiemTraEmail();
        const sdtHopLe = kiemTraSDT();
        const matKhauHopLe = kiemTraMatKhau();
        const xacNhanHopLe = kiemTraXacNhan();

        if (!emailHopLe || !sdtHopLe
            || !matKhauHopLe || !xacNhanHopLe) {
            event.preventDefault();

            const oLoiDauTien = form.querySelector(".is-invalid");
            if (oLoiDauTien) {
                oLoiDauTien.focus();
            }
        }
    });

    // Xoa loi khi nguoi dung chinh sua
    email.addEventListener("input", function () {
        xoaLoi(email, loiEmail);
    });

    soDienThoai.addEventListener("input", function () {
        xoaLoi(soDienThoai, loiSDT);
    });

    matKhauMoi.addEventListener("input", function () {
        xoaLoi(matKhauMoi, loiMatKhau);
        xoaLoi(xacNhan, loiXacNhan);
    });

    xacNhan.addEventListener("input", function () {
        xoaLoi(xacNhan, loiXacNhan);
    });

});
