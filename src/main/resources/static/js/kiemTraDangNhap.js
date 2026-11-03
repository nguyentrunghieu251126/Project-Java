
document.addEventListener("DOMContentLoaded", function () {

    const form = document.getElementById("dangNhapForm");
    if (!form) return;

    const email = document.getElementById("username");
    const matKhau = document.getElementById("password");

    const loiEmail = document.getElementById("loiEmail");
    const loiMatKhau = document.getElementById("loiMatKhau");

    function hienLoi(input, noiHienLoi, thongBao) {
        input.classList.add("is-invalid");
        noiHienLoi.textContent = thongBao;
    }

    function xoaLoi(input, noiHienLoi) {
        input.classList.remove("is-invalid");
        noiHienLoi.textContent = "";
    }

    form.addEventListener("submit", function (event) {

        let hopLe = true;

        xoaLoi(email, loiEmail);
        xoaLoi(matKhau, loiMatKhau);

        const giaTriEmail = email.value.trim();

        if (giaTriEmail === "") {

            hienLoi(
                email,
                loiEmail,
                "Vui lòng nhập email."
            );

            hopLe = false;

        } else if (
            !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(giaTriEmail)
        ) {

            hienLoi(
                email,
                loiEmail,
                "Vui lòng nhập địa chỉ email hợp lệ."
            );

            hopLe = false;
        }

        if (matKhau.value === "") {

            hienLoi(
                matKhau,
                loiMatKhau,
                "Vui lòng nhập mật khẩu."
            );

            hopLe = false;
        }

        if (!hopLe) {
            event.preventDefault();
        }
    });

    email.addEventListener("input", function () {
        xoaLoi(email, loiEmail);
    });

    matKhau.addEventListener("input", function () {
        xoaLoi(matKhau, loiMatKhau);
    });
});
