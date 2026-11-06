(function () {
  "use strict";
  if (!document.getElementById("product-compare")) return;

  const backToTop = document.getElementById("backToTop");
  if (backToTop) {
    const updateBackToTop = () => backToTop.classList.toggle("show", window.scrollY > 400);
    window.addEventListener("scroll", updateBackToTop, { passive: true });
    backToTop.addEventListener("click", () => window.scrollTo({ top: 0, behavior: "smooth" }));
    updateBackToTop();
  }

  const url = new URL(window.location.href);
  let alertId = null;
  if (url.searchParams.has("dangxuat")) {
    alertId = "logoutSuccessAlert";
    url.searchParams.delete("dangxuat");
  } else if (url.searchParams.has("dangnhap")) {
    alertId = "loginSuccessAlert";
    url.searchParams.delete("dangnhap");
  }
  if (!alertId) return;

  window.history.replaceState({}, "", url.pathname + url.search + url.hash);
  const alertElement = document.getElementById(alertId);
  if (!alertElement) return;
  alertElement.style.display = "block";
  requestAnimationFrame(() => alertElement.classList.add("show"));

  let closed = false;
  const close = () => {
    if (closed) return;
    closed = true;
    alertElement.classList.remove("show");
    setTimeout(() => alertElement.remove(), 200);
  };
  alertElement.querySelector(".btn-close")?.addEventListener("click", close);
  setTimeout(close, 3000);
})();
