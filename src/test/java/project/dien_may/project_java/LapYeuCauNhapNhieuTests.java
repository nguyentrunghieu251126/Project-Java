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
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.repository.YeuCauNhapHangRepository;
import project.dien_may.project_java.service.admin.KhoAdminService;
import project.dien_may.project_java.service.admin.KhoAdminService.KetQuaNhap;
import project.dien_may.project_java.service.admin.NghiepVuException;
import project.dien_may.project_java.service.admin.PhieuNhapService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** TV4 – NVK09, NVK10: một yêu cầu nhập gồm nhiều sản phẩm, lập một phiếu nhập nhiều món. Mỗi test tự rollback. */
@SpringBootTest
@Transactional
class LapYeuCauNhapNhieuTests {

    private static final String NHAN_VIEN_KHO = "kho1@dienmay.vn";
    private static final String QUAN_TRI = "admin1@dienmay.vn";

    @Autowired private KhoAdminService khoService;
    @Autowired private PhieuNhapService phieuNhapService;
    @Autowired private SanPhamRepository sanPhamRepository;
    @Autowired private YeuCauNhapHangRepository yeuCauRepository;
    @Autowired private ChiTietYeuCauNhapHangRepository chiTietRepository;

    /** Hai sản phẩm đang kinh doanh, chưa có yêu cầu đang chờ; ép về hết hàng để được phép yêu cầu nhập. */
    private List<SanPham> haiSanPhamCanhBao() {
        List<SanPham> ds = sanPhamRepository.findAll().stream()
                .filter(s -> s.isDangKinhDoanh()
                        && !chiTietRepository.existsBySanPhamMaSanPhamAndYeuCauTrangThai(
                                s.getMaSanPham(), TrangThaiYeuCauNhap.CHO_DUYET)
                        && !chiTietRepository.existsBySanPhamMaSanPhamAndYeuCauTrangThai(
                                s.getMaSanPham(), TrangThaiYeuCauNhap.DA_DUYET))
                .limit(2).toList();
        assumeTrue(ds.size() == 2, "CSDL không có đủ 2 sản phẩm phù hợp");
        return ds.stream().map(s -> {
            if (s.getTinhTrangTonKho() == TinhTrangTonKho.DU) {
                s.setSoLuong(0);
                return sanPhamRepository.saveAndFlush(s);
            }
            return s;
        }).toList();
    }

    private List<Integer> ma(List<SanPham> ds) {
        return ds.stream().map(SanPham::getMaSanPham).toList();
    }

    @Test
    void motYeuCauGomHaiSanPham() {
        List<SanPham> ds = haiSanPhamCanhBao();

        KetQuaNhap ketQua = khoService.lapYeuCauNhapNhieu(ma(ds), List.of(4, 6), NHAN_VIEN_KHO);

        assertFalse(ketQua.daDuyet());
        List<ChiTietYeuCauNhapHang> chiTiet = chiTietRepository.findByYeuCauMaYeuCauNhap(ketQua.maYeuCau());
        assertEquals(2, chiTiet.size());
        assertEquals(10, chiTiet.stream().mapToInt(ChiTietYeuCauNhapHang::getSoLuongYeuCau).sum());
        assertEquals(TrangThaiYeuCauNhap.CHO_DUYET,
                yeuCauRepository.findById(ketQua.maYeuCau()).orElseThrow().getTrangThai());
    }

    @Test
    void lapPhieuNhapChoYeuCauNhieuMonCongDuTonKho() {
        List<SanPham> ds = haiSanPhamCanhBao();
        int ton0 = ds.get(0).getSoLuong(), ton1 = ds.get(1).getSoLuong();

        KetQuaNhap ketQua = khoService.lapYeuCauNhapNhieu(ma(ds), List.of(4, 6), QUAN_TRI);
        assertTrue(ketQua.daDuyet());

        List<ChiTietYeuCauNhapHang> chiTiet = chiTietRepository.findByYeuCauMaYeuCauNhap(ketQua.maYeuCau());
        List<Integer> maDong = chiTiet.stream().map(ChiTietYeuCauNhapHang::getMaChiTietYeuCauNhap).toList();
        List<BigDecimal> donGia = Collections.nCopies(maDong.size(), new BigDecimal("1000000"));
        PhieuNhap phieu = phieuNhapService.lapPhieuNhap(ketQua.maYeuCau(), NHAN_VIEN_KHO, maDong, donGia, null);

        assertEquals(2, phieu.getChiTiet().size());
        assertEquals(ton0 + 4, sanPhamRepository.findById(ds.get(0).getMaSanPham()).orElseThrow().getSoLuong());
        assertEquals(ton1 + 6, sanPhamRepository.findById(ds.get(1).getMaSanPham()).orElseThrow().getSoLuong());
    }

    @Test
    void chonTrungSanPhamHoacThieuSoLuongThiBaoLoi() {
        List<SanPham> ds = haiSanPhamCanhBao();
        Integer ma0 = ds.get(0).getMaSanPham();
        assertThrows(NghiepVuException.class,
                () -> khoService.lapYeuCauNhapNhieu(List.of(ma0, ma0), List.of(1, 2), NHAN_VIEN_KHO));
        assertThrows(NghiepVuException.class,
                () -> khoService.lapYeuCauNhapNhieu(ma(ds), List.of(3), NHAN_VIEN_KHO));
        assertThrows(NghiepVuException.class,
                () -> khoService.lapYeuCauNhapNhieu(List.of(), List.of(), NHAN_VIEN_KHO));
    }

    @Test
    void sanPhamDaCoYeuCauKhongHienTrongDanhSachChon() {
        List<SanPham> ds = haiSanPhamCanhBao();
        khoService.lapYeuCauNhapNhieu(ma(ds), List.of(1, 1), NHAN_VIEN_KHO);

        assertTrue(khoService.sanPhamCoTheYeuCauNhap().stream()
                .noneMatch(d -> ma(ds).contains(d.sanPham().getMaSanPham())));
        assertThrows(NghiepVuException.class,
                () -> khoService.lapYeuCauNhapNhieu(ma(ds).subList(0, 1), List.of(1), NHAN_VIEN_KHO));
    }

    @Test
    void sanPhamConHangVanNhapThemDuoc() {
        var ketQua = sanPhamRepository.findAll().stream()
                .filter(s -> s.isDangKinhDoanh() && s.getTinhTrangTonKho() == TinhTrangTonKho.DU
                        && !chiTietRepository.existsBySanPhamMaSanPhamAndYeuCauTrangThai(
                                s.getMaSanPham(), TrangThaiYeuCauNhap.CHO_DUYET)
                        && !chiTietRepository.existsBySanPhamMaSanPhamAndYeuCauTrangThai(
                                s.getMaSanPham(), TrangThaiYeuCauNhap.DA_DUYET))
                .findFirst();
        assumeTrue(ketQua.isPresent(), "CSDL không có sản phẩm còn hàng phù hợp");
        Integer ma = ketQua.get().getMaSanPham();

        assertTrue(khoService.sanPhamCoTheYeuCauNhap().stream()
                .anyMatch(d -> d.sanPham().getMaSanPham().equals(ma) && !d.canhBao()));
        KetQuaNhap yeuCau = khoService.lapYeuCauNhapNhieu(List.of(ma), List.of(10), NHAN_VIEN_KHO);
        assertEquals(1, chiTietRepository.findByYeuCauMaYeuCauNhap(yeuCau.maYeuCau()).size());
    }
}
