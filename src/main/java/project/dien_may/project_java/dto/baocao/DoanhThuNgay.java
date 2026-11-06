package project.dien_may.project_java.dto.baocao;

import java.math.BigDecimal;

/** Doanh thu gộp theo một ngày, là kết quả truy vấn thô trước khi gộp thành kỳ báo cáo. */
public record DoanhThuNgay(Integer nam, Integer thang, Integer ngay,
                           Long soDon, BigDecimal tienGiam, BigDecimal doanhThu) {
}
