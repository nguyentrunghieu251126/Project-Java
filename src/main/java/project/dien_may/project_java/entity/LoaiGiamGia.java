package project.dien_may.project_java.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Giá trị khớp ràng buộc CK_KhuyenMai_Loai trong CSDL. */
@Getter
@RequiredArgsConstructor
public enum LoaiGiamGia {
    PHAN_TRAM("Phần trăm (%)"),
    SO_TIEN("Số tiền (₫)");

    private final String nhan;
}
