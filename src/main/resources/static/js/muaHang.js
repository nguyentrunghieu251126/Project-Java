"use strict";
/* TV3 – xử lý giao diện phần mua hàng. Chỉ thay đổi hiển thị; số liệu thật do backend tính lại. */

const dinhDangTien = (so) => so.toLocaleString("vi-VN") + "₫";
const ganChu = (selector, chu) => document.querySelectorAll(selector).forEach((el) => { el.textContent = chu; });

/* ===================== THÔNG BÁO ===================== */
function dongThongBao(el) {
  el.classList.remove("show");
  setTimeout(() => el.remove(), 250);
}
function ganThongBao(el) {
  requestAnimationFrame(() => el.classList.add("show"));
  el.querySelector(".btn-close")?.addEventListener("click", () => dongThongBao(el));
  setTimeout(() => dongThongBao(el), el.classList.contains("mh-toast-err") ? 7000 : 4000);
}
function hienThongBao(noiDung, laLoi) {
  let khung = document.querySelector(".mh-toast-wrap");
  if (!khung) { khung = document.createElement("div"); khung.className = "mh-toast-wrap"; document.body.appendChild(khung); }
  const el = document.createElement("div");
  el.className = "mh-toast " + (laLoi ? "mh-toast-err" : "mh-toast-ok");
  el.setAttribute("role", laLoi ? "alert" : "status");
  const icon = document.createElement("i");
  icon.className = "bi " + (laLoi ? "bi-exclamation-circle-fill" : "bi-check-circle-fill");
  const chu = document.createElement("span");
  chu.textContent = noiDung;
  const dong = document.createElement("button");
  dong.type = "button"; dong.className = "btn-close"; dong.setAttribute("aria-label", "Đóng thông báo");
  el.append(icon, chu, dong);
  khung.appendChild(el);
  ganThongBao(el);
}
document.querySelectorAll(".mh-toast").forEach(ganThongBao);

/* ===================== GIỎ HÀNG ===================== */
const dongGioHang = [...document.querySelectorAll(".mh-line")];
const dongChonDuoc = () => dongGioHang.filter((d) => !d.querySelector('input[name="sanPham"]').disabled);

function tinhLaiGioHang() {
  if (!dongGioHang.length) return;
  let soChon = 0;
  let tong = 0;
  dongGioHang.forEach((dong) => {
    const chon = dong.querySelector('input[name="sanPham"]');
    const soLuong = Number(dong.querySelector(".mh-qty input")?.value || 0);
    dong.classList.toggle("is-off", !chon.checked);
    if (chon.checked) {
      soChon += 1;
      tong += Number(dong.dataset.donGia || 0) * soLuong;
    }
  });
  ganChu("[data-so-chon]", soChon);
  ganChu("[data-tong-dong]", dongGioHang.length);
  ganChu("[data-tong-chon]", dinhDangTien(tong));
  const nutMua = document.querySelector("[data-nut-mua]");
  if (nutMua) nutMua.disabled = soChon === 0;
  const nutXoa = document.querySelector("[data-xoa-da-chon]");
  if (nutXoa) nutXoa.disabled = soChon === 0;
  const chonTatCa = document.getElementById("chonTatCa");
  if (chonTatCa) {
    const tong = dongChonDuoc().length;
    chonTatCa.checked = tong > 0 && soChon === tong;
    chonTatCa.indeterminate = soChon > 0 && soChon < tong;
  }
}

document.getElementById("chonTatCa")?.addEventListener("change", (e) => {
  dongChonDuoc().forEach((dong) => { dong.querySelector('input[name="sanPham"]').checked = e.target.checked; });
  tinhLaiGioHang();
});
dongGioHang.forEach((dong) =>
  dong.querySelector('input[name="sanPham"]').addEventListener("change", tinhLaiGioHang));

// Xóa đã chọn: chép mã sản phẩm đang chọn vào form xóa nhiều trước khi gửi
document.getElementById("formXoaDaChon")?.addEventListener("submit", (e) => {
  const form = e.target;
  form.querySelectorAll('input[name="maSanPham"]').forEach((i) => i.remove());
  dongGioHang.forEach((dong) => {
    const chon = dong.querySelector('input[name="sanPham"]');
    if (!chon.checked) return;
    const an = document.createElement("input");
    an.type = "hidden"; an.name = "maSanPham"; an.value = chon.value;
    form.appendChild(an);
  });
});

// Nút +/- số lượng (không vượt tồn kho). Lưu lên server sau 400ms kể từ lần bấm cuối.
const formMuaHang = document.getElementById("formMuaHang");
const csrfTen = document.querySelector('meta[name="_csrf_parameter"]')?.content || "_csrf";
const csrfMa = document.querySelector('meta[name="_csrf"]')?.content || "";

function luuSoLuong(input, soLuong) {
  const url = formMuaHang?.dataset.urlCapNhat;
  if (!url || url === "#") return;
  const duLieu = new URLSearchParams({ maSanPham: input.dataset.maSanPham, soLuong: String(soLuong) });
  if (csrfMa) duLieu.append(csrfTen, csrfMa);
  fetch(url, { method: "POST", body: duLieu, headers: { "X-Requested-With": "fetch" } })
    .then((r) => (r.ok ? r.json() : Promise.reject(r.status)))
    .then((kq) => {
      if (!kq.thanhCong) { hienThongBao(kq.thongBao || "Không cập nhật được số lượng.", true); return; }
      if (Number(input.value) !== kq.soLuong) { input.value = kq.soLuong; tinhLaiGioHang(); }
      // Cập nhật số trên biểu tượng giỏ hàng ở header
      const huyHieu = document.getElementById("cartCount");
      if (huyHieu && kq.soLuongGioHang !== undefined) huyHieu.textContent = kq.soLuongGioHang;
    })
    .catch(() => { hienThongBao("Mất kết nối, đang tải lại giỏ hàng…", true); setTimeout(() => location.reload(), 1200); });
}

document.querySelectorAll(".mh-qty").forEach((hop) => {
  const input = hop.querySelector("input");
  const max = Number(hop.dataset.max || input.max || 99);
  const nutGiam = hop.querySelector('[data-buoc="-1"]');
  const nutTang = hop.querySelector('[data-buoc="1"]');
  let hen = null;
  const capNhat = (giaTri, luu) => {
    const so = Math.min(Math.max(Number(giaTri) || 1, 1), max);
    input.value = so;
    if (!input.disabled) {
      nutGiam.disabled = so <= 1;
      nutTang.disabled = so >= max;
    }
    tinhLaiGioHang();
    if (luu) {
      clearTimeout(hen);
      hen = setTimeout(() => luuSoLuong(input, so), 400);
    }
  };
  hop.querySelectorAll(".mh-qty-btn").forEach((nut) =>
    nut.addEventListener("click", () => capNhat(Number(input.value) + Number(nut.dataset.buoc), true)));
  input.addEventListener("change", () => capNhat(input.value, true));
  capNhat(input.value, false);
});
tinhLaiGioHang();

/* ===================== THANH TOÁN ===================== */

// Thời gian giao dự kiến đổi theo địa chỉ đang chọn (data-* do server tính theo tỉnh / thành)
const radioDiaChiGiao = document.querySelectorAll('input[name="maDiaChi"]');
const oNgayGiao = document.querySelector("[data-giao-ngay]");
if (radioDiaChiGiao.length && oNgayGiao) {
  const oVung = document.querySelector("[data-giao-vung]");
  const capNhatNgayGiao = () => {
    const chon = document.querySelector('input[name="maDiaChi"]:checked');
    if (!chon) return;
    oNgayGiao.textContent = chon.dataset.ngayNhan;
    if (oVung) oVung.textContent = `${chon.dataset.vung}, ${chon.dataset.soNgay}`;
  };
  radioDiaChiGiao.forEach((r) => r.addEventListener("change", capNhatNgayGiao));
  capNhatNgayGiao();
}

// Chọn địa chỉ trong modal: cập nhật ô "Địa chỉ nhận hàng" rồi đóng modal
const modalDiaChi = document.getElementById("modalDiaChi");
if (modalDiaChi) {
  modalDiaChi.querySelectorAll('input[name="maDiaChi"]').forEach((r) => {
    r.addEventListener("change", () => {
      ganChu("[data-chon-ten]", r.dataset.ten);
      ganChu("[data-chon-sdt]", r.dataset.sdt);
      ganChu("[data-chon-dia-chi]", r.dataset.diaChi);
      document.querySelectorAll("[data-chon-mac-dinh]").forEach((el) => { el.hidden = r.dataset.macDinh !== "true"; });
      bootstrap.Modal.getInstance(modalDiaChi)?.hide();
    });
  });
}

// Sổ địa chỉ: hỏi lại trước khi xóa (form xóa nằm ngoài form đặt hàng, nút dùng thuộc tính form=)
document.querySelectorAll("form[data-xac-nhan]").forEach((f) => {
  f.addEventListener("submit", (e) => {
    if (!confirm(f.dataset.xacNhan)) e.preventDefault();
  });
});

// Tổng thanh toán = tiền hàng − mã giảm giá − điểm thưởng − ưu đãi lần đầu của phương thức thanh toán
// (chỉ để hiển thị; server tính lại khi đặt hàng)
const tongTienHang = Number(document.querySelector("[data-tam-tinh]")?.dataset.tamTinh || 0);
let giamMaHienTai = 0;
const hopDiem = document.getElementById("diem-thuong");
const batDungDiem = document.getElementById("batDungDiem");
const oDiem = document.getElementById("diemSuDung");

// Điểm được dùng tối đa: không quá số điểm đang có, không quá 50% tiền hàng sau mã giảm giá; dưới 10 điểm thì 0
function diemToiDa() {
  if (!hopDiem) return 0;
  const d = hopDiem.dataset;
  const theoDon = Math.floor((tongTienHang - giamMaHienTai) * Number(d.phanTram) / 100 / Number(d.giaTri));
  const toiDa = Math.min(Number(d.diem), Math.max(0, theoDon));
  return toiDa < Number(d.toiThieu) ? 0 : toiDa;
}

function diemDangDung() {
  if (!hopDiem || !batDungDiem?.checked) return 0;
  const toiDa = diemToiDa();
  let so = Math.floor(Number(oDiem.value) || 0);
  if (so > toiDa) { so = toiDa; oDiem.value = so; }
  return so < Number(hopDiem.dataset.toiThieu) ? 0 : so;
}

function capNhatTongThanhToan() {
  if (!document.querySelector("[data-tam-tinh]")) return;
  // Điểm thưởng
  const diem = diemDangDung();
  const giamDiem = hopDiem ? diem * Number(hopDiem.dataset.giaTri) : 0;
  if (hopDiem) {
    const toiDa = diemToiDa();
    const goiY = hopDiem.querySelector("[data-goi-y-diem]");
    if (goiY) {
      goiY.textContent = toiDa > 0
        ? `Dùng từ ${hopDiem.dataset.toiThieu} đến ${toiDa.toLocaleString("vi-VN")} điểm cho đơn này (giảm tối đa ${dinhDangTien(toiDa * Number(hopDiem.dataset.giaTri))}).`
        : "Đơn này chưa dùng được điểm (cần ít nhất 10 điểm và tối đa 50% giá trị đơn).";
    }
    oDiem.max = toiDa;
    const dongDiem = document.querySelector("[data-dong-giam-diem]");
    if (dongDiem) {
      dongDiem.hidden = giamDiem <= 0;
      ganChu("[data-so-tien-giam-diem]", "−" + dinhDangTien(giamDiem));
      ganChu("[data-so-diem-dung]", `(${diem.toLocaleString("vi-VN")} điểm)`);
    }
  }
  // Ưu đãi phương thức thanh toán: không vượt số tiền còn lại
  const pt = document.querySelector('input[name="phuongThucThanhToan"]:checked');
  const giamTT = Math.min(Number(pt?.dataset.giamTt || 0), Math.max(0, tongTienHang - giamMaHienTai - giamDiem));
  const dongTT = document.querySelector("[data-dong-giam-tt]");
  if (dongTT) {
    // Luôn hiện dòng này; chưa có ưu đãi thì ghi 0₫ (màu xám)
    const oTien = dongTT.querySelector("[data-so-tien-giam-tt]");
    oTien.textContent = giamTT > 0 ? "−" + dinhDangTien(giamTT) : "0₫";
    oTien.classList.toggle("mh-minus", giamTT > 0);
    oTien.classList.toggle("mh-zero", giamTT <= 0);
    const ten = pt?.closest(".mh-option")?.querySelector(".mh-option-body > strong")?.textContent || "";
    const oTen = dongTT.querySelector("[data-ten-pt-giam]");
    if (oTen) oTen.textContent = ten ? `(${ten})` : "";
  }
  const tongGiam = giamMaHienTai + giamDiem + giamTT;
  const tong = Math.max(0, tongTienHang - tongGiam);
  const tietKiem = document.querySelector("[data-tiet-kiem]");
  if (tietKiem) {
    tietKiem.hidden = tongGiam <= 0;
    tietKiem.querySelector("b").textContent = dinhDangTien(tongGiam);
  }
  ganChu("[data-tong-thanh-toan]", dinhDangTien(tong));
  // Điểm sẽ tích: mỗi 100.000₫ thực trả = 1 điểm
  const diemNhan = document.querySelector("[data-diem-nhan]");
  if (diemNhan) {
    const so = Math.floor(tong / 100000);
    diemNhan.hidden = so <= 0;
    diemNhan.querySelector("b").textContent = so.toLocaleString("vi-VN");
  }
}

// Bật / tắt dùng điểm; ô số điểm tắt thì không gửi lên server
if (hopDiem && batDungDiem && oDiem) {
  const oNhap = document.getElementById("oNhapDiem");
  const capNhatBat = () => {
    oNhap.hidden = !batDungDiem.checked;
    oDiem.disabled = !batDungDiem.checked;
    if (batDungDiem.checked && !(Number(oDiem.value) > 0)) oDiem.value = diemToiDa();
    capNhatTongThanhToan();
  };
  batDungDiem.addEventListener("change", capNhatBat);
  oDiem.addEventListener("input", capNhatTongThanhToan);
  oDiem.addEventListener("change", () => {
    const so = Math.floor(Number(oDiem.value) || 0);
    if (so > 0 && so < Number(hopDiem.dataset.toiThieu)) oDiem.value = hopDiem.dataset.toiThieu;
    capNhatTongThanhToan();
  });
  document.getElementById("nutDungToiDa")?.addEventListener("click", () => { oDiem.value = diemToiDa(); capNhatTongThanhToan(); });
  capNhatBat();
}

// Ưu đãi: sao chép mã giảm giá
document.querySelectorAll(".mh-copy-code").forEach((nut) => {
  nut.addEventListener("click", () => {
    const ma = nut.dataset.ma;
    const xong = () => { const cu = nut.textContent; nut.textContent = "Đã sao chép"; setTimeout(() => { nut.textContent = cu; }, 1500); };
    if (navigator.clipboard) navigator.clipboard.writeText(ma).then(xong, xong); else xong();
  });
});

// Đổi phương thức thanh toán: hiện thông tin chuyển khoản (nếu có) và tính lại ưu đãi
const radioThanhToan = document.querySelectorAll('input[name="phuongThucThanhToan"]');
if (radioThanhToan.length) {
  const hopChuyenKhoan = document.getElementById("thongTinChuyenKhoan");
  const capNhatThanhToan = () => {
    const chon = document.querySelector('input[name="phuongThucThanhToan"]:checked');
    if (hopChuyenKhoan) hopChuyenKhoan.hidden = !chon || chon.value !== "CHUYEN_KHOAN";
    capNhatTongThanhToan();
  };
  radioThanhToan.forEach((r) => r.addEventListener("change", capNhatThanhToan));
  capNhatThanhToan();
}

// Mã ưu đãi: chọn trong danh sách hoặc nhập mã, rồi tính lại tóm tắt (server kiểm tra lại khi đặt hàng)
const modalMa = document.getElementById("modalMaUuDai");
if (modalMa) {
  const tamTinh = Number(document.querySelector("[data-tam-tinh]")?.dataset.tamTinh || 0);
  const oNhap = document.getElementById("nhapMaGiamGia");
  const loi = document.getElementById("loiMaGiamGia");
  const oMaDaChon = document.getElementById("maGiamGiaDaChon");
  const radioMa = () => [...modalMa.querySelectorAll('input[name="chonMa"]')];

  const apDung = (ve) => {
    const dongGiam = document.querySelector("[data-dong-giam]");
    const tomTat = document.querySelector("[data-voucher-tom-tat]");
    const giam = ve ? Math.min(Number(ve.dataset.tienGiam || 0), tamTinh) : 0;
    if (ve) {
      ganChu("[data-ma-hien]", ve.dataset.ma);
      ganChu("[data-so-tien-giam]", "−" + dinhDangTien(giam));
      tomTat.replaceChildren();
      const ma = document.createElement("span");
      ma.className = "mh-code";
      ma.textContent = ve.dataset.ma;
      tomTat.append(ma, ` ${ve.dataset.ten} đã được áp dụng`);
    } else {
      tomTat.textContent = "Chọn hoặc nhập mã giảm giá";
    }
    dongGiam.hidden = !ve;
    oMaDaChon.value = ve ? ve.dataset.ma : "";
    giamMaHienTai = giam;
    capNhatTongThanhToan();
  };

  // Trang hiển thị lại sau lỗi: giữ mã khách đã chọn
  if (oMaDaChon.value) {
    const r = radioMa().find((x) => x.value.toUpperCase() === oMaDaChon.value.toUpperCase() && !x.disabled);
    if (r) { r.checked = true; apDung(r.closest(".mh-ticket")); } else { oMaDaChon.value = ""; }
  }

  modalMa.addEventListener("show.bs.modal", () => { loi.hidden = true; oNhap.value = ""; });

  document.getElementById("nutApDungMa").addEventListener("click", () => {
    const ma = oNhap.value.trim().toUpperCase();
    const radio = radioMa().find((r) => r.value.toUpperCase() === ma);
    loi.hidden = true;
    if (!ma) return;
    if (!radio || radio.disabled) {
      loi.querySelector("span").textContent = radio
        ? (radio.closest(".mh-ticket")?.dataset.lyDo || "Mã này chưa dùng được cho đơn hàng.")
        : "Mã không tồn tại, đã hết hạn, hết lượt hoặc đang tạm dừng.";
      loi.hidden = false;
      return;
    }
    radio.checked = true;
    radio.closest(".mh-ticket").scrollIntoView({ block: "nearest", behavior: "smooth" });
  });
  oNhap.addEventListener("keydown", (e) => {
    if (e.key === "Enter") { e.preventDefault(); document.getElementById("nutApDungMa").click(); }
  });

  document.getElementById("nutXacNhanMa").addEventListener("click", () =>
    apDung(modalMa.querySelector('input[name="chonMa"]:checked')?.closest(".mh-ticket")));
  document.getElementById("nutBoMa").addEventListener("click", () => {
    radioMa().forEach((r) => { r.checked = false; });
  });
}

// Chống bấm "Đặt hàng" hai lần
document.getElementById("formDatHang")?.addEventListener("submit", (e) => {
  const nut = e.target.querySelector(".mh-btn-order");
  if (nut) { setTimeout(() => { nut.disabled = true; nut.textContent = "Đang đặt hàng…"; }, 0); }
});

/* ===================== ĐƠN HÀNG ===================== */

// Hủy đơn: gán action + mã đơn cho modal theo nút được bấm
const modalHuy = document.getElementById("modalHuyDon");
if (modalHuy) {
  modalHuy.addEventListener("show.bs.modal", (e) => {
    const nut = e.relatedTarget;
    if (!nut) return;
    document.getElementById("formHuyDon").action = nut.dataset.action || "#";
    const hien = modalHuy.querySelector("[data-ma-don-hien]");
    if (hien) hien.textContent = "#" + (nut.dataset.maDon || "");
  });
}

// Đánh giá sản phẩm
const modalDanhGia = document.getElementById("modalDanhGia");
if (modalDanhGia) {
  const form = document.getElementById("formDanhGia");
  const nhanSao = { 1: "Rất tệ", 2: "Không hài lòng", 3: "Bình thường", 4: "Hài lòng", 5: "Tuyệt vời" };
  const chuSao = modalDanhGia.querySelector("[data-rating-text]");
  const noiDung = modalDanhGia.querySelector("#noiDungDanhGia");
  const demKyTu = modalDanhGia.querySelector("[data-dem-ky-tu]");

  modalDanhGia.addEventListener("show.bs.modal", (e) => {
    const nut = e.relatedTarget;
    if (!nut) return;
    form.reset();
    form.querySelector('input[name="maSanPham"]').value = nut.dataset.maSanPham || "";
    modalDanhGia.querySelector("[data-review-ten]").textContent = nut.dataset.ten || "";
    const anh = modalDanhGia.querySelector("[data-review-anh]");
    anh.src = nut.dataset.anh || "";
    anh.alt = nut.dataset.ten || "";
    chuSao.textContent = "Chọn số sao";
    demKyTu.textContent = "0";
  });

  form.querySelectorAll('input[name="soSao"]').forEach((r) =>
    r.addEventListener("change", () => { chuSao.textContent = nhanSao[r.value]; }));
  noiDung.addEventListener("input", () => { demKyTu.textContent = noiDung.value.length; });
}

// In hóa đơn
document.querySelectorAll("[data-in-hoa-don]").forEach((nut) =>
  nut.addEventListener("click", () => window.print()));
