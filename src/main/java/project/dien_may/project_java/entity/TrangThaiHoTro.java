package project.dien_may.project_java.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Giá trị khớp ràng buộc CK_YCHT_TrangThai trong CSDL; mau dùng chung lớp nhãn với trạng thái đơn hàng. */
@Getter
@RequiredArgsConstructor
public enum TrangThaiHoTro {
    MOI("Đã gửi", "cho", "bi-send"),
    DANG_XU_LY("Đang xử lý", "xu-ly", "bi-hourglass-split"),
    DA_XU_LY("Đã phản hồi", "tot", "bi-check2-circle");

    private final String nhan;
    private final String mau;
    private final String icon;
}
