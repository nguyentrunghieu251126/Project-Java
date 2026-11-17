// Thêm vào giỏ hàng
let cartCount = 0;
const cartBadge = document.getElementById("cartCount");
const cartLink = document.querySelector(".cart-link");
const toast = new bootstrap.Toast(document.getElementById("cartToast"), { delay: 2000 });
const toastMsg = document.getElementById("toastMsg");

document.querySelectorAll(".add-cart").forEach((btn) => {
  btn.addEventListener("click", () => {
    cartCount++;
    cartBadge.textContent = cartCount;
    cartLink.classList.remove("bump");
    void cartLink.offsetWidth;
    cartLink.classList.add("bump");

    const name = btn.closest(".product-card").querySelector("h6").textContent;
    toastMsg.textContent = `Đã thêm "${name}" vào giỏ hàng!`;
    toast.show();
  });
});

// Đồng hồ đếm ngược deal sốc + flash sale (đến hết ngày)
function updateCountdown() {
  const now = new Date();
  const end = new Date(now);
  end.setHours(23, 59, 59, 999);
  const diff = Math.max(0, end - now);
  const pad = (n) => String(n).padStart(2, "0");
  const setAll = (sel, val) => document.querySelectorAll(sel).forEach((el) => (el.textContent = pad(val)));
  setAll(".cd-h", Math.floor(diff / 3600000));
  setAll(".cd-m", Math.floor((diff % 3600000) / 60000));
  setAll(".cd-s", Math.floor((diff % 60000) / 1000));
}
updateCountdown();
setInterval(updateCountdown, 1000);

// Tabs lọc (chỉ đổi trạng thái active)
document.querySelectorAll(".filter-tabs .nav-link").forEach((tab) => {
  tab.addEventListener("click", () => {
    document.querySelectorAll(".filter-tabs .nav-link").forEach((t) => t.classList.remove("active"));
    tab.classList.add("active");
  });
});

// Nút lên đầu trang
const backToTop = document.getElementById("backToTop");
window.addEventListener("scroll", () => {
  backToTop.classList.toggle("show", window.scrollY > 400);
});
backToTop.addEventListener("click", () => window.scrollTo({ top: 0, behavior: "smooth" }));


// Thong bao dang nhap va dang xuat
document.addEventListener("DOMContentLoaded", function () {
  const url = new URL(window.location.href);
  let alertId = null;

  if (url.searchParams.has("dangxuat")) {
    alertId = "logoutSuccessAlert";
    url.searchParams.delete("dangxuat");
  } else if (url.searchParams.has("dangnhap")) {
    alertId = "loginSuccessAlert";
    url.searchParams.delete("dangnhap");
  }

  if (!alertId) {
    return;
  }

  const alertElement = document.getElementById(alertId);

  window.history.replaceState(
      {},
      "",
      url.pathname + url.search + url.hash
  );

  if (!alertElement) {
    return;
  }

  alertElement.style.display = "block";

  requestAnimationFrame(function () {
    alertElement.classList.add("show");
  });

  let daDong = false;

  function dongThongBao() {
    if (daDong) {
      return;
    }

    daDong = true;
    alertElement.classList.remove("show");

    setTimeout(function () {
      alertElement.remove();
    }, 200);
  }

  const nutDong = alertElement.querySelector(".btn-close");

  if (nutDong) {
    nutDong.addEventListener("click", dongThongBao);
  }

  setTimeout(dongThongBao, 3000);
});
(function () {
  function khoiTaoCarousel(gridId, prevId, nextId) {
    const track = document.getElementById(gridId);
    const prev = document.getElementById(prevId);
    const next = document.getElementById(nextId);
    if (!track || !prev || !next) return;
    const visibleCards = () => [...track.children].filter((card) =>
        !card.hidden && getComputedStyle(card).display !== 'none');
    function capNhatNut() {
      const maxScroll = Math.max(0, track.scrollWidth - track.clientWidth);
      const isScrollable = maxScroll > 3;
      prev.hidden = !isScrollable;
      next.hidden = !isScrollable;
      prev.disabled = track.scrollLeft <= 2;
      next.disabled = track.scrollLeft >= maxScroll - 2;}
    function buocTruot() {
      const cards = visibleCards();
      if (cards.length < 2) return track.clientWidth;
      return Math.abs(cards[1].offsetLeft - cards[0].offsetLeft) || track.clientWidth;}
    function chuyenHuong(direction) {
      track.scrollBy({
        left: direction * buocTruot(),
        behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'
      });
    }
    prev.addEventListener('click', () => chuyenHuong(-1));
    next.addEventListener('click', () => chuyenHuong(1));
    track.addEventListener('scroll', capNhatNut, { passive: true });
    window.addEventListener('resize', capNhatNut);
    track.addEventListener('product-carousel-filter', () => {
      track.scrollLeft = 0;
      requestAnimationFrame(capNhatNut);
    });
    window.addEventListener('load', capNhatNut);
    requestAnimationFrame(capNhatNut);}
  khoiTaoCarousel('flashSaleGrid', 'flashSalePrev', 'flashSaleNext');
  khoiTaoCarousel('bestSellerGrid', 'bestSellerPrev', 'bestSellerNext');
})();
