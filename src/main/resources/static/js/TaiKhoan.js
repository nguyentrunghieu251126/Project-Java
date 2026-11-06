"use strict";
document.querySelectorAll(".account-toggle-password[data-target]").forEach((button) => {
  button.addEventListener("click", () => {
    const input = document.getElementById(button.dataset.target);
    if (!input) return;
    const visible = input.type === "password";
    input.type = visible ? "text" : "password";
    button.setAttribute("aria-label", (visible ? "Ẩn" : "Hiện") + " mật khẩu");
    const icon = button.querySelector("i");
    if (icon) icon.className = visible ? "bi bi-eye-slash" : "bi bi-eye";
  });
});

const accountPasswordForm = document.getElementById("accountPasswordForm");
if (accountPasswordForm) {
  accountPasswordForm.addEventListener("submit", (event) => {
    const newPassword = document.getElementById("matKhauMoi");
    const confirmation = document.getElementById("xacNhanMatKhau");
    const error = document.getElementById("clientPasswordError");
    if (!newPassword || !confirmation || !error) return;
    error.hidden = true;
    error.textContent = "";
    if (newPassword.value !== confirmation.value) {
      event.preventDefault();
      error.textContent = "Xác nhận mật khẩu mới không khớp.";
      error.hidden = false;
      confirmation.focus();
    }
  });
}


// Tự hiện và tự ẩn thông báo sau khi lưu, giống alert đăng nhập ở trang chủ.
const accountSuccessAlert = document.getElementById("accountSuccessAlert");
if (accountSuccessAlert) {
  requestAnimationFrame(() => accountSuccessAlert.classList.add("show"));
  let closed = false;
  function closeAccountSuccess() {
    if (closed) return;
    closed = true;
    accountSuccessAlert.classList.remove("show");
    window.setTimeout(() => accountSuccessAlert.remove(), 200);
  }
  const closeButton = accountSuccessAlert.querySelector(".btn-close");
  if (closeButton) closeButton.addEventListener("click", closeAccountSuccess);
  window.setTimeout(closeAccountSuccess, 3500);
}
