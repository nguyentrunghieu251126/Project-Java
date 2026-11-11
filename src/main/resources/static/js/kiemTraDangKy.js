
document.addEventListener("DOMContentLoaded", function () {

    const form = document.getElementById("dangKyForm");
    if (!form) return;

    const fields = {
        hoTen: document.getElementById("hoTen"),
        email: document.getElementById("email"),
        soDienThoai: document.getElementById("soDienThoai"),
        matKhau: document.getElementById("matKhau"),
        xacNhanMatKhau: document.getElementById("xacNhanMatKhau")
    };

    function hienLoi(tenTruong, thongBao) {
        const input = fields[tenTruong];
        const vungLoi = document.getElementById("loi-" + tenTruong);

        input.classList.add("is-invalid");
        input.setAttribute("aria-invalid", "true");
        vungLoi.textContent = thongBao;
    }

    function xoaLoi(tenTruong) {
        const input = fields[tenTruong];
        const vungLoi = document.getElementById("loi-" + tenTruong);

        input.classList.remove("is-invalid");
        input.removeAttribute("aria-invalid");
        vungLoi.textContent = "";
    }

    function kiemTra() {
        let hopLe = true;

        Object.keys(fields).forEach(xoaLoi);

        const hoTen = fields.hoTen.value.trim();
        const email = fields.email.value.trim();
        const sdt = fields.soDienThoai.value.trim();
        const matKhau = fields.matKhau.value;
        const xacNhan = fields.xacNhanMatKhau.value;

        if (!hoTen) {
            hienLoi("hoTen", "Vui lòng nhập họ và tên.");
            hopLe = false;
        } else if (hoTen.length > 100) {
            hienLoi("hoTen", "Họ và tên không được vượt quá 100 ký tự.");
            hopLe = false;
        }

        if (!email) {
            hienLoi("email", "Vui lòng nhập email.");
            hopLe = false;
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            hienLoi("email", "Vui lòng nhập địa chỉ email hợp lệ.");
            hopLe = false;
        } else if (email.length > 100) {
            hienLoi("email", "Email không được vượt quá 100 ký tự.");
            hopLe = false;
        }

        // SDT khong bat buoc
        if (sdt && !/^0[0-9]{9,10}$/.test(sdt)) {
            hienLoi(
                "soDienThoai",
                "Số điện thoại phải gồm 10 hoặc 11 chữ số và bắt đầu bằng số 0."
            );
            hopLe = false;
        }

        if (!matKhau) {
            hienLoi("matKhau", "Vui lòng nhập mật khẩu.");
            hopLe = false;
        } else if (!/^[\x21-\x7E]{8,72}$/.test(matKhau)) {
            hienLoi("matKhau", "Mật khẩu phải có từ 8 đến 72 ký tự, không dấu và không có khoảng trắng.");
            hopLe = false;
        }

        if (!xacNhan) {
            hienLoi("xacNhanMatKhau", "Vui lòng xác nhận mật khẩu.");
            hopLe = false;
        } else if (xacNhan !== matKhau) {
            hienLoi("xacNhanMatKhau", "Xác nhận mật khẩu không khớp.");
            hopLe = false;
        }

        return hopLe;
    }

    form.addEventListener("submit", function (event) {
        if (!kiemTra()) {
            event.preventDefault();

            const oLoi = form.querySelector(".is-invalid");
            if (oLoi) oLoi.focus();
        }
    });

    Object.keys(fields).forEach(function (tenTruong) {
        fields[tenTruong].addEventListener("input", function () {
            xoaLoi(tenTruong);
        });
    });
});
// Hien/an mat khau - Dang ky
document.addEventListener("DOMContentLoaded", function () {

    function xuLyHienMatKhau(idInput, idNut) {
        const input = document.getElementById(idInput);
        const nut = document.getElementById(idNut);

        if (!input || !nut) return;

        const bieuTuong = nut.querySelector("i");
        if (!bieuTuong) return;

        nut.addEventListener("click", function () {

            const dangAn = input.type === "password";

            input.type = dangAn ? "text" : "password";

            bieuTuong.classList.toggle("bi-eye", !dangAn);
            bieuTuong.classList.toggle("bi-eye-slash", dangAn);

            nut.setAttribute(
                "aria-label",
                dangAn ? "Ẩn mật khẩu" : "Hiện mật khẩu"
            );

            nut.setAttribute(
                "aria-pressed",
                String(dangAn)
            );
        });
    }

    xuLyHienMatKhau(
        "matKhau",
        "nutHienMatKhauDangKy"
    );

    xuLyHienMatKhau(
        "xacNhanMatKhau",
        "nutHienXacNhanMatKhauDangKy"
    );
});
