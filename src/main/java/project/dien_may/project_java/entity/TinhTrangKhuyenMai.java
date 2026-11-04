package project.dien_may.project_java.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Tình trạng hiển thị của khuyến mãi, suy ra từ cờ TrangThai, ngày hiệu lực và số lượt. */
@Getter
@RequiredArgsConstructor
public enum TinhTrangKhuyenMai {
    DANG_CHAY("Đang chạy", "tot", "bi-play-circle"),
    SAP_DIEN_RA("Sắp diễn ra", "xu-ly", "bi-calendar-event"),
    TAM_DUNG("Tạm dừng", "cho", "bi-pause-circle"),
    HET_LUOT("Hết lượt", "trung-tinh", "bi-slash-circle"),
    HET_HAN("Hết hạn", "trung-tinh", "bi-clock-history");

    private final String nhan;
    private final String mau;
    private final String icon;
}
