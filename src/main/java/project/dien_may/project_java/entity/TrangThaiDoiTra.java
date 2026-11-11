package project.dien_may.project_java.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Giá trị khớp ràng buộc CK_PDT_TrangThai trong CSDL. */
@Getter
@RequiredArgsConstructor
public enum TrangThaiDoiTra {
    CHO_TIEP_NHAN("Chờ tiếp nhận", "cho", "bi-hourglass-split"),
    CHAP_NHAN("Đã chấp nhận", "xu-ly", "bi-check2"),
    HOAN_THANH("Hoàn thành", "tot", "bi-check2-circle"),
    TU_CHOI("Từ chối", "xau", "bi-x-circle");

    private final String nhan;
    private final String mau;
    private final String icon;
}
