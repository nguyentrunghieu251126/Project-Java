package project.dien_may.project_java.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** TV3 – loại giao dịch điểm thưởng, khớp CK_LichSuDiemThuong_Loai. */
@Getter
@RequiredArgsConstructor
public enum LoaiGiaoDichDiem {
    CONG("Cộng điểm", "+"),
    TRU("Trừ điểm", "−");

    private final String nhan;
    private final String dau;
}
