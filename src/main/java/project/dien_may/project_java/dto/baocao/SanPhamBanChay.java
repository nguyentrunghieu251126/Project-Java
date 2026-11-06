package project.dien_may.project_java.dto.baocao;

import java.math.BigDecimal;

public record SanPhamBanChay(Integer maSanPham, String tenSanPham, String maModel, String tenDanhMuc,
                             Long soLuongBan, BigDecimal doanhSo) {
}
