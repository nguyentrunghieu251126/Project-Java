package project.dien_may.project_java;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.dien_may.project_java.entity.ChiTietDonHang;
import project.dien_may.project_java.entity.DonHang;
import project.dien_may.project_java.entity.PhieuDoiTra;
import project.dien_may.project_java.entity.TrangThaiDoiTra;
import project.dien_may.project_java.entity.TrangThaiDonHang;
import project.dien_may.project_java.repository.ChiTietDonHangRepository;
import project.dien_may.project_java.repository.DonHangRepository;
import project.dien_may.project_java.repository.PhieuDoiTraRepository;
import project.dien_may.project_java.service.BaoHanhDoiTraService;
import project.dien_may.project_java.service.admin.NghiepVuException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** TV4 – KH21: khách gửi yêu cầu đổi trả nhiều sản phẩm của một đơn. Mỗi test tự rollback. */
@SpringBootTest
@Transactional
class DoiTraNhieuSanPhamTests {

    private static final String LY_DO = BaoHanhDoiTraService.LY_DO_DOI_TRA.get(0);

    @Autowired private BaoHanhDoiTraService service;
    @Autowired private DonHangRepository donHangRepository;
    @Autowired private ChiTietDonHangRepository chiTietRepository;
    @Autowired private PhieuDoiTraRepository doiTraRepository;

    /** Một đơn đã giao có ít nhất 2 sản phẩm chưa có yêu cầu đổi trả đang mở; ngày giao đặt về 3 ngày trước. */
    private DonHang donDaGiaoNhieuSanPham() {
        var ketQua = donHangRepository.findAll().stream()
                .filter(d -> d.getTrangThai() == TrangThaiDonHang.DA_GIAO)
                .filter(d -> chiTietRepository.findByDonHangMaDonHang(d.getMaDonHang()).stream()
                        .filter(ct -> !coYeuCauDangMo(ct)).count() >= 2)
                .findFirst();
        assumeTrue(ketQua.isPresent(), "CSDL không có đơn đã giao gồm từ 2 sản phẩm");
        DonHang don = ketQua.get();
        don.setNgayGiao(LocalDateTime.now().minusDays(3));
        return donHangRepository.saveAndFlush(don);
    }

    private boolean coYeuCauDangMo(ChiTietDonHang ct) {
        return doiTraRepository.existsByChiTietDonHangMaChiTietDonHangAndTrangThaiIn(ct.getMaChiTietDonHang(),
                List.of(TrangThaiDoiTra.CHO_TIEP_NHAN, TrangThaiDoiTra.CHAP_NHAN));
    }

    private List<Integer> haiSanPham(DonHang don) {
        return chiTietRepository.findByDonHangMaDonHang(don.getMaDonHang()).stream()
                .filter(ct -> !coYeuCauDangMo(ct))
                .limit(2).map(ChiTietDonHang::getMaChiTietDonHang).toList();
    }

    private String email(DonHang don) {
        return don.getTaiKhoan().getEmail();
    }

    @Test
    void guiMotLanChoHaiSanPhamTaoHaiPhieu() {
        DonHang don = donDaGiaoNhieuSanPham();
        List<Integer> chon = haiSanPham(don);

        List<PhieuDoiTra> dsPhieu = service.guiDoiTraNhieu(email(don), don.getMaDonHang(), chon, LY_DO, "Màn hình bị sọc");

        assertEquals(2, dsPhieu.size());
        dsPhieu.forEach(p -> {
            assertEquals(TrangThaiDoiTra.CHO_TIEP_NHAN, p.getTrangThai());
            assertTrue(p.getLyDoDoiTra().startsWith(LY_DO));
        });
    }

    @Test
    void sanPhamDaGuiThiTrangDoiTraKhongChoChonNua() {
        DonHang don = donDaGiaoNhieuSanPham();
        List<Integer> chon = haiSanPham(don);
        service.guiDoiTraNhieu(email(don), don.getMaDonHang(), chon.subList(0, 1), LY_DO, null);

        var trang = service.trangDoiTra(email(don), don.getMaDonHang());
        var dongDaGui = trang.dsDong().stream()
                .filter(d -> d.chiTiet().getMaChiTietDonHang().equals(chon.get(0))).findFirst().orElseThrow();
        assertFalse(dongDaGui.duocChon());
        assertThrows(NghiepVuException.class,
                () -> service.guiDoiTraNhieu(email(don), don.getMaDonHang(), chon.subList(0, 1), LY_DO, null));
    }

    @Test
    void chuaChonSanPhamHoacLyDoThiBaoLoi() {
        DonHang don = donDaGiaoNhieuSanPham();
        List<Integer> chon = haiSanPham(don);
        assertThrows(NghiepVuException.class,
                () -> service.guiDoiTraNhieu(email(don), don.getMaDonHang(), List.of(), LY_DO, null));
        assertThrows(NghiepVuException.class,
                () -> service.guiDoiTraNhieu(email(don), don.getMaDonHang(), chon, "Lý do tự nghĩ ra", null));
    }

    @Test
    void quaBaMuoiNgayThiKhongDoiTraDuoc() {
        DonHang don = donDaGiaoNhieuSanPham();
        don.setNgayGiao(LocalDateTime.now().minusDays(BaoHanhDoiTraService.HAN_DOI_TRA_NGAY + 1));
        donHangRepository.saveAndFlush(don);

        assertFalse(service.trangDoiTra(email(don), don.getMaDonHang()).conHan());
        assertThrows(NghiepVuException.class,
                () -> service.guiDoiTraNhieu(email(don), don.getMaDonHang(), haiSanPham(don), LY_DO, null));
    }

    @Test
    void khongXemDuocDonCuaKhachKhac() {
        DonHang don = donDaGiaoNhieuSanPham();
        assertThrows(ResponseStatusException.class,
                () -> service.trangDoiTra("khong.phai.chu.don@gmail.com", don.getMaDonHang()));
    }
}
