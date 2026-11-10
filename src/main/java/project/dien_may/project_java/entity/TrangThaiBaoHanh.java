package project.dien_may.project_java.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Giá trị khớp ràng buộc CK_PBH_TrangThai trong CSDL. */
@Getter
@RequiredArgsConstructor
public enum TrangThaiBaoHanh {
    CHO_TIEP_NHAN("Chờ tiếp nhận", "cho", "bi-hourglass-split"),
    DANG_BAO_HANH("Đang bảo hành", "xu-ly", "bi-tools"),
    HOAN_THANH("Hoàn thành", "tot", "bi-check2-circle"),
    TU_CHOI("Từ chối", "xau", "bi-x-circle");

    private final String nhan;
    private final String mau;
    private final String icon;
}
