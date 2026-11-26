// Form có data-xac-nhan phải được xác nhận qua hộp thoại trước khi gửi.
// Thuộc tính tùy chọn trên form:
//   data-xac-nhan-tieu-de : tiêu đề hộp thoại (mặc định "Xác nhận thao tác")
//   data-xac-nhan-nut     : chữ trên nút đồng ý (mặc định "Đồng ý")
//   data-xac-nhan-kieu    : "chinh" cho thao tác bình thường (xanh), bỏ trống là thao tác nguy hiểm (đỏ)
//   data-xac-nhan-trich   : tên ô nhập trong form, nội dung ô được trích lại trong hộp thoại
//   data-xac-nhan-trich-nhan : nhãn của phần trích (mặc định "Lý do")
(function () {
  const hop = document.getElementById("hopXacNhan");
  if (!hop) return;

  const modal = new bootstrap.Modal(hop);
  const tieuDe = document.getElementById("hopXacNhanTieuDe");
  const noiDung = document.getElementById("hopXacNhanNoiDung");
  const trich = document.getElementById("hopXacNhanTrich");
  const trichNhan = document.getElementById("hopXacNhanTrichNhan");
  const trichNoiDung = document.getElementById("hopXacNhanTrichNoiDung");
  const bieuTuong = hop.querySelector(".hop-xac-nhan-bieu-tuong i");
  const nutDongY = document.getElementById("hopXacNhanDongY");
  let formCho = null;

  document.addEventListener("submit", function (e) {
    const form = e.target;
    if (!form.dataset.xacNhan) return;
    e.preventDefault();
    formCho = form;

    const d = form.dataset;
    const binhThuong = d.xacNhanKieu === "chinh";
    hop.classList.toggle("kieu-chinh", binhThuong);
    bieuTuong.className = binhThuong ? "bi bi-question-circle" : "bi bi-exclamation-triangle";
    nutDongY.className = binhThuong ? "btn btn-chinh" : "btn btn-danger";
    tieuDe.textContent = d.xacNhanTieuDe || "Xác nhận thao tác";
    noiDung.textContent = d.xacNhan;
    nutDongY.textContent = d.xacNhanNut || "Đồng ý";

    const o = d.xacNhanTrich ? form.elements[d.xacNhanTrich] : null;
    const giaTri = o && o.value ? o.value.trim() : "";
    trich.hidden = !giaTri;
    trichNhan.textContent = d.xacNhanTrichNhan || "Lý do";
    trichNoiDung.textContent = giaTri;

    modal.show();
  });

  hop.addEventListener("shown.bs.modal", function () {
    nutDongY.focus();
  });

  nutDongY.addEventListener("click", function () {
    if (!formCho) return;
    nutDongY.disabled = true;
    formCho.submit();
  });

  hop.addEventListener("hidden.bs.modal", function () {
    nutDongY.disabled = false;
  });
})();

// In báo cáo dưới dạng pdf
document.addEventListener("click", function (e) {
  const nut = e.target.closest("[data-in-bao-cao]");
  if (!nut) return;

  const tieuDeCu = document.title;
  window.addEventListener(
    "afterprint",
    function () {
      document.title = tieuDeCu;
    },
    { once: true },
  );
  document.title = nut.dataset.inBaoCao;
  window.print();
});

// Đổi đơn vị của ô giá trị giảm theo loại khuyến mãi
(function () {
  const loai = document.getElementById("loaiGiamGia");
  const donVi = document.getElementById("donViGiam");
  if (!loai || !donVi) return;

  const capNhat = function () {
    donVi.textContent = loai.value === "PHAN_TRAM" ? "%" : "₫";
  };
  loai.addEventListener("change", capNhat);
  capNhat();
})();
