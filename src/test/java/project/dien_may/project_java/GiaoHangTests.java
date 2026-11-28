package project.dien_may.project_java;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.dien_may.project_java.entity.ChiTietDonHang;
import project.dien_may.project_java.entity.DonHang;
import project.dien_may.project_java.entity.PhuongThucThanhToan;
import project.dien_may.project_java.entity.TrangThaiDonHang;
import project.dien_may.project_java.entity.TrangThaiThanhToan;
import project.dien_may.project_java.repository.ChiTietDonHangRepository;
import project.dien_may.project_java.repository.DonHangRepository;
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.repository.ThanhToanRepository;
import project.dien_may.project_java.service.admin.GiaoHangService;
import project.dien_may.project_java.service.admin.NghiepVuException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** TV4 – NVGH01 – NVGH04: nhân viên giao hàng nhận đơn, giao thành công, giao thất bại. Mỗi test tự rollback. */
@SpringBootTest
@Transactional
class GiaoHangTests {

    private static final String GIAO_HANG_1 = "giaohang1@dienmay.vn";
    private static final String GIAO_HANG_2 = "giaohang2@dienmay.vn";

    @Autowired private GiaoHangService service;
    @Autowired private DonHangRepository donHangRepository;
    @Autowired private ChiTietDonHangRepository chiTietRepository;
    @Autowired private SanPhamRepository sanPhamRepository;
    @Autowired private ThanhToanRepository thanhToanRepository;

    /** Lấy một đơn có sản phẩm, đưa về trạng thái "Đã xuất kho", chưa ai nhận. */
    private Integer donDaXuatKho() {
        var ketQua = donHangRepository.findAll().stream()
                .filter(d -> !chiTietRepository.findByDonHangMaDonHang(d.getMaDonHang()).isEmpty())
                .findFirst();
        assumeTrue(ketQua.isPresent(), "CSDL không có đơn hàng");
        DonHang don = ketQua.get();
        don.setTrangThai(TrangThaiDonHang.DA_XUAT_KHO);
        don.setNhanVienGiao(null);
        don.setNgayGiao(null);
        don.setLyDoGiaoThatBai(null);
        donHangRepository.saveAndFlush(don);
        return don.getMaDonHang();
    }

    private DonHang don(Integer ma) {
        return donHangRepository.findById(ma).orElseThrow();
    }

    @Test
    void nhanDonThiChuyenSangDangGiao() {
        Integer ma = donDaXuatKho();

        service.nhanDon(ma, GIAO_HANG_1);

        assertEquals(TrangThaiDonHang.DANG_GIAO, don(ma).getTrangThai());
        assertEquals(GIAO_HANG_1, don(ma).getNhanVienGiao().getEmail());
    }

    @Test
    void donDaCoNguoiNhanThiNguoiKhacKhongNhanVaKhongXuLyDuoc() {
        Integer ma = donDaXuatKho();
        service.nhanDon(ma, GIAO_HANG_1);

        assertThrows(NghiepVuException.class, () -> service.nhanDon(ma, GIAO_HANG_2));
        assertThrows(NghiepVuException.class, () -> service.daGiao(ma, GIAO_HANG_2));
        assertThrows(NghiepVuException.class, () -> service.giaoThatBai(ma, GIAO_HANG_2, "Thử"));
        assertThrows(ResponseStatusException.class, () -> service.chiTiet(ma, GIAO_HANG_2));
    }

    @Test
    void daGiaoGhiNgayGiaoVaGhiNhanDaThuTienKhiNhanHang() {
        Integer ma = donDaXuatKho();
        service.nhanDon(ma, GIAO_HANG_1);
        var thanhToan = thanhToanRepository.findFirstByDonHangMaDonHangOrderByMaThanhToanDesc(ma);
        boolean thuKhiGiao = thanhToan.isPresent()
                && thanhToan.get().getTrangThai() == TrangThaiThanhToan.CHUA_THANH_TOAN
                && List.of(PhuongThucThanhToan.COD, PhuongThucThanhToan.THE).contains(thanhToan.get().getPhuongThuc());

        service.daGiao(ma, GIAO_HANG_1);

        assertEquals(TrangThaiDonHang.DA_GIAO, don(ma).getTrangThai());
        assertNotNull(don(ma).getNgayGiao());
        if (thuKhiGiao) {
            assertEquals(TrangThaiThanhToan.DA_THANH_TOAN,
                    thanhToanRepository.findFirstByDonHangMaDonHangOrderByMaThanhToanDesc(ma).orElseThrow().getTrangThai());
        }
    }

    @Test
    void giaoThatBaiThiHuyDonVaHoanHangVeKho() {
        Integer ma = donDaXuatKho();
        Map<Integer, Integer> tonTruoc = new HashMap<>();
        Map<Integer, Integer> soLuongDon = new HashMap<>();
        for (ChiTietDonHang ct : chiTietRepository.findByDonHangMaDonHang(ma)) {
            Integer maSp = ct.getSanPham().getMaSanPham();
            tonTruoc.put(maSp, ct.getSanPham().getSoLuong());
            soLuongDon.merge(maSp, ct.getSoLuong(), Integer::sum);
        }
        service.nhanDon(ma, GIAO_HANG_1);

        assertThrows(NghiepVuException.class, () -> service.giaoThatBai(ma, GIAO_HANG_1, "  "));
        service.giaoThatBai(ma, GIAO_HANG_1, "Khách từ chối nhận hàng");

        assertEquals(TrangThaiDonHang.DA_HUY, don(ma).getTrangThai());
        assertEquals("Khách từ chối nhận hàng", don(ma).getLyDoGiaoThatBai());
        tonTruoc.forEach((maSp, ton) -> assertEquals(ton + soLuongDon.get(maSp),
                sanPhamRepository.findById(maSp).orElseThrow().getSoLuong()));
    }
}
