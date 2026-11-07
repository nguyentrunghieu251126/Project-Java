package project.dien_may.project_java.config;

import java.util.Set;

/**
 * Quy tắc vai trò nào được gán quyền nào, dùng chung cho kiểm tra ở server và ô khóa trên màn hình phân quyền.
 * Mã chức năng không thuộc các nhóm dưới đây là quyền khu quản lý, dành cho quản trị viên và nhân viên kho.
 */
public final class NhomQuyen {

    public static final String QUAN_TRI = "QUAN_TRI";
    public static final String NHAN_VIEN_KHO = "NHAN_VIEN_KHO";
    public static final String KHACH_HANG = "KHACH_HANG";

    /** KHO_DUYET_NHAP nằm ở đây vì nhân viên kho là người gửi yêu cầu, không được tự duyệt yêu cầu của mình. */
    private static final Set<String> CHI_QUAN_TRI = Set.of(
            "PHANQUYEN_QUANLY", "TAIKHOAN_XEM", "TAIKHOAN_THEM", "TAIKHOAN_SUA", "TAIKHOAN_XOA", "KHO_DUYET_NHAP");

    private static final Set<String> CHI_KHACH_HANG = Set.of(
            "GIOHANG_QUANLY", "DONHANG_DATHANG", "DONHANG_XEM_CANHAN", "DONHANG_HUY",
            "BAOHANH_YEUCAU", "DOITRA_YEUCAU", "DANHGIA_VIET", "HOTRO_GUI");

    private static final String MOI_VAI_TRO = "TAIKHOAN_CANHAN";

    private NhomQuyen() {
    }

    public static boolean choPhepGan(String tenVaiTro, String maChucNang) {
        return lyDoKhoa(tenVaiTro, maChucNang) == null;
    }

    /** @return lý do vai trò không được gán quyền này, null nếu được phép */
    public static String lyDoKhoa(String tenVaiTro, String maChucNang) {
        if (MOI_VAI_TRO.equals(maChucNang)) {
            return null;
        }
        if (CHI_QUAN_TRI.contains(maChucNang)) {
            return QUAN_TRI.equals(tenVaiTro) ? null : "Chỉ quản trị viên";
        }
        if (CHI_KHACH_HANG.contains(maChucNang)) {
            return KHACH_HANG.equals(tenVaiTro) ? null : "Chỉ dành cho khách hàng";
        }
        return QUAN_TRI.equals(tenVaiTro) || NHAN_VIEN_KHO.equals(tenVaiTro) ? null : "Chỉ dành cho khu quản lý";
    }
}
