// Mã QR mô phỏng cho bản demo: chỉ có hình dáng giống QR, không mã hóa dữ liệu thật.
// Mỗi phần tử [data-qr-gia="nội dung"] được vẽ thành SVG; cùng nội dung luôn ra cùng một hình.
function veMaQrGia(khung) {
  const N = 29; // kích thước lưới giống QR phiên bản 3
  const noiDung = khung.dataset.qrGia || "PDMJV";
  const lan = Number(khung.dataset.qrLan || 0);

  // Bộ sinh số giả ngẫu nhiên có hạt giống từ nội dung (mulberry32)
  let h = 2166136261 ^ lan;
  for (const c of noiDung) h = Math.imul(h ^ c.charCodeAt(0), 16777619);
  const ngauNhien = () => {
    h = (h + 0x6d2b79f5) | 0;
    let t = Math.imul(h ^ (h >>> 15), 1 | h);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };

  const o = Array.from({ length: N }, () => Array(N).fill(null));
  // Ba ô định vị ở góc kèm viền trắng bao quanh
  const dinhVi = (r0, c0) => {
    for (let r = -1; r <= 7; r++) {
      for (let c = -1; c <= 7; c++) {
        const r1 = r0 + r, c1 = c0 + c;
        if (r1 < 0 || c1 < 0 || r1 >= N || c1 >= N) continue;
        const vien = r === 0 || r === 6 || c === 0 || c === 6;
        const loi = r >= 2 && r <= 4 && c >= 2 && c <= 4;
        o[r1][c1] = r >= 0 && r <= 6 && c >= 0 && c <= 6 && (vien || loi);
      }
    }
  };
  dinhVi(0, 0);
  dinhVi(0, N - 7);
  dinhVi(N - 7, 0);
  // Ô căn chỉnh nhỏ ở góc dưới phải
  for (let r = -2; r <= 2; r++) {
    for (let c = -2; c <= 2; c++) {
      o[N - 7 + r][N - 7 + c] = Math.max(Math.abs(r), Math.abs(c)) !== 1;
    }
  }
  // Đường căn thời gian
  for (let i = 8; i < N - 8; i++) {
    o[6][i] = i % 2 === 0;
    o[i][6] = i % 2 === 0;
  }
  // Chừa khoảng trống ở giữa cho logo
  const giua = Math.floor(N / 2);
  for (let r = giua - 3; r <= giua + 3; r++) {
    for (let c = giua - 3; c <= giua + 3; c++) o[r][c] = false;
  }

  let duong = "";
  for (let r = 0; r < N; r++) {
    for (let c = 0; c < N; c++) {
      const den = o[r][c] === null ? ngauNhien() < 0.5 : o[r][c];
      if (den) duong += `M${c} ${r}h1v1h-1z`;
    }
  }
  khung.innerHTML =
    `<svg viewBox="-2 -2 ${N + 4} ${N + 4}" role="img" aria-label="Mã QR thanh toán (mô phỏng)" shape-rendering="crispEdges">` +
    `<rect x="-2" y="-2" width="${N + 4}" height="${N + 4}" fill="#fff"/>` +
    `<path d="${duong}" fill="currentColor"/></svg>`;
}

document.querySelectorAll("[data-qr-gia]").forEach(veMaQrGia);

// Trang quét mã QR: đếm ngược hạn mã, tạo mã mới, sao chép, mô phỏng quét thành công
(function () {
  const trang = document.getElementById("thanh-toan-qr");
  if (!trang) return;

  const khungQr = trang.querySelector("[data-qr-gia]");
  const hopQr = trang.querySelector(".mh-qr-frame");
  const hetHan = trang.querySelector(".mh-qr-expired");
  const daQuet = trang.querySelector(".mh-qr-scanned");
  const dongHo = trang.querySelector("[data-qr-dem-nguoc]");
  const soGiay = dongHo.querySelector("b");
  const formXacNhan = document.getElementById("formDaThanhToan");
  const nutXacNhan = formXacNhan.querySelector("button[type=submit]");
  const thoiHan = Number(dongHo.dataset.qrDemNguoc) || 900;
  let conLai = thoiHan;
  let hen = null;

  const hien = () => {
    const p = String(Math.floor(conLai / 60)).padStart(2, "0");
    const g = String(conLai % 60).padStart(2, "0");
    soGiay.textContent = `${p}:${g}`;
    dongHo.classList.toggle("sap-het", conLai <= 60);
  };
  const batDau = () => {
    clearInterval(hen);
    conLai = thoiHan;
    hetHan.hidden = true;
    hopQr.classList.remove("het-han");
    nutXacNhan.disabled = false;
    hien();
    hen = setInterval(() => {
      conLai--;
      hien();
      if (conLai <= 0) {
        clearInterval(hen);
        hetHan.hidden = false;
        hopQr.classList.add("het-han");
        nutXacNhan.disabled = true;
      }
    }, 1000);
  };

  trang.querySelector("[data-qr-lam-moi]").addEventListener("click", () => {
    khungQr.dataset.qrLan = String(Number(khungQr.dataset.qrLan || 0) + 1);
    veMaQrGia(khungQr);
    batDau();
  });

  trang.querySelectorAll("[data-sao-chep]").forEach((nut) => {
    nut.addEventListener("click", () => {
      const nhan = nut.querySelector("span");
      const xong = () => {
        nut.classList.add("da-chep");
        nhan.textContent = "Đã chép";
        setTimeout(() => { nut.classList.remove("da-chep"); nhan.textContent = "Sao chép"; }, 1500);
      };
      if (navigator.clipboard) navigator.clipboard.writeText(nut.dataset.saoChep).then(xong, xong);
      else xong();
    });
  });

  // Bấm "Tôi đã thanh toán": hiện hiệu ứng quét thành công rồi mới gửi form
  formXacNhan.addEventListener("submit", (e) => {
    if (formXacNhan.dataset.dangGui) return;
    e.preventDefault();
    formXacNhan.dataset.dangGui = "1";
    clearInterval(hen);
    nutXacNhan.disabled = true;
    nutXacNhan.innerHTML = '<span class="spinner-border spinner-border-sm" aria-hidden="true"></span> Đang xác nhận thanh toán…';
    daQuet.hidden = false;
    hopQr.classList.add("da-quet");
    setTimeout(() => formXacNhan.submit(), 1200);
  });

  batDau();
})();
