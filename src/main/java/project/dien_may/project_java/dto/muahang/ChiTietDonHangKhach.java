package project.dien_may.project_java.dto.muahang;

import project.dien_may.project_java.entity.ChiTietDonHang;
import project.dien_may.project_java.entity.DonHang;
import project.dien_may.project_java.entity.LichSuDiemThuong;
import project.dien_may.project_java.entity.LoaiGiaoDichDiem;
import project.dien_may.project_java.entity.ThanhToan;
import project.dien_may.project_java.entity.TrangThaiDonHang;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ChiTietDonHangKhach(
        DonHang donHang,
        List<ChiTietDonHang> chiTiet,
        ThanhToan thanhToan,
        List<BuocDonHang> cacBuoc,
        Map<Integer, Integer> soSaoDaDanhGia,
        List<LichSuDiemThuong> lichSuDiem) {
    public int diemDaDung() {
        return lichSuDiem.stream().filter(l -> l.getLoaiGiaoDich() == LoaiGiaoDichDiem.TRU
                && l.getMoTa() != null && l.getMoTa().startsWith(QuyTacDiemThuong.MO_TA_DUNG))
                .mapToInt(LichSuDiemThuong::getSoDiem).sum();
    }

    public BigDecimal tienGiamBangDiem() {
        return QuyTacDiemThuong.tienGiam(diemDaDung());
    }

    public int diemTichLuy() {
        return lichSuDiem.stream().filter(l -> l.getLoaiGiaoDich() == LoaiGiaoDichDiem.CONG
                && l.getMoTa() != null && l.getMoTa().startsWith(QuyTacDiemThuong.MO_TA_TICH))
                .mapToInt(LichSuDiemThuong::getSoDiem).sum();
    }

    public String maHien() {
        return String.format("DH%06d", donHang.getMaDonHang());
    }

    public ThoiGianGiaoHang giaoHang() {
        return ThoiGianGiaoHang.theoDiaChi(donHang.getDiaChiGiaoHang());
    }

    public String ngayNhanDuKien() {
        TrangThaiDonHang tt = donHang.getTrangThai();
        if (tt == TrangThaiDonHang.DA_HUY || tt == TrangThaiDonHang.DA_GIAO || donHang.getNgayGiao() != null
                || donHang.getNgayDat() == null) {
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
