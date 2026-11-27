package project.dien_may.project_java;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import project.dien_may.project_java.controller.KiemThuKhoController;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.service.TonKhoKhongDuException;
import project.dien_may.project_java.service.TonKhoService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class KiemThuKhoControllerTests {
    private final SanPhamRepository sanPhamRepository = mock(SanPhamRepository.class);
    private final TonKhoService tonKhoService = mock(TonKhoService.class);
    private final KiemThuKhoController controller = new KiemThuKhoController(sanPhamRepository, tonKhoService);

    @Test
    void muaMotSanPhamTruKhoVaTraVeSoLuongConLai() {
        SanPham sanPham = sanPhamDangKinhDoanh();
        when(sanPhamRepository.findById(7)).thenReturn(Optional.of(sanPham));
        when(tonKhoService.getTonKho(7)).thenReturn(2);

        var ketQua = controller.muaMotSanPham(new KiemThuKhoController.MuaRequest(7, "Khách hàng 01"));

        assertEquals(200, ketQua.getStatusCode().value());
        assertTrue(ketQua.getBody().thanhCong());
        assertEquals(2, ketQua.getBody().tonKho());
        assertEquals(Thread.currentThread().getName(), ketQua.getBody().luongXuLy());
        verify(tonKhoService).truTonKho(7, 1);
    }

    @Test
    void hetHangTraVeThongDiepRoRangVaTonKhoThat() {
        SanPham sanPham = sanPhamDangKinhDoanh();
        when(sanPhamRepository.findById(7)).thenReturn(Optional.of(sanPham));
        when(tonKhoService.getTonKho(7)).thenReturn(0);
        doThrow(new TonKhoKhongDuException("out of stock")).when(tonKhoService).truTonKho(7, 1);

        var ketQua = controller.muaMotSanPham(new KiemThuKhoController.MuaRequest(7, "Khách hàng 02"));

        assertEquals(409, ketQua.getStatusCode().value());
        assertFalse(ketQua.getBody().thanhCong());
        assertEquals("Không đủ hàng trong kho.", ketQua.getBody().thongDiep());
        assertEquals(0, ketQua.getBody().tonKho());
    }

    @Test
    void xungDotCapNhatTraVeThongBaoDeHieu() {
        SanPham sanPham = sanPhamDangKinhDoanh();
        when(sanPhamRepository.findById(7)).thenReturn(Optional.of(sanPham));
        when(tonKhoService.getTonKho(7)).thenReturn(3);
        doThrow(new OptimisticLockingFailureException("version conflict"))
                .when(tonKhoService).truTonKho(7, 1);

        var ketQua = controller.muaMotSanPham(new KiemThuKhoController.MuaRequest(7, "Khách hàng 03"));

        assertEquals(409, ketQua.getStatusCode().value());
        assertFalse(ketQua.getBody().thanhCong());
        assertEquals("Kho vừa được cập nhật bởi một giao dịch khác. Vui lòng làm mới số lượng kho rồi thử lại.",
                ketQua.getBody().thongDiep());
        assertEquals(3, ketQua.getBody().tonKho());
    }

    private SanPham sanPhamDangKinhDoanh() {
        SanPham sanPham = mock(SanPham.class);
        when(sanPham.isDangKinhDoanh()).thenReturn(true);
        return sanPham;
    }
}
