package project.dien_may.project_java.dto.muahang;

import project.dien_may.project_java.entity.PhieuBaoHanh;
import project.dien_may.project_java.entity.TrangThaiBaoHanh;
import java.time.LocalDateTime;
import java.util.List;

public record ThongTinBaoHanh(Integer maChiTiet, Integer maDon, String tenSanPham, String hinhAnh,
        String soSeri, LocalDateTime ngayMua, LocalDateTime ngayNhan,
        int thangChinhHang, int thangMoRong, LocalDateTime ngayHetHan,
        List<PhieuBaoHanh> lichSuSuaChua) {

    public String maDonHien() {
        return String.format("DH%06d", maDon);
    }

    public boolean khongBaoHanh() {
        return thangChinhHang + thangMoRong <= 0;
    }

    public boolean conHan() {
        return !khongBaoHanh() && !LocalDateTime.now().isAfter(ngayHetHan);
    }

    public boolean choTiepNhan() {
        return lichSuSuaChua.stream().anyMatch(p -> p.getTrangThai() == TrangThaiBaoHanh.CHO_TIEP_NHAN);
    }

    public boolean duocYeuCau() {
        return conHan() && !choTiepNhan();
    }
}
