package project.dien_may.project_java.dto.baocao;

import java.math.BigDecimal;

public record DoanhThuNgay(Integer nam, Integer thang, Integer ngay,
        Long soDon, BigDecimal tienGiam, BigDecimal doanhThu) {
}
