package project.dien_may.project_java;

import org.junit.jupiter.api.Test;
import project.dien_may.project_java.service.admin.BaoCaoService;
import project.dien_may.project_java.service.admin.BaoCaoService.BaoCaoNam;
import project.dien_may.project_java.service.admin.BaoCaoService.DoanhThuThang;
import project.dien_may.project_java.service.admin.BaoCaoService.TomTatDoanhThu;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TomTatDoanhThuTests {

    private final BaoCaoService service = new BaoCaoService(null, null, null);

    /** Tháng không có trong doanhThu thì không có đơn; tháng sau thangNay là tháng chưa tới. */
    private TomTatDoanhThu tomTat(int thangNay, Map<Integer, Long> doanhThu) {
        List<DoanhThuThang> ds = new ArrayList<>();
        BigDecimal tong = BigDecimal.ZERO;
        for (int t = 1; t <= 12; t++) {
            Long tien = doanhThu.get(t);
            BigDecimal giaTri = BigDecimal.valueOf(tien == null ? 0 : tien);
            ds.add(new DoanhThuThang(t, tien == null ? 0 : 1, BigDecimal.ZERO, giaTri, t > thangNay));
            tong = tong.add(giaTri);
        }
        return service.tomTatDoanhThu(new BaoCaoNam(2026, ds, null, 0, BigDecimal.ZERO, tong, BigDecimal.ZERO), thangNay);
    }

    @Test
    void boQuaCacThangTruocDonDauTienVaThangDangChay() {
        TomTatDoanhThu kq = tomTat(11, Map.of(8, 89L, 9, 173L, 10, 87L, 11, 6L));
        assertEquals(BigDecimal.valueOf(355), kq.caNam());
        assertEquals(11, kq.thangHienTai().thang());
        assertEquals(9, kq.thangCaoNhat().thang());
        assertEquals(10, kq.thangThapNhat().thang());
    }

    @Test
    void thangKhongCoDoanhThuGiuaNamVanDuocTinhLaThapNhat() {
        TomTatDoanhThu kq = tomTat(5, Map.of(1, 50L, 3, 70L));
        assertEquals(2, kq.thangThapNhat().thang());
        assertEquals(3, kq.thangCaoNhat().thang());
    }

    @Test
    void thangDangChayCoTheLaCaoNhat() {
        assertEquals(4, tomTat(4, Map.of(3, 10L, 4, 90L)).thangCaoNhat().thang());
    }

    @Test
    void chuaCoDuLieuThiKhongSoSanh() {
        TomTatDoanhThu kq = tomTat(3, Map.of());
        assertNull(kq.thangCaoNhat());
        assertNull(kq.thangThapNhat());
    }

    @Test
    void thangDauTienCoDonChuaCoThangThapNhat() {
        TomTatDoanhThu kq = tomTat(1, Map.of(1, 10L));
        assertEquals(1, kq.thangCaoNhat().thang());
        assertNull(kq.thangThapNhat());
    }
}
