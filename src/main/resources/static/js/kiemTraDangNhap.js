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
document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll(".thong-bao-tai-khoan").forEach(function (thongBao) {
        let daDong = false;

        function dongThongBao() {
            if (daDong) return;
            daDong = true;
            thongBao.classList.remove("show");
            setTimeout(function () {
                thongBao.remove();
            }, 200);
        }

        const nutDong = thongBao.querySelector(".btn-close");
        if (nutDong) {
            nutDong.addEventListener("click", dongThongBao);
        }

        setTimeout(dongThongBao, 3000);
    });
});
// Hien/an mat khau - Dang nhap
document.addEventListener("DOMContentLoaded", function () {
    const matKhau = document.getElementById("password");
    const nutHienMatKhau = document.getElementById("nutHienMatKhauDangNhap");
    if (!matKhau || !nutHienMatKhau) return;
    const bieuTuong = nutHienMatKhau.querySelector("i");
    nutHienMatKhau.addEventListener("click", function () {
        const dangAn = matKhau.type === "password";
        matKhau.type = dangAn ? "text" : "password";
        bieuTuong.classList.toggle("bi-eye", !dangAn);
        bieuTuong.classList.toggle("bi-eye-slash", dangAn);
        nutHienMatKhau.setAttribute(
            "aria-label",
            dangAn ? "Ẩn mật khẩu" : "Hiện mật khẩu"
        );
        nutHienMatKhau.setAttribute(
            "aria-pressed",
            String(dangAn)
        );
    });
});
