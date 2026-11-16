package project.dien_may.project_java.dto.muahang;

import project.dien_may.project_java.entity.PhieuBaoHanh;
import project.dien_may.project_java.entity.TrangThaiBaoHanh;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TV3 – một sản phẩm khách đã nhận trên trang "Thông tin bảo hành" (/tai-khoan/bao-hanh).
 * Hạn bảo hành = ngày giao + tháng bảo hành chính hãng + tháng bảo hành mở rộng.
 */
public record ThongTinBaoHanh(Integer maChiTiet, Integer maDon, String tenSanPham, String hinhAnh,
                              String soSeri, LocalDateTime ngayMua, LocalDateTime ngayNhan,
                              int thangChinhHang, int thangMoRong, LocalDateTime ngayHetHan,
                              List<PhieuBaoHanh> lichSuSuaChua) {

    public String maDonHien() {
        return String.format("DH%06d", maDon);
    }

    /** Sản phẩm không có thời gian bảo hành (0 tháng). */
    public boolean khongBaoHanh() {
        return thangChinhHang + thangMoRong <= 0;
    }

    public boolean conHan() {
        return !khongBaoHanh() && !LocalDateTime.now().isAfter(ngayHetHan);
    }

    /** Có phiếu khách vừa gửi mà nhân viên kho chưa tiếp nhận. */
    public boolean choTiepNhan() {
        return lichSuSuaChua.stream().anyMatch(p -> p.getTrangThai() == TrangThaiBaoHanh.CHO_TIEP_NHAN);
    }

    /**
     * Hiện nút "Yêu cầu bảo hành": còn hạn và không có phiếu đang chờ tiếp nhận.
     * Phiếu đã được tiếp nhận (đang bảo hành / hoàn thành / từ chối) vẫn cho gửi yêu cầu mới.
     */
    public boolean duocYeuCau() {
        return conHan() && !choTiepNhan();
    }
}
