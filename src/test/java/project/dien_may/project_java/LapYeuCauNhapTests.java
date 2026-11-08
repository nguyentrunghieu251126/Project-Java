package project.dien_may.project_java;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.entity.TinhTrangTonKho;
import project.dien_may.project_java.entity.TrangThaiYeuCauNhap;
import project.dien_may.project_java.entity.YeuCauNhapHang;
import project.dien_may.project_java.repository.ChiTietYeuCauNhapHangRepository;
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.repository.YeuCauNhapHangRepository;
import project.dien_may.project_java.service.admin.KhoAdminService;
import project.dien_may.project_java.service.admin.KhoAdminService.KetQuaNhap;
import project.dien_may.project_java.service.admin.NghiepVuException;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Lập phiếu yêu cầu nhập từ màn hình tồn kho. Mỗi test tự rollback nên không để lại dữ liệu trong CSDL. */
@SpringBootTest
@Transactional
class LapYeuCauNhapTests {

    private static final String NHAN_VIEN_KHO = "kho1@dienmay.vn";
    private static final String QUAN_TRI = "admin1@dienmay.vn";

    @Autowired
    private KhoAdminService khoService;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private YeuCauNhapHangRepository yeuCauRepository;

    @Autowired
    private ChiTietYeuCauNhapHangRepository chiTietRepository;

    private SanPham sanPham(Predicate<SanPham> dieuKien) {
        var ketQua = sanPhamRepository.findAll().stream().filter(dieuKien).findFirst();
        assumeTrue(ketQua.isPresent(), "CSDL không có sản phẩm phù hợp");
        return ketQua.get();
    }

    /** Không còn sản phẩm cảnh báo trong CSDL thì cho một sản phẩm hết hàng; test tự rollback. */
    private SanPham sanPhamCanhBaoChuaCoPhieu() {
        SanPham sanPham = sanPham(s -> s.isDangKinhDoanh()
                && !chiTietRepository.existsBySanPhamMaSanPhamAndYeuCauTrangThai(
                        s.getMaSanPham(), TrangThaiYeuCauNhap.CHO_DUYET));
        if (sanPham.getTinhTrangTonKho() == TinhTrangTonKho.DU) {
            sanPham.setSoLuong(0);
            sanPham = sanPhamRepository.saveAndFlush(sanPham);
        }
        return sanPham;
    }

    @Test
    void sanPhamCanhBaoLapDuocPhieuChoDuyet() {
        SanPham sanPham = sanPhamCanhBaoChuaCoPhieu();

        int tonTruoc = sanPham.getSoLuong();

        KetQuaNhap ketQua = khoService.lapYeuCauNhap(sanPham.getMaSanPham(), 7, NHAN_VIEN_KHO);

        assertFalse(ketQua.daNhapKho());
        YeuCauNhapHang yeuCau = yeuCauRepository.findById(ketQua.maYeuCau()).orElseThrow();
        assertEquals(TrangThaiYeuCauNhap.CHO_DUYET, yeuCau.getTrangThai());
        assertEquals(NHAN_VIEN_KHO, yeuCau.getTaiKhoan().getEmail());
        assertEquals(tonTruoc, tonKho(sanPham), "Chưa duyệt thì chưa cộng tồn kho");
    }

    @Test
    void quanTriNhapHangKhongCanDuyet() {
        SanPham sanPham = sanPhamCanhBaoChuaCoPhieu();
        int tonTruoc = sanPham.getSoLuong();

        KetQuaNhap ketQua = khoService.lapYeuCauNhap(sanPham.getMaSanPham(), 7, QUAN_TRI);

        assertTrue(ketQua.daNhapKho());
        YeuCauNhapHang yeuCau = yeuCauRepository.findById(ketQua.maYeuCau()).orElseThrow();
        assertEquals(TrangThaiYeuCauNhap.DA_DUYET, yeuCau.getTrangThai());
        assertEquals(QUAN_TRI, yeuCau.getNguoiDuyet().getEmail());
        assertEquals(tonTruoc + 7, tonKho(sanPham));
    }

    @Test
    void duyetPhieuThiCongTonKho() {
        SanPham sanPham = sanPhamCanhBaoChuaCoPhieu();
        int tonTruoc = sanPham.getSoLuong();
        Integer ma = khoService.lapYeuCauNhap(sanPham.getMaSanPham(), 7, NHAN_VIEN_KHO).maYeuCau();

        khoService.duyet(ma, QUAN_TRI);

        assertEquals(tonTruoc + 7, tonKho(sanPham));
    }

    @Test
    void tuChoiPhieuThiKhongCongTonKho() {
        SanPham sanPham = sanPhamCanhBaoChuaCoPhieu();
        int tonTruoc = sanPham.getSoLuong();
        Integer ma = khoService.lapYeuCauNhap(sanPham.getMaSanPham(), 7, NHAN_VIEN_KHO).maYeuCau();

        khoService.tuChoi(ma, QUAN_TRI, "Chưa cần nhập");

        assertEquals(tonTruoc, tonKho(sanPham));
    }

    private int tonKho(SanPham sanPham) {
        return sanPhamRepository.findById(sanPham.getMaSanPham()).orElseThrow().getSoLuong();
    }

    @Test
    void sanPhamDuHangKhongLapDuocPhieu() {
        SanPham sanPham = sanPham(s -> s.getTinhTrangTonKho() == TinhTrangTonKho.DU);

        assertThrows(NghiepVuException.class,
                () -> khoService.lapYeuCauNhap(sanPham.getMaSanPham(), 7, NHAN_VIEN_KHO));
    }

    @Test
    void soLuongPhaiLonHonKhong() {
        SanPham sanPham = sanPhamCanhBaoChuaCoPhieu();

        assertThrows(NghiepVuException.class,
                () -> khoService.lapYeuCauNhap(sanPham.getMaSanPham(), 0, NHAN_VIEN_KHO));
    }

    @Test
    void khongLapTrungPhieuDangChoDuyet() {
        SanPham sanPham = sanPhamCanhBaoChuaCoPhieu();
        khoService.lapYeuCauNhap(sanPham.getMaSanPham(), 7, NHAN_VIEN_KHO);

        assertThrows(NghiepVuException.class,
                () -> khoService.lapYeuCauNhap(sanPham.getMaSanPham(), 3, NHAN_VIEN_KHO));
    }
}
