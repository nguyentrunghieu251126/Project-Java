"use strict";
/*
 * TV3 – KH11: bấm icon giỏ hàng trên thẻ sản phẩm thì thêm vào giỏ ngay tại trang đang xem
 * (gửi fetch tới /gio-hang/them-nhanh), hiện thông báo nhỏ và cập nhật số trên nút "Giỏ hàng".
 * Nút "Mua ngay" vẫn gửi form bình thường để chuyển thẳng sang trang thanh toán.
 * Dùng chung cho trang chủ, chi tiết sản phẩm và so sánh sản phẩm.
 */
(function () {
  function hienThongBao(noiDung, laLoi) {
    let khung = document.getElementById("tv3-toast-wrap");
    if (!khung) {
      khung = document.createElement("div");
      khung.id = "tv3-toast-wrap";
      khung.setAttribute("aria-live", "polite");
      khung.style.cssText = "position:fixed;top:16px;right:16px;z-index:2000;display:grid;gap:10px;max-width:calc(100vw - 32px)";
      document.body.appendChild(khung);
    }
    const el = document.createElement("div");
    el.setAttribute("role", laLoi ? "alert" : "status");
    el.style.cssText = "display:flex;align-items:center;gap:10px;max-width:400px;padding:13px 16px;border-radius:12px;"
      + "font-size:14px;line-height:1.5;box-shadow:0 14px 34px rgba(15,23,42,.16);border:1px solid;"
      + "opacity:0;transform:translateY(-8px);transition:opacity .25s ease,transform .25s ease;"
      + (laLoi ? "background:#fff3f2;border-color:#f3c6c1;color:#b3261e;"
               : "background:#e8f7ed;border-color:#b3e0c0;color:#166534;");
    const icon = document.createElement("i");
    icon.className = "bi " + (laLoi ? "bi-exclamation-circle-fill" : "bi-cart-check-fill");
    icon.style.fontSize = "18px";
    const chu = document.createElement("span");
    chu.style.flex = "1";
    chu.textContent = noiDung;
    el.append(icon, chu);
    if (!laLoi) {
      const xem = document.createElement("a");
      xem.textContent = "Xem giỏ";
      xem.href = (document.querySelector(".cart-link") || {}).href || "/gio-hang";
      xem.style.cssText = "color:#0a8f3c;font-weight:700;text-decoration:none;white-space:nowrap";
      el.appendChild(xem);
    }
    khung.appendChild(el);
    requestAnimationFrame(() => { el.style.opacity = "1"; el.style.transform = "none"; });
    setTimeout(() => {
      el.style.opacity = "0";
      setTimeout(() => el.remove(), 250);
    }, laLoi ? 5000 : 3000);
  }

  function capNhatHuyHieu(soLuong) {
    const huyHieu = document.getElementById("cartCount");
    if (huyHieu && soLuong !== undefined) huyHieu.textContent = soLuong;
    const nut = document.querySelector(".cart-link");
    if (nut) {
      nut.classList.remove("bump");
      void nut.offsetWidth; // khởi động lại hiệu ứng nảy
      nut.classList.add("bump");
    }
  }

  document.addEventListener("submit", (e) => {
    const form = e.target;
    if (!(form instanceof HTMLFormElement)) return;
    const action = form.getAttribute("action") || "";
    if (!/\/gio-hang\/them$/.test(action)) return;
    const nutBam = e.submitter;
    if (nutBam && nutBam.name === "muaNgay") return; // "Mua ngay": để form chuyển sang thanh toán

    e.preventDefault();
    if (nutBam) nutBam.disabled = true;
    fetch(action.replace(/\/them$/, "/them-nhanh"), {
      method: "POST",
      body: new FormData(form),
      headers: { "X-Requested-With": "fetch", "Accept": "application/json" },
    })
      .then((r) => {
        if (r.redirected && r.url.includes("/dang-nhap")) {
          window.location.href = r.url; // chưa đăng nhập
          return null;
        }
        if (r.status === 403) throw new Error("Chỉ tài khoản khách hàng mới thêm được sản phẩm vào giỏ.");
        if (!r.ok) throw new Error("Không thêm được vào giỏ hàng, vui lòng thử lại.");
        return r.json();
      })
      .then((kq) => {
        if (!kq) return;
        hienThongBao(kq.thongBao, !kq.thanhCong);
        if (kq.thanhCong) capNhatHuyHieu(kq.soLuongGioHang);
      })
      .catch((loi) => hienThongBao(loi.message, true))
      .finally(() => { if (nutBam) nutBam.disabled = false; });
  });
})();
