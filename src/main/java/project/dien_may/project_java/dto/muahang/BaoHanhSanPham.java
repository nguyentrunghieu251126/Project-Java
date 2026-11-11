package project.dien_may.project_java.dto.muahang;

import java.time.LocalDate;

/** TV3 – một sản phẩm đã mua và hạn bảo hành (khung "Thông tin bảo hành" ở trang Tổng quan). */
public record BaoHanhSanPham(Integer maDon, String tenSanPham, String hinhAnh, LocalDate ngayNhan,
                             int soThang, LocalDate hanBaoHanh) {

    public String maHien() {
        return String.format("DH%06d", maDon);
    }

    public boolean conHan() {
        return !hanBaoHanh.isBefore(LocalDate.now());
    }
}
