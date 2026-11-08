package project.dien_may.project_java.config;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Các tab của menu khu quản lý theo đúng thứ tự hiển thị ở admin/layout.html, kèm quyền mở được tab.
 * Dùng để đưa người dùng tới tab đầu tiên họ được phép xem khi đăng nhập hoặc vào /admin.
 * Thêm, bớt hay đổi thứ tự mục trên menu thì sửa danh sách này cho khớp.
 */
public final class TabKhuQuanLy {

    public static final String TONG_QUAN = "/admin";

    private record Tab(String duongDan, Set<String> quyen) {
    }

    private static final List<Tab> DS_TAB = List.of(
            new Tab(TONG_QUAN, Set.of("TONGQUAN_XEM")),
            new Tab("/admin/bao-cao/doanh-thu", Set.of("BAOCAO_DOANHTHU")),
            new Tab("/admin/bao-cao/ban-chay", Set.of("BAOCAO_BANCHAY")),
            new Tab("/admin/bao-cao/ton-kho", Set.of("BAOCAO_TONKHO")),
            new Tab("/admin/san-pham", Set.of("SANPHAM_XEM")),
            new Tab("/admin/khuyen-mai", Set.of("KHUYENMAI_XEM")),
            new Tab("/admin/kho/don-hang", Set.of("DONHANG_XEM_TATCA")),
            new Tab("/admin/kho/ton-kho", Set.of("KHO_XEM_TONKHO")),
            new Tab("/admin/kho/yeu-cau-nhap", Set.of("KHO_DUYET_NHAP", "KHO_YEUCAU_NHAP")),
            new Tab("/admin/tai-khoan", Set.of("TAIKHOAN_XEM")),
            new Tab("/admin/phan-quyen", Set.of("PHANQUYEN_QUANLY")));

    private TabKhuQuanLy() {
    }

    /** Đường dẫn tab đầu tiên mở được; rỗng khi không được cấp tab nào. */
    public static Optional<String> tabDauTien(Collection<? extends GrantedAuthority> dsQuyen) {
        Set<String> coQuyen = dsQuyen.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        return DS_TAB.stream()
                .filter(tab -> tab.quyen().stream().anyMatch(coQuyen::contains))
                .map(Tab::duongDan)
                .findFirst();
    }
}
