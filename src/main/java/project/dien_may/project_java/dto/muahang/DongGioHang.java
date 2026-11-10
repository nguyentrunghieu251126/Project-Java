package project.dien_may.project_java.dto.muahang;

import java.math.BigDecimal;

/** TV3 – một dòng giỏ hàng để hiển thị (giá theo giá hiện tại của sản phẩm). */
public record DongGioHang(
        Integer maSanPham,
        String tenSanPham,
        String thuongHieu,
        Integer thoiGianBaoHanh,
        String anh,
        BigDecimal donGia,
        int soLuong,
        int tonKho,
        boolean dangKinhDoanh,
        String danhMuc
) {
    /** Dùng cho ưu đãi thanh toán chỉ áp dụng với laptop. */
    public boolean laLaptop() {
        return danhMuc != null && danhMuc.trim().equalsIgnoreCase("Laptop");
    }

    private static final int NGUONG_SAP_HET = 5;

    public BigDecimal thanhTien() {
        return donGia.multiply(BigDecimal.valueOf(soLuong));
    }

    /** Còn bán và đủ hàng cho số lượng đang chọn. */
    public boolean muaDuoc() {
        return dangKinhDoanh && tonKho >= soLuong && soLuong > 0;
    }

    public boolean hetHang() {
        return !dangKinhDoanh || tonKho <= 0;
    }

    public boolean sapHet() {
        return !hetHang() && tonKho <= NGUONG_SAP_HET;
    }

    /** Số lượng tối đa được chọn trên ô số lượng. */
    public int toiDa() {
        return Math.max(tonKho, 1);
    }
}
