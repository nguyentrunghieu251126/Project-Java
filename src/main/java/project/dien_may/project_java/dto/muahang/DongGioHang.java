package project.dien_may.project_java.dto.muahang;

import java.math.BigDecimal;

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
        String danhMuc) {
    public boolean laLaptop() {
        return danhMuc != null && danhMuc.trim().equalsIgnoreCase("Laptop");
    }

    private static final int NGUONG_SAP_HET = 5;

    public BigDecimal thanhTien() {
        return donGia.multiply(BigDecimal.valueOf(soLuong));
    }

    public boolean muaDuoc() {
        return dangKinhDoanh && tonKho >= soLuong && soLuong > 0;
    }

    public boolean hetHang() {
        return !dangKinhDoanh || tonKho <= 0;
    }

    public boolean sapHet() {
        return !hetHang() && tonKho <= NGUONG_SAP_HET;
    }

    public int toiDa() {
        return Math.max(tonKho, 1);
    }
}
