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

// Đăng ký nhận tin
document.getElementById("newsletterForm").addEventListener("submit", (e) => {
  e.preventDefault();
  toastMsg.textContent = "Đăng ký thành công! Voucher đã được gửi tới email của bạn.";
  toast.show();
  e.target.reset();
});

// Nút lên đầu trang
const backToTop = document.getElementById("backToTop");
window.addEventListener("scroll", () => {
  backToTop.classList.toggle("show", window.scrollY > 400);
});
backToTop.addEventListener("click", () => window.scrollTo({ top: 0, behavior: "smooth" }));
