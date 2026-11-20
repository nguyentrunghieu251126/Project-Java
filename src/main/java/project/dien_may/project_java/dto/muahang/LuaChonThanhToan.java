package project.dien_may.project_java.dto.muahang;

import project.dien_may.project_java.entity.PhuongThucThanhToan;
import java.math.BigDecimal;
import java.util.List;

public record LuaChonThanhToan(PhuongThucThanhToan phuongThuc, List<UuDaiThanhToan.DieuKien> uuDai,
        BigDecimal tienGiam) {

    public boolean coUuDai() {
        return !uuDai.isEmpty();
    }

    public String moTaUuDai() {
        return String.join(" · ", uuDai.stream().map(UuDaiThanhToan.DieuKien::moTa).toList());
    }
}
