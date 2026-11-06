package project.dien_may.project_java.dto;

import java.math.BigDecimal;

public record SanPhamNoiBat(
        Integer maSanPham,
        String tenSanPham,
        String thuongHieu,
        BigDecimal donGia,
        Integer soLuong,
        String anhDaiDien,
        String nhom,
        boolean banChay,
        long soLuotDanhGia,
        String diemDanhGiaHienThi,
        BigDecimal giaGoc,
        Integer phanTramGiam
) {}
