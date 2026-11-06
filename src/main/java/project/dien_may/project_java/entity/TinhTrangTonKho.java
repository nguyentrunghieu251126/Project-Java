package project.dien_may.project_java.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** "Sắp hết" là còn hàng nhưng dưới MucTonToiThieu. */
@Getter
@RequiredArgsConstructor
public enum TinhTrangTonKho {
    HET("Hết hàng", "xau", "bi-x-circle"),
    SAP_HET("Sắp hết", "cho", "bi-exclamation-triangle"),
    DU("Đủ hàng", "tot", "bi-check2-circle");

    private final String nhan;
    private final String mau;
    private final String icon;
}
