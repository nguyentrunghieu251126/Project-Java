package project.dien_may.project_java.dto.muahang;

import project.dien_may.project_java.entity.PhuongThucThanhToan;

import java.math.BigDecimal;
import java.util.List;

/**
 * TV3 – một phương thức thanh toán ở trang thanh toán.
 *
 * @param uuDai    các điều kiện ưu đãi lần đầu; rỗng nếu phương thức không có ưu đãi hoặc khách đã dùng
 * @param tienGiam số tiền đơn này được giảm khi chọn phương thức (0 nếu chưa đủ điều kiện)
 */
public record LuaChonThanhToan(PhuongThucThanhToan phuongThuc, List<UuDaiThanhToan.DieuKien> uuDai, BigDecimal tienGiam) {

    public boolean coUuDai() {
        return !uuDai.isEmpty();
    }

    /** Chữ "Laptop từ 15 triệu giảm 150K · Đơn từ 6 triệu giảm 100K". */
    public String moTaUuDai() {
        return String.join(" · ", uuDai.stream().map(UuDaiThanhToan.DieuKien::moTa).toList());
    }
}
