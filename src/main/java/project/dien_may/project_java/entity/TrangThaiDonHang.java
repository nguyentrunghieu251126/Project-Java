package project.dien_may.project_java.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Giá trị khớp ràng buộc CK_DonHang_TrangThai trong CSDL. */
@Getter
@RequiredArgsConstructor
public enum TrangThaiDonHang {
    CHO_XAC_NHAN("Chờ xác nhận", "cho", "bi-hourglass-split"),
    DA_XAC_NHAN("Đã xác nhận", "xu-ly", "bi-check2"),
    DA_XUAT_KHO("Đã xuất kho", "xu-ly", "bi-box-seam"),
    DANG_GIAO("Đang giao", "xu-ly", "bi-truck"),
    DA_GIAO("Đã giao", "tot", "bi-check2-circle"),
    DA_HUY("Đã hủy", "xau", "bi-x-circle");

    private final String nhan;
    private final String mau;
    private final String icon;
}
