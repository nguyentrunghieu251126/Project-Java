package project.dien_may.project_java.dto.muahang;

import project.dien_may.project_java.entity.ChiTietDonHang;
import project.dien_may.project_java.entity.DonHang;
import project.dien_may.project_java.entity.ThanhToan;
import project.dien_may.project_java.entity.TrangThaiDonHang;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** TV3 – KH16 – KH18, KH22: dữ liệu trang chi tiết đơn / hóa đơn. */
public record ChiTietDonHangKhach(
        DonHang donHang,
        List<ChiTietDonHang> chiTiet,
        ThanhToan thanhToan,
        List<BuocDonHang> cacBuoc,
        Map<Integer, Integer> soSaoDaDanhGia
) {
    /** Mã hiển thị dạng DH000031. */
    public String maHien() {
        return String.format("DH%06d", donHang.getMaDonHang());
    }

    /** Thời gian giao dự kiến theo tỉnh / thành trong địa chỉ giao hàng của đơn. */
    public ThoiGianGiaoHang giaoHang() {
        return ThoiGianGiaoHang.theoDiaChi(donHang.getDiaChiGiaoHang());
    }

    /** Ngày nhận dự kiến (tính từ ngày đặt); null khi đơn đã giao xong hoặc đã hủy. */
    public String ngayNhanDuKien() {
        TrangThaiDonHang tt = donHang.getTrangThai();
        if (tt == TrangThaiDonHang.DA_HUY || tt == TrangThaiDonHang.DA_GIAO || donHang.getNgayGiao() != null || donHang.getNgayDat() == null) {
            return null;
        }
        return giaoHang().khoangNgay(donHang.getNgayDat().toLocalDate());
    }

    public BigDecimal tamTinh() {
        return chiTiet.stream().map(ChiTietDonHang::getThanhTien).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean duocHuy() {
        return donHang.getTrangThai() == TrangThaiDonHang.CHO_XAC_NHAN;
    }

    public boolean duocDanhGia() {
        return donHang.getTrangThai() == TrangThaiDonHang.DA_GIAO;
    }

    public boolean daHuy() {
        return donHang.getTrangThai() == TrangThaiDonHang.DA_HUY;
    }

    public boolean daDanhGia(Integer maSanPham) {
        return soSaoDaDanhGia.containsKey(maSanPham);
    }

    public Integer soSao(Integer maSanPham) {
        return soSaoDaDanhGia.get(maSanPham);
    }
}
