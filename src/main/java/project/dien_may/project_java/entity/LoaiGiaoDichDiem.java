package project.dien_may.project_java.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LoaiGiaoDichDiem {
    CONG("Cộng điểm", "+"),
    TRU("Trừ điểm", "−");

    private final String nhan;
    private final String dau;
}
