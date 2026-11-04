// Form có data-xac-nhan phải được xác nhận qua hộp thoại trước khi gửi
(function () {
  const hop = document.getElementById("hopXacNhan");
  if (!hop) return;

  const modal = new bootstrap.Modal(hop);
  const noiDung = document.getElementById("hopXacNhanNoiDung");
  let formCho = null;

  document.addEventListener("submit", function (e) {
    const form = e.target;
    if (!form.dataset.xacNhan) return;
    e.preventDefault();
    formCho = form;
    noiDung.textContent = form.dataset.xacNhan;
    modal.show();
  });

  document.getElementById("hopXacNhanDongY").addEventListener("click", function () {
    if (formCho) formCho.submit(); // submit() không kích hoạt lại sự kiện nên không bị hỏi lần hai
  });
})();

// In báo cáo: trình duyệt lấy tiêu đề trang làm tên file khi chọn "Lưu dưới dạng PDF"
document.addEventListener("click", function (e) {
  const nut = e.target.closest("[data-in-bao-cao]");
  if (!nut) return;

  const tieuDeCu = document.title;
  window.addEventListener("afterprint", function () {
    document.title = tieuDeCu;
  }, { once: true });
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
