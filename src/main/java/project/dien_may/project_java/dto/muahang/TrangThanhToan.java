package project.dien_may.project_java.dto.muahang;

import java.math.BigDecimal;
import java.util.List;

/** TV3 – dữ liệu dựng trang thanh toán. */
public record TrangThanhToan(
        List<DongGioHang> dong,
        BigDecimal tamTinh,
        List<MaUuDai> maDungDuoc,
        List<MaUuDai> maSapCo,
        List<MaUuDai> maHetHan,
        List<LuaChonThanhToan> phuongThuc,
        long diemHienCo
) {
    public int soSanPham() {
        return dong.size();
    }

    public boolean coDongKhongMuaDuoc() {
        return dong.stream().anyMatch(d -> !d.muaDuoc());
    }
}
