package project.dien_may.project_java.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Giá trị khớp ràng buộc CK_ThanhToan_TrangThai trong CSDL. */
@Getter
@RequiredArgsConstructor
public enum TrangThaiThanhToan {
    CHUA_THANH_TOAN("Chưa thanh toán", "cho"),
    DA_THANH_TOAN("Đã thanh toán", "tot"),
    DA_HOAN_TIEN("Đã hoàn tiền", "xu-ly");

    private final String nhan;
    private final String mau;
}
