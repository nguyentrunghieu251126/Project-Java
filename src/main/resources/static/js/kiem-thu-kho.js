(() => {
    const root = document.querySelector(".kiem-thu-kho");
    if (!root) return;

    const sanPham = document.getElementById("sanPhamDemo");
    const anhSanPham = document.getElementById("anhSanPhamDemo");
    const anhTrong = document.getElementById("anhSanPhamTrong");
    const tonKho = document.getElementById("tonKhoThucTe");
    const nhatKy = document.getElementById("nhatKyMuaHang");
    const trangThai = document.getElementById("trangThaiTacVu");
    const nutLamMoi = document.getElementById("lamMoiTonKho");
    const nutMua = document.getElementById("muaMotSanPham");
    const nutDongThoi = document.getElementById("giaLapDongThoi");
    const csrf = root.dataset.csrfToken || "";

    const themDong = (noiDung, thanhCong, luongXuLy) => {
        const dong = document.createElement("li");
        dong.className = thanhCong ? "terminal-thanh-cong" : "terminal-that-bai";
        dong.textContent = `${noiDung}${luongXuLy ? ` | Luồng xử lý: ${luongXuLy}` : ""}`;
        nhatKy.appendChild(dong);
        nhatKy.scrollTop = nhatKy.scrollHeight;
    };

    const xoaGoiY = () => {
        const goiY = nhatKy.querySelector(".terminal-goi-y");
        if (goiY) goiY.remove();
    };

    const capNhatAnhSanPham = () => {
        const luaChon = sanPham.selectedOptions[0];
        const duongDanAnh = luaChon?.dataset.anh || "";
        anhSanPham.onerror = () => {
            anhSanPham.hidden = true;
            anhTrong.hidden = false;
        };
        anhSanPham.hidden = !duongDanAnh;
        anhTrong.hidden = Boolean(duongDanAnh);
        anhSanPham.alt = luaChon?.textContent.trim() || "";
        if (duongDanAnh) anhSanPham.src = duongDanAnh;
        else anhSanPham.removeAttribute("src");
    };

    const docJson = async (response) => {
        let body;
        try {
            body = await response.json();
        } catch {
            throw new Error("Máy chủ không trả về kết quả hợp lệ. Vui lòng thử lại.");
        }
        if (!response.ok && response.status !== 409) {
            throw new Error(body.thongDiep || body.message || body.error || "Không thể hoàn tất yêu cầu.");
        }
        return body;
    };

    const lamMoiTonKho = async () => {
        const maSanPham = sanPham.value;
        if (!maSanPham) return;
        nutLamMoi.disabled = true;
        trangThai.textContent = "Đang lấy số lượng mới nhất...";
        try {
            const response = await fetch(`${root.dataset.tonKhoUrl}/${encodeURIComponent(maSanPham)}`, {
                cache: "no-store",
                headers: { Accept: "application/json" }
            });
            const duLieu = await docJson(response);
            tonKho.textContent = `${duLieu.tonKho} sản phẩm — ${duLieu.tenSanPham}`;
            trangThai.textContent = "Đã cập nhật từ dữ liệu trong hệ thống.";
        } catch (error) {
            trangThai.textContent = error.message;
            themDong(`Không thể làm mới tồn kho: ${error.message}`, false);
        } finally {
            nutLamMoi.disabled = false;
        }
    };

    const muaMotSanPham = async (nguoiMua) => {
        const response = await fetch(root.dataset.muaUrl, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json",
                "X-CSRF-TOKEN": csrf
            },
            body: JSON.stringify({
                maSanPham: Number(sanPham.value),
                nguoiMua
            })
        });
        const duLieu = await docJson(response);
        const dong = duLieu.thanhCong
            ? `${duLieu.nguoiMua} đã mua ${duLieu.soLuong} sản phẩm; trong kho còn ${duLieu.tonKho}.`
            : `${duLieu.nguoiMua} không mua được: ${duLieu.thongDiep} Trong kho còn ${duLieu.tonKho}.`;
        themDong(dong, duLieu.thanhCong, duLieu.luongXuLy);
        tonKho.textContent = `${duLieu.tonKho} sản phẩm`;
        return duLieu;
    };

    nutLamMoi.addEventListener("click", lamMoiTonKho);
    sanPham.addEventListener("change", () => {
        capNhatAnhSanPham();
        lamMoiTonKho();
    });
    nutMua.addEventListener("click", async () => {
        nutMua.disabled = true;
        trangThai.textContent = "Đang xử lý yêu cầu mua hàng...";
        xoaGoiY();
        try {
            await muaMotSanPham("Khách hàng trên web");
            trangThai.textContent = "Đã xử lý xong yêu cầu mua hàng.";
        } catch (error) {
            trangThai.textContent = error.message;
            themDong(`Không thể đặt mua: ${error.message}`, false);
        } finally {
            nutMua.disabled = false;
        }
    });

    nutDongThoi.addEventListener("click", async () => {
        nutDongThoi.disabled = true;
        nutMua.disabled = true;
        nutLamMoi.disabled = true;
        trangThai.textContent = "Đang gửi đồng thời 15 yêu cầu mua hàng...";
        xoaGoiY();
        try {
            const ketQua = await Promise.allSettled(
                Array.from({ length: 15 }, (_, index) =>
                    muaMotSanPham(`Khách hàng ${String(index + 1).padStart(2, "0")}`))
            );
            const loi = ketQua.filter((item) => item.status === "rejected").length;
            trangThai.textContent = loi
                ? `Đã gửi đủ 15 yêu cầu; ${loi} yêu cầu gặp lỗi kết nối.`
                : "Đã xử lý xong 15 yêu cầu mua đồng thời.";
            if (loi) {
                ketQua.filter((item) => item.status === "rejected").forEach((item) => {
                    themDong(`Không thể hoàn tất yêu cầu: ${item.reason.message}`, false);
                });
            }
        } finally {
            nutDongThoi.disabled = false;
            nutMua.disabled = false;
            nutLamMoi.disabled = false;
            await lamMoiTonKho();
        }
    });

    capNhatAnhSanPham();
    if (sanPham.value) lamMoiTonKho();
})();
