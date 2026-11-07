package project.dien_may.project_java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import project.dien_may.project_java.config.NhomQuyen;
import project.dien_may.project_java.entity.Quyen;
import project.dien_may.project_java.repository.QuyenRepository;
import project.dien_may.project_java.service.admin.NghiepVuException;
import project.dien_may.project_java.service.admin.PhanQuyenService;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Quy tắc gán quyền theo vai trò. Chỉ thử trường hợp bị từ chối nên không ghi gì vào CSDL. */
@SpringBootTest
class PhanQuyenServiceTests {

    private static final int MA_NHAN_VIEN_KHO = 2;

    @Autowired
    private PhanQuyenService phanQuyenService;

    @Autowired
    private QuyenRepository quyenRepository;

    @ParameterizedTest
    @CsvSource({
            "QUAN_TRI, PHANQUYEN_QUANLY, true",
            "NHAN_VIEN_KHO, PHANQUYEN_QUANLY, false",
            "KHACH_HANG, TAIKHOAN_XEM, false",
            "QUAN_TRI, KHO_DUYET_NHAP, true",
            "NHAN_VIEN_KHO, KHO_DUYET_NHAP, false",
            "KHACH_HANG, GIOHANG_QUANLY, true",
            "QUAN_TRI, GIOHANG_QUANLY, false",
            "NHAN_VIEN_KHO, HOTRO_GUI, false",
            "QUAN_TRI, TAIKHOAN_CANHAN, true",
            "NHAN_VIEN_KHO, TAIKHOAN_CANHAN, true",
            "KHACH_HANG, TAIKHOAN_CANHAN, true",
            "QUAN_TRI, SANPHAM_SUA, true",
            "NHAN_VIEN_KHO, SANPHAM_SUA, true",
            "KHACH_HANG, SANPHAM_SUA, false"
    })
    void choPhepGanTheoBangNhomQuyen(String vaiTro, String maChucNang, boolean duocPhep) {
        assertEquals(duocPhep, NhomQuyen.choPhepGan(vaiTro, maChucNang));
    }

    @Test
    void capQuyenKhongHopLeBiTuChoiVaKhongLuuGi() {
        Integer maPhanQuyen = quyenRepository.findAll().stream()
                .filter(q -> "PHANQUYEN_QUANLY".equals(q.getMaChucNang()))
                .map(Quyen::getMaQuyen).findFirst().orElseThrow();
        Map<Integer, Set<Integer>> truocKhiGui = phanQuyenService.quyenDaCap();

        NghiepVuException loi = assertThrows(NghiepVuException.class,
                () -> phanQuyenService.capNhat(List.of(MA_NHAN_VIEN_KHO + ":" + maPhanQuyen)));

        assertTrue(loi.getMessage().startsWith("Không thể cấp quyền"));
        assertEquals(truocKhiGui, phanQuyenService.quyenDaCap());
    }

    @Test
    void oBiKhoaKhopVoiNhomQuyen() {
        Integer maPhanQuyen = quyenRepository.findAll().stream()
                .filter(q -> "PHANQUYEN_QUANLY".equals(q.getMaChucNang()))
                .map(Quyen::getMaQuyen).findFirst().orElseThrow();

        assertEquals("Chỉ quản trị viên", phanQuyenService.oBiKhoa().get(MA_NHAN_VIEN_KHO).get(maPhanQuyen));
    }
}
