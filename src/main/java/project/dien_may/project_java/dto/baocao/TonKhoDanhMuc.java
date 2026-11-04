package project.dien_may.project_java.dto.baocao;

import java.math.BigDecimal;

public record TonKhoDanhMuc(String tenDanhMuc, Long soSanPham, Long soLuong, BigDecimal giaTri, Long soCanNhap) {
}
