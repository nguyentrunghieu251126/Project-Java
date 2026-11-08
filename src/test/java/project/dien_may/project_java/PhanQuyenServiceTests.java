package project.dien_may.project_java;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.dien_may.project_java.entity.Quyen;
import project.dien_may.project_java.entity.TaiKhoan;
import project.dien_may.project_java.repository.QuyenRepository;
import project.dien_may.project_java.repository.TaiKhoanRepository;
import project.dien_may.project_java.service.admin.NghiepVuException;
import project.dien_may.project_java.service.admin.PhanQuyenService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Quy tắc phân quyền. Mỗi test tự rollback nên không để lại dữ liệu trong CSDL. */
@SpringBootTest
@Transactional
class PhanQuyenServiceTests {

    private static final String QUAN_TRI = "admin1@dienmay.vn";
    private static final String NHAN_VIEN = "kho1@dienmay.vn";
    private static final String KHACH_HANG = "lan.bui7@gmail.com";

    @Autowired
    private PhanQuyenService phanQuyenService;

    @Autowired
    private QuyenRepository quyenRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    private Integer maQuyen(String maChucNang) {
        return quyenRepository.findAll().stream()
                .filter(q -> maChucNang.equals(q.getMaChucNang()))
                .map(Quyen::getMaQuyen).findFirst().orElseThrow();
    }

    private TaiKhoan taiKhoan(String email) {
        return taiKhoanRepository.findByEmailIgnoreCase(email).orElseThrow();
    }

    @Test
    void tongQuanLaNhomRiengDungDauBangPhanQuyen() {
        var nhomDau = phanQuyenService.quyenTheoNhom().entrySet().iterator().next();
        assertEquals("Tổng quan", nhomDau.getKey());
        assertEquals(List.of("TONGQUAN_XEM"), nhomDau.getValue().stream().map(Quyen::getMaChucNang).toList());
    }

    private Set<String> quyenDangNhap(String email) {
        return userDetailsService.loadUserByUsername(email).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    /**
     * Đưa quyền toàn thể về trạng thái các test dựa vào: có SANPHAM_XEM, chưa có TONGQUAN_XEM.
     * Admin có thể đã đổi trên giao diện; chạy trong transaction của test nên được rollback.
     */
    @BeforeEach
    void datQuyenToanTheBanDau() {
        List<Integer> dsQuyen = new ArrayList<>(phanQuyenService.quyenToanTheNhanVien().stream()
                .filter(ma -> !ma.equals("TONGQUAN_XEM") && !ma.equals("SANPHAM_XEM")).map(this::maQuyen).toList());
        dsQuyen.add(maQuyen("SANPHAM_XEM"));
        phanQuyenService.capNhatToanTheNhanVien(dsQuyen);
    }

    /** Mã quyền của toàn thể nhân viên, bỏ một quyền và thêm một quyền. */
    private List<Integer> toanTheBo(String quyenBo, String quyenThem) {
        List<Integer> ketQua = new ArrayList<>(phanQuyenService.quyenToanTheNhanVien().stream()
                .filter(ma -> !ma.equals(quyenBo)).map(this::maQuyen).toList());
        ketQua.add(maQuyen(quyenThem));
        return ketQua;
    }

    @Test
    void quanTriMacNhienCoMoiQuyen() {
        Set<String> quyen = quyenDangNhap(QUAN_TRI);

        assertTrue(quyen.contains("ROLE_QUAN_TRI"));
        quyenRepository.findAll().forEach(q -> assertTrue(quyen.contains(q.getMaChucNang()), q.getMaChucNang()));
    }

    @Test
    void capThemVaThuHoiRiengChoMotNhanVien() {
        TaiKhoan nhanVien = taiKhoan(NHAN_VIEN);

        phanQuyenService.capNhatNhanVien(nhanVien.getMaTaiKhoan(), toanTheBo("SANPHAM_XEM", "TONGQUAN_XEM"));

        Set<String> quyen = quyenDangNhap(NHAN_VIEN);
        assertTrue(quyen.contains("TONGQUAN_XEM"));
        assertFalse(quyen.contains("SANPHAM_XEM"));
        assertEquals(Set.of("SANPHAM_XEM", "TONGQUAN_XEM"), phanQuyenService.quyenChinhRieng(nhanVien.getMaTaiKhoan()));
    }

    @Test
    void chinhRiengKhongAnhHuongNhanVienKhac() {
        TaiKhoan nhanVien = taiKhoan(NHAN_VIEN);
        TaiKhoan nguoiKhac = phanQuyenService.dsNhanVien().stream()
                .filter(t -> !t.getEmail().equals(NHAN_VIEN)).findFirst().orElseThrow();
        Set<String> truocKhiDoi = quyenDangNhap(nguoiKhac.getEmail());

        phanQuyenService.capNhatNhanVien(nhanVien.getMaTaiKhoan(), toanTheBo("SANPHAM_XEM", "TONGQUAN_XEM"));

        assertEquals(truocKhiDoi, quyenDangNhap(nguoiKhac.getEmail()));
    }

    @Test
    void tickGiongToanTheThiKhongConChinhRieng() {
        TaiKhoan nhanVien = taiKhoan(NHAN_VIEN);
        Integer ma = nhanVien.getMaTaiKhoan();
        phanQuyenService.capNhatNhanVien(ma, toanTheBo("SANPHAM_XEM", "TONGQUAN_XEM"));

        phanQuyenService.capNhatNhanVien(ma,
                phanQuyenService.quyenToanTheNhanVien().stream().map(this::maQuyen).toList());

        assertTrue(phanQuyenService.quyenChinhRieng(ma).isEmpty());
    }

    @Test
    void quyenToanTheApDungChoNguoiChuaChinhRieng() {
        phanQuyenService.capNhatToanTheNhanVien(toanTheBo("SANPHAM_XEM", "TONGQUAN_XEM"));

        Set<String> quyen = quyenDangNhap(NHAN_VIEN);
        assertTrue(quyen.contains("TONGQUAN_XEM"));
        assertFalse(quyen.contains("SANPHAM_XEM"));
    }

    @Test
    void khongChinhRiengChoTaiKhoanKhongPhaiNhanVien() {
        List<Integer> dsQuyen = List.of(maQuyen("SANPHAM_XEM"));

        assertThrows(ResponseStatusException.class,
                () -> phanQuyenService.capNhatNhanVien(taiKhoan(KHACH_HANG).getMaTaiKhoan(), dsQuyen));
        assertThrows(ResponseStatusException.class,
                () -> phanQuyenService.capNhatNhanVien(taiKhoan(QUAN_TRI).getMaTaiKhoan(), dsQuyen));
    }

    @Test
    void maQuyenKhongTonTaiBiTuChoi() {
        Integer ma = taiKhoan(NHAN_VIEN).getMaTaiKhoan();

        assertThrows(NghiepVuException.class, () -> phanQuyenService.capNhatNhanVien(ma, List.of(999999)));
        assertThrows(NghiepVuException.class, () -> phanQuyenService.capNhatToanTheNhanVien(List.of(999999)));
    }

    @Test
    void chuVietTatLayHaiTuCuoi() {
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setHoTen("Trần Minh Anh");
        assertEquals("MA", taiKhoan.getChuVietTat());
        taiKhoan.setHoTen("Hải");
        assertEquals("H", taiKhoan.getChuVietTat());
    }
}
