package project.dien_may.project_java.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Giá trị khớp ràng buộc CK_YCNH_TrangThai trong CSDL. */
@Getter
@RequiredArgsConstructor
public enum TrangThaiYeuCauNhap {
    CHO_DUYET("Chờ duyệt", "cho", "bi-hourglass-split"),
    DA_DUYET("Đã duyệt", "xu-ly", "bi-check2"),
    TU_CHOI("Từ chối", "xau", "bi-x-circle"),
    DA_NHAP("Đã nhập kho", "tot", "bi-check2-circle");

    private final String nhan;
    private final String mau;
    private final String icon;
}
