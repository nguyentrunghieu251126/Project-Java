package project.dien_may.project_java.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LoaiGiamGia {
    PHAN_TRAM("Phần trăm (%)"),
    SO_TIEN("Số tiền (₫)");

    private final String nhan;
}
