package project.dien_may.project_java.dto.muahang;

import project.dien_may.project_java.entity.ChiTietDonHang;
import project.dien_may.project_java.entity.DonHang;

import java.util.List;

/** TV3 – KH19: một thẻ đơn hàng trong lịch sử mua hàng (hiện tối đa 2 sản phẩm đầu). */
public record DonHangTomTat(DonHang donHang, List<ChiTietDonHang> chiTiet) {

    public static final int SO_DONG_XEM_TRUOC = 2;

    /** Mã hiển thị dạng DH000031. */
    public String maHien() {
        return String.format("DH%06d", donHang.getMaDonHang());
    }

    public List<ChiTietDonHang> xemTruoc() {
        return chiTiet.size() <= SO_DONG_XEM_TRUOC ? chiTiet : chiTiet.subList(0, SO_DONG_XEM_TRUOC);
    }

    public int soDongAn() {
        return Math.max(0, chiTiet.size() - SO_DONG_XEM_TRUOC);
    }
}
