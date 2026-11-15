package project.dien_may.project_java;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import project.dien_may.project_java.entity.ChiTietYeuCauNhapHang;
import project.dien_may.project_java.entity.PhieuNhap;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.entity.TinhTrangTonKho;
import project.dien_may.project_java.entity.TrangThaiYeuCauNhap;
import project.dien_may.project_java.repository.ChiTietYeuCauNhapHangRepository;
import project.dien_may.project_java.repository.PhieuNhapRepository;
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.repository.YeuCauNhapHangRepository;
import project.dien_may.project_java.service.admin.KhoAdminService;
import project.dien_may.project_java.service.admin.NghiepVuException;
import project.dien_may.project_java.service.admin.PhieuNhapService;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** TV4 – NVK10: lập phiếu nhập cho yêu cầu đã duyệt. Mỗi test tự rollback nên không để lại dữ liệu. */
@SpringBootTest
@Transactional
class PhieuNhapTests {

    private static final String NHAN_VIEN_KHO = "kho1@dienmay.vn";
    private static final String QUAN_TRI = "admin1@dienmay.vn";
    private static final BigDecimal DON_GIA = new BigDecimal("1500000");

    @Autowired private KhoAdminService khoService;
    @Autowired private PhieuNhapService phieuNhapService;
    @Autowired private SanPhamRepository sanPhamRepository;
    @Autowired private YeuCauNhapHangRepository yeuCauRepository;
    @Autowired private ChiTietYeuCauNhapHangRepository chiTietRepository;
    @Autowired private PhieuNhapRepository phieuNhapRepository;

    /** Lập một yêu cầu nhập 7 cái cho một sản phẩm cảnh báo, trả về mã yêu cầu. */
    private Integer lapYeuCau(SanPham sanPham) {
        return khoService.lapYeuCauNhap(sanPham.getMaSanPham(), 7, NHAN_VIEN_KHO).maYeuCau();
    }

    private SanPham sanPhamCanhBao() {
        var ketQua = sanPhamRepository.findAll().stream()
                .filter(s -> s.isDangKinhDoanh()
                        && !chiTietRepository.existsBySanPhamMaSanPhamAndYeuCauTrangThai(
                                s.getMaSanPham(), TrangThaiYeuCauNhap.CHO_DUYET)
                        && !chiTietRepository.existsBySanPhamMaSanPhamAndYeuCauTrangThai(
                                s.getMaSanPham(), TrangThaiYeuCauNhap.DA_DUYET))
                .findFirst();
        assumeTrue(ketQua.isPresent(), "CSDL không có sản phẩm phù hợp");
        SanPham sanPham = ketQua.get();
        if (sanPham.getTinhTrangTonKho() == TinhTrangTonKho.DU) {
            sanPham.setSoLuong(0);
            sanPham = sanPhamRepository.saveAndFlush(sanPham);
        }
        return sanPham;
    }

    private List<Integer> maChiTiet(Integer maYeuCau) {
        return chiTietRepository.findByYeuCauMaYeuCauNhap(maYeuCau).stream()
                .map(ChiTietYeuCauNhapHang::getMaChiTietYeuCauNhap).toList();
    }

    private int tonKho(SanPham sanPham) {
        return sanPhamRepository.findById(sanPham.getMaSanPham()).orElseThrow().getSoLuong();
    }

    @Test
    void lapPhieuNhapThiCongTonKhoVaChuyenDaNhap() {
        SanPham sanPham = sanPhamCanhBao();
        int tonTruoc = sanPham.getSoLuong();
        Integer ma = lapYeuCau(sanPham);
        khoService.duyet(ma, QUAN_TRI);

        PhieuNhap phieu = phieuNhapService.lapPhieuNhap(ma, NHAN_VIEN_KHO, maChiTiet(ma), List.of(DON_GIA), "HD-001");

        assertEquals(tonTruoc + 7, tonKho(sanPham));
        assertEquals(TrangThaiYeuCauNhap.DA_NHAP, yeuCauRepository.findById(ma).orElseThrow().getTrangThai());
        assertEquals(0, DON_GIA.multiply(BigDecimal.valueOf(7)).compareTo(phieu.getTongTienNhap()));
    }

    @Test
    void chuaDuyetThiKhongLapDuocPhieuNhap() {
        SanPham sanPham = sanPhamCanhBao();
        int tonTruoc = sanPham.getSoLuong();
        Integer ma = lapYeuCau(sanPham);

        assertThrows(NghiepVuException.class,
                () -> phieuNhapService.lapPhieuNhap(ma, NHAN_VIEN_KHO, maChiTiet(ma), List.of(DON_GIA), null));
        assertEquals(tonTruoc, tonKho(sanPham));
    }

    @Test
    void khongLapPhieuNhapHaiLan() {
        SanPham sanPham = sanPhamCanhBao();
        Integer ma = lapYeuCau(sanPham);
        khoService.duyet(ma, QUAN_TRI);
        phieuNhapService.lapPhieuNhap(ma, NHAN_VIEN_KHO, maChiTiet(ma), List.of(DON_GIA), null);
        int tonSauLanDau = tonKho(sanPham);

        assertThrows(NghiepVuException.class,
                () -> phieuNhapService.lapPhieuNhap(ma, NHAN_VIEN_KHO, maChiTiet(ma), List.of(DON_GIA), null));
        assertEquals(tonSauLanDau, tonKho(sanPham), "Không được cộng kho lần thứ hai");
    }

    @Test
    void donGiaAmThiKhongLuuGi() {
        SanPham sanPham = sanPhamCanhBao();
        int tonTruoc = sanPham.getSoLuong();
        Integer ma = lapYeuCau(sanPham);
        khoService.duyet(ma, QUAN_TRI);

        assertThrows(NghiepVuException.class, () -> phieuNhapService.lapPhieuNhap(
                ma, NHAN_VIEN_KHO, maChiTiet(ma), List.of(new BigDecimal("-1")), null));
        assertFalse(phieuNhapRepository.existsByYeuCauMaYeuCauNhap(ma));
        assertEquals(tonTruoc, tonKho(sanPham));
        assertEquals(TrangThaiYeuCauNhap.DA_DUYET, yeuCauRepository.findById(ma).orElseThrow().getTrangThai());
    }
}
