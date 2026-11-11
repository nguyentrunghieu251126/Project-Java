package project.dien_may.project_java;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import project.dien_may.project_java.dto.muahang.DatHangForm;
import project.dien_may.project_java.dto.muahang.DiaChiForm;
import project.dien_may.project_java.dto.muahang.ThoiGianGiaoHang;
import project.dien_may.project_java.dto.muahang.UuDaiThanhToan;
import project.dien_may.project_java.dto.muahang.TinhThanh;
import project.dien_may.project_java.dto.muahang.DongGioHang;
import project.dien_may.project_java.entity.KhuyenMai;
import project.dien_may.project_java.entity.LoaiGiamGia;
import project.dien_may.project_java.entity.PhuongThucThanhToan;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.service.admin.NghiepVuException;
import project.dien_may.project_java.service.muahang.DatHangService;
import project.dien_may.project_java.service.muahang.DonHangKhachHangService;
import project.dien_may.project_java.service.muahang.GioHangService;
import project.dien_may.project_java.service.muahang.SoDiaChiService;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * TV3 – giỏ hàng, đặt hàng (@Transactional), hủy đơn + hoàn tồn kho, mã giảm giá, đánh giá.
 * Chạy trên CSDL mẫu; mỗi test tự rollback nên không để lại dữ liệu.
 * Tồn kho đọc bằng JDBC vì các câu UPDATE trừ/hoàn kho chạy thẳng trên CSDL.
 */
@SpringBootTest
@Transactional
class MuaHangTests {

    private static final String KHACH_HANG = "lan.bui7@gmail.com";

    @Autowired
    private GioHangService gioHangService;

    @Autowired
    private DatHangService datHangService;

    @Autowired
    private DonHangKhachHangService donHangService;

    @Autowired
    private SoDiaChiService soDiaChiService;

    @Autowired
    private project.dien_may.project_java.service.muahang.DiemThuongService diemThuongService;

    /** Đẩy thay đổi của JPA xuống CSDL trước khi đọc lại bằng JDBC. */
    @Autowired
    private jakarta.persistence.EntityManager em;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private SanPham sanPhamConHang(int toiThieu) {
        var ketQua = sanPhamRepository.findAll().stream()
                .filter(s -> s.isDangKinhDoanh() && s.getSoLuong() >= toiThieu)
                .findFirst();
        assumeTrue(ketQua.isPresent(), "CSDL không có sản phẩm đủ hàng");
        return ketQua.get();
    }

    private int tonKho(Integer maSanPham) {
        return dem("SELECT SoLuong FROM dbo.SanPham WHERE MaSanPham = ?", maSanPham);
    }

    private int dem(String sql, Object... thamSo) {
        Integer ketQua = jdbc.queryForObject(sql, Integer.class, thamSo);
        return ketQua == null ? 0 : ketQua;
    }

    private String trangThaiDon(Integer maDon) {
        return jdbc.queryForObject("SELECT TrangThaiDonHang FROM dbo.DonHang WHERE MaDonHang = ?", String.class, maDon);
    }

    private DatHangForm form(Integer maSanPham) {
        DatHangForm form = new DatHangForm();
        form.setHoTenNguoiNhan("Bùi Thu Lan");
        form.setSoDienThoaiNguoiNhan("0955597971");
        form.setDiaChiGiaoHang("431 Nguyễn Trãi, Quận 10, TP. Hồ Chí Minh");
        form.setPhuongThucThanhToan(PhuongThucThanhToan.COD);
        form.setSanPham(List.of(maSanPham));
        return form;
    }

    private boolean coTrongGio(Integer maSanPham) {
        return gioHangService.layGioHang(KHACH_HANG).stream().anyMatch(d -> d.maSanPham().equals(maSanPham));
    }

    @Test
    void themVaoGioKhongVuotTonKho() {
        SanPham sp = sanPhamConHang(1);
        gioHangService.xoa(KHACH_HANG, List.of(sp.getMaSanPham()));

        gioHangService.them(KHACH_HANG, sp.getMaSanPham(), 9999);

        DongGioHang dong = gioHangService.layGioHang(KHACH_HANG).stream()
                .filter(d -> d.maSanPham().equals(sp.getMaSanPham())).findFirst().orElseThrow();
        assertEquals(tonKho(sp.getMaSanPham()), dong.soLuong());
        assertThrows(NghiepVuException.class, () -> gioHangService.them(KHACH_HANG, sp.getMaSanPham(), 1),
                "Đã bằng tồn kho thì không thêm được nữa");
    }

    @Test
    void datHangTruTonKhoLuuThanhToanVaXoaKhoiGio() {
        SanPham sp = sanPhamConHang(2);
        gioHangService.xoa(KHACH_HANG, List.of(sp.getMaSanPham()));
        gioHangService.them(KHACH_HANG, sp.getMaSanPham(), 2);
        int tonTruoc = tonKho(sp.getMaSanPham());

        Integer maDon = datHangService.datHang(KHACH_HANG, form(sp.getMaSanPham()));

        assertEquals(tonTruoc - 2, tonKho(sp.getMaSanPham()));
        assertEquals("CHO_XAC_NHAN", trangThaiDon(maDon));
        assertEquals(1, dem("SELECT COUNT(*) FROM dbo.ThanhToan WHERE MaDonHang = ?", maDon));
        BigDecimal tong = jdbc.queryForObject("SELECT TongTien FROM dbo.DonHang WHERE MaDonHang = ?", BigDecimal.class, maDon);
        assertEquals(0, sp.getDonGia().multiply(BigDecimal.valueOf(2)).compareTo(tong));
        assertFalse(coTrongGio(sp.getMaSanPham()), "Mua xong thì bỏ khỏi giỏ");
    }

    @Test
    void datHangKhiHetHangBiChanVaKhongTaoDon() {
        SanPham sp = sanPhamConHang(1);
        gioHangService.xoa(KHACH_HANG, List.of(sp.getMaSanPham()));
        gioHangService.them(KHACH_HANG, sp.getMaSanPham(), 1);
        jdbc.update("UPDATE dbo.SanPham SET SoLuong = 0 WHERE MaSanPham = ?", sp.getMaSanPham());
        int soDonTruoc = dem("SELECT COUNT(*) FROM dbo.DonHang");

        assertThrows(NghiepVuException.class, () -> datHangService.datHang(KHACH_HANG, form(sp.getMaSanPham())));

        assertEquals(0, tonKho(sp.getMaSanPham()), "Không bán vượt tồn kho (NFR06)");
        assertEquals(soDonTruoc, dem("SELECT COUNT(*) FROM dbo.DonHang"));
    }

    @Test
    void huyDonChoXacNhanThiHoanTonKho() {
        SanPham sp = sanPhamConHang(1);
        gioHangService.xoa(KHACH_HANG, List.of(sp.getMaSanPham()));
        gioHangService.them(KHACH_HANG, sp.getMaSanPham(), 1);
        int tonTruoc = tonKho(sp.getMaSanPham());
        Integer maDon = datHangService.datHang(KHACH_HANG, form(sp.getMaSanPham()));

        donHangService.huyDon(KHACH_HANG, maDon);

        assertEquals("DA_HUY", trangThaiDon(maDon));
        assertEquals(tonTruoc, tonKho(sp.getMaSanPham()));
    }

    @Test
    void khongHuyDuocDonDaXacNhan() {
        SanPham sp = sanPhamConHang(1);
        gioHangService.xoa(KHACH_HANG, List.of(sp.getMaSanPham()));
        gioHangService.them(KHACH_HANG, sp.getMaSanPham(), 1);
        Integer maDon = datHangService.datHang(KHACH_HANG, form(sp.getMaSanPham()));
        jdbc.update("UPDATE dbo.DonHang SET TrangThaiDonHang = 'DA_XAC_NHAN' WHERE MaDonHang = ?", maDon);

        assertThrows(NghiepVuException.class, () -> donHangService.huyDon(KHACH_HANG, maDon));
    }

    @Test
    void chiDanhGiaDuocKhiDonDaGiao() {
        SanPham sp = sanPhamConHang(1);
        gioHangService.xoa(KHACH_HANG, List.of(sp.getMaSanPham()));
        gioHangService.them(KHACH_HANG, sp.getMaSanPham(), 1);
        Integer maDon = datHangService.datHang(KHACH_HANG, form(sp.getMaSanPham()));

        assertThrows(NghiepVuException.class,
                () -> donHangService.danhGia(KHACH_HANG, maDon, sp.getMaSanPham(), 5, "Tốt"));
    }

    @Test
    void tinhTienGiamTheoLoaiMa() {
        KhuyenMai phanTram = new KhuyenMai();
        phanTram.setLoaiGiamGia(LoaiGiamGia.PHAN_TRAM);
        phanTram.setGiaTriGiam(BigDecimal.valueOf(15));
        assertEquals(0, BigDecimal.valueOf(5_622_000)
                .compareTo(DatHangService.tinhTienGiam(phanTram, BigDecimal.valueOf(37_480_000))));

        KhuyenMai soTien = new KhuyenMai();
        soTien.setLoaiGiamGia(LoaiGiamGia.SO_TIEN);
        soTien.setGiaTriGiam(BigDecimal.valueOf(250_000));
        assertEquals(0, BigDecimal.valueOf(250_000)
                .compareTo(DatHangService.tinhTienGiam(soTien, BigDecimal.valueOf(37_480_000))));
        assertTrue(DatHangService.tinhTienGiam(soTien, BigDecimal.valueOf(100_000))
                .compareTo(BigDecimal.valueOf(100_000)) == 0, "Không giảm quá giá trị đơn");
    }

    @Test
    void muaLaiDonDaHuyDuaSanPhamVaoGio() {
        SanPham sp = sanPhamConHang(1);
        gioHangService.xoa(KHACH_HANG, List.of(sp.getMaSanPham()));
        gioHangService.them(KHACH_HANG, sp.getMaSanPham(), 1);
        Integer maDon = datHangService.datHang(KHACH_HANG, form(sp.getMaSanPham()));
        donHangService.huyDon(KHACH_HANG, maDon);
        assertFalse(coTrongGio(sp.getMaSanPham()));

        GioHangService.KetQuaMuaLai ketQua = gioHangService.muaLai(KHACH_HANG, maDon);

        assertTrue(ketQua.daThem().contains(sp.getMaSanPham()));
        assertTrue(coTrongGio(sp.getMaSanPham()));
    }

    @Test
    void khongMuaLaiDonDangCho() {
        SanPham sp = sanPhamConHang(1);
        gioHangService.xoa(KHACH_HANG, List.of(sp.getMaSanPham()));
        gioHangService.them(KHACH_HANG, sp.getMaSanPham(), 1);
        Integer maDon = datHangService.datHang(KHACH_HANG, form(sp.getMaSanPham()));

        assertThrows(NghiepVuException.class, () -> gioHangService.muaLai(KHACH_HANG, maDon));
    }

    // ===== Sổ địa chỉ (bảng SoDiaChi) =====

    private DiaChiForm diaChiMoi(String ten, boolean macDinh) {
        DiaChiForm f = new DiaChiForm();
        f.setTenNguoiNhan(ten);
        f.setSoDienThoai("0901234567");
        f.setKhuVuc(TinhThanh.THANH_PHO.get(0));
        f.setDiaChiChiTiet("12 Nguyễn Huệ, Phường Bến Nghé");
        f.setLaMacDinh(macDinh);
        return f;
    }

    private int soDiaChiMacDinh() {
        return dem("SELECT COUNT(*) FROM dbo.SoDiaChi d JOIN dbo.TaiKhoan t ON t.MaTaiKhoan = d.MaTaiKhoan "
                + "WHERE t.Email = ? AND d.LaMacDinh = 1", KHACH_HANG);
    }

    @Test
    void themDiaChiMacDinhChiConMotMacDinh() {
        assumeTrue(soDiaChiService.danhSach(KHACH_HANG).size() <= SoDiaChiService.SO_DIA_CHI_TOI_DA - 2, "Sổ địa chỉ đã gần đầy");
        soDiaChiService.luu(KHACH_HANG, diaChiMoi("Người nhận A", false));
        Integer maB = soDiaChiService.luu(KHACH_HANG, diaChiMoi("Người nhận B", true));
        em.flush();

        assertEquals(1, soDiaChiMacDinh());
        assertTrue(soDiaChiService.timCuaKhach(KHACH_HANG, maB).isLaMacDinh());
        // Địa chỉ mặc định được đồng bộ sang hồ sơ (TaiKhoan.DiaChi)
        String diaChiHoSo = jdbc.queryForObject("SELECT DiaChi FROM dbo.TaiKhoan WHERE Email = ?", String.class, KHACH_HANG);
        assertEquals(soDiaChiService.timCuaKhach(KHACH_HANG, maB).getDiaChiDayDu(), diaChiHoSo);
    }

    @Test
    void suaDiaChiCapNhatCsdl() {
        assumeTrue(soDiaChiService.danhSach(KHACH_HANG).size() < SoDiaChiService.SO_DIA_CHI_TOI_DA, "Sổ địa chỉ đã đầy");
        Integer ma = soDiaChiService.luu(KHACH_HANG, diaChiMoi("Tên cũ", false));
        DiaChiForm sua = DiaChiForm.tu(soDiaChiService.timCuaKhach(KHACH_HANG, ma));
        sua.setTenNguoiNhan("Tên mới");
        soDiaChiService.luu(KHACH_HANG, sua);
        em.flush();

        assertEquals("Tên mới", jdbc.queryForObject("SELECT TenNguoiNhan FROM dbo.SoDiaChi WHERE MaDiaChi = ?", String.class, ma));
    }

    @Test
    void xoaDiaChiMacDinhThiChonMacDinhKhac() {
        assumeTrue(soDiaChiService.danhSach(KHACH_HANG).size() <= SoDiaChiService.SO_DIA_CHI_TOI_DA - 2, "Sổ địa chỉ đã gần đầy");
        soDiaChiService.luu(KHACH_HANG, diaChiMoi("Người nhận A", false));
        Integer maMacDinh = soDiaChiService.luu(KHACH_HANG, diaChiMoi("Người nhận B", true));
        soDiaChiService.xoa(KHACH_HANG, maMacDinh);
        em.flush();

        assertEquals(0, dem("SELECT COUNT(*) FROM dbo.SoDiaChi WHERE MaDiaChi = ?", maMacDinh));
        assertEquals(1, soDiaChiMacDinh());
    }

    @Test
    void khongSuaXoaDiaChiKhongTonTai() {
        assertThrows(NghiepVuException.class, () -> soDiaChiService.timCuaKhach(KHACH_HANG, -1));
        assertThrows(NghiepVuException.class, () -> soDiaChiService.xoa(KHACH_HANG, -1));
    }

    @Test
    void khongLuuTinhThanhNgoaiDanhSach() {
        DiaChiForm f = diaChiMoi("Người nhận", false);
        f.setKhuVuc("Sao Hỏa");
        assertThrows(NghiepVuException.class, () -> soDiaChiService.luu(KHACH_HANG, f));
    }

    @Test
    void diaChiCangXaThiGiaoCangLau() {
        ThoiGianGiaoHang hcm = ThoiGianGiaoHang.theoKhuVuc("TP. Hồ Chí Minh");
        ThoiGianGiaoHang dongNai = ThoiGianGiaoHang.theoKhuVuc("Đồng Nai");
        ThoiGianGiaoHang haNoi = ThoiGianGiaoHang.theoKhuVuc("Hà Nội");
        assertTrue(hcm.denNgay() < dongNai.denNgay() && dongNai.denNgay() < haNoi.denNgay());
        // Địa chỉ trong đơn: lấy tỉnh / thành sau dấu phẩy cuối
        assertEquals(haNoi, ThoiGianGiaoHang.theoDiaChi("12 Tràng Tiền, Hoàn Kiếm, Hà Nội"));
        // Mọi tỉnh / thành trong danh sách đều đã được xếp vùng
        TinhThanh.tatCa().forEach(t -> assertFalse("Toàn quốc".equals(ThoiGianGiaoHang.theoKhuVuc(t).vung()), t));
    }

    @Test
    void maGiamGiaXepTheoTienGiamGiamDan() {
        SanPham sp = sanPhamConHang(1);
        gioHangService.them(KHACH_HANG, sp.getMaSanPham(), 1);
        var trang = datHangService.layTrangThanhToan(KHACH_HANG, List.of(sp.getMaSanPham()));
        var ds = trang.maDungDuoc();
        for (int i = 1; i < ds.size(); i++) {
            assertTrue(ds.get(i - 1).tienGiam().compareTo(ds.get(i).tienGiam()) >= 0, "Mã giảm nhiều phải đứng trước");
        }
        // Mã hết hạn / hết lượt không nằm trong nhóm dùng được
        trang.maHetHan().forEach(m -> assertFalse(m.dungDuoc(), m.maGiamGia()));
    }

    // ===== Ưu đãi lần đầu theo phương thức thanh toán =====

    private boolean conUuDai(PhuongThucThanhToan pt, Integer maSanPham) {
        return datHangService.layTrangThanhToan(KHACH_HANG, List.of(maSanPham)).phuongThuc().stream()
                .filter(lc -> lc.phuongThuc() == pt).findFirst().orElseThrow().coUuDai();
    }

    @Test
    void uuDaiLanDauChiDungMotLanVaTraLaiKhiHuyDon() {
        SanPham sp = sanPhamConHang(2);
        gioHangService.xoa(KHACH_HANG, List.of(sp.getMaSanPham()));
        gioHangService.them(KHACH_HANG, sp.getMaSanPham(), 1);
        assumeTrue(conUuDai(PhuongThucThanhToan.KREDIVO, sp.getMaSanPham()), "Tài khoản mẫu đã dùng ưu đãi Kredivo");
        BigDecimal giamDuKien = UuDaiThanhToan.tinh(PhuongThucThanhToan.KREDIVO, sp.getDonGia(), BigDecimal.ZERO).tienGiam();

        DatHangForm f = form(sp.getMaSanPham());
        f.setPhuongThucThanhToan(PhuongThucThanhToan.KREDIVO);
        Integer maDon = datHangService.datHang(KHACH_HANG, f);

        BigDecimal tong = jdbc.queryForObject("SELECT TongTien FROM dbo.DonHang WHERE MaDonHang = ?", BigDecimal.class, maDon);
        assertEquals(0, sp.getDonGia().subtract(giamDuKien).compareTo(tong), "Tổng tiền đã trừ ưu đãi thanh toán");
        // Dùng rồi: ưu đãi Kredivo không hiện nữa, phương thức khác vẫn còn
        gioHangService.them(KHACH_HANG, sp.getMaSanPham(), 1);
        assertFalse(conUuDai(PhuongThucThanhToan.KREDIVO, sp.getMaSanPham()));
        // Hủy đơn: ưu đãi được trả lại
        donHangService.huyDon(KHACH_HANG, maDon);
        assertTrue(conUuDai(PhuongThucThanhToan.KREDIVO, sp.getMaSanPham()));
    }

    @Test
    void uuDaiThanhToanLayDieuKienGiamNhieuNhat() {
        // Đơn 20 triệu có laptop 16 triệu qua ZaloPay: 150K (laptop) > 100K (đơn từ 6 triệu)
        var kq = UuDaiThanhToan.tinh(PhuongThucThanhToan.ZALOPAY, BigDecimal.valueOf(20_000_000), BigDecimal.valueOf(16_000_000));
        assertEquals(0, BigDecimal.valueOf(150_000).compareTo(kq.tienGiam()));
        // Đơn dưới mức tối thiểu thì không giảm
        assertFalse(UuDaiThanhToan.tinh(PhuongThucThanhToan.SACOMBANK, BigDecimal.valueOf(5_000_000), BigDecimal.ZERO).coGiam());
        // COD không có ưu đãi
        assertTrue(UuDaiThanhToan.dieuKien(PhuongThucThanhToan.COD).isEmpty());
    }

    // ===== Điểm thưởng (bảng LichSuDiemThuong) =====

    private void tangDiem(int soDiem) {
        jdbc.update("""
                INSERT INTO dbo.LichSuDiemThuong (MaTaiKhoan, LoaiGiaoDich, SoDiem, MoTa)
                SELECT MaTaiKhoan, 'CONG', ?, N'Điểm thử nghiệm' FROM dbo.TaiKhoan WHERE Email = ?""", soDiem, KHACH_HANG);
    }

    @Test
    void datHangDungDiemVaTichDiemHuyThiTraLai() {
        SanPham sp = sanPhamConHang(1);
        assumeTrue(sp.getDonGia().compareTo(BigDecimal.valueOf(100_000)) >= 0, "Cần sản phẩm từ 100.000₫");
        gioHangService.xoa(KHACH_HANG, List.of(sp.getMaSanPham()));
        gioHangService.them(KHACH_HANG, sp.getMaSanPham(), 1);
        tangDiem(500);
        long diemTruoc = diemThuongService.tongDiem(KHACH_HANG);

        DatHangForm f = form(sp.getMaSanPham());
        f.setDiemSuDung(10);
        Integer maDon = datHangService.datHang(KHACH_HANG, f);

        BigDecimal tong = jdbc.queryForObject("SELECT TongTien FROM dbo.DonHang WHERE MaDonHang = ?", BigDecimal.class, maDon);
        assertEquals(0, sp.getDonGia().subtract(BigDecimal.valueOf(10_000)).compareTo(tong), "Tổng tiền đã trừ 10 điểm = 10.000₫");
        int diemTich = tong.divide(BigDecimal.valueOf(100_000), 0, java.math.RoundingMode.DOWN).intValue();
        assertEquals(diemTruoc - 10 + diemTich, diemThuongService.tongDiem(KHACH_HANG));

        // Hủy đơn: hoàn 10 điểm đã dùng, thu hồi điểm vừa tích
        donHangService.huyDon(KHACH_HANG, maDon);
        assertEquals(diemTruoc, diemThuongService.tongDiem(KHACH_HANG));
    }

    @Test
    void khongDungDuoiToiThieuHoacVuotSoDiem() {
        SanPham sp = sanPhamConHang(1);
        gioHangService.xoa(KHACH_HANG, List.of(sp.getMaSanPham()));
        gioHangService.them(KHACH_HANG, sp.getMaSanPham(), 1);

        DatHangForm it = form(sp.getMaSanPham());
        it.setDiemSuDung(5);
        assertThrows(NghiepVuException.class, () -> datHangService.datHang(KHACH_HANG, it), "Dưới 10 điểm");

        DatHangForm nhieu = form(sp.getMaSanPham());
        nhieu.setDiemSuDung((int) diemThuongService.tongDiem(KHACH_HANG) + 1000);
        assertThrows(NghiepVuException.class, () -> datHangService.datHang(KHACH_HANG, nhieu), "Vượt số điểm đang có");
    }

    @Test
    void quyTacDiemThuong() {
        assertEquals(12, project.dien_may.project_java.dto.muahang.QuyTacDiemThuong.diemNhanDuoc(BigDecimal.valueOf(1_250_000)));
        // Đơn 100.000₫ chỉ dùng tối đa 50% = 50 điểm dù có 500 điểm
        assertEquals(50, project.dien_may.project_java.dto.muahang.QuyTacDiemThuong.diemToiDa(500, BigDecimal.valueOf(100_000)));
        // Có dưới 10 điểm thì không dùng được
        assertEquals(0, project.dien_may.project_java.dto.muahang.QuyTacDiemThuong.diemToiDa(9, BigDecimal.valueOf(10_000_000)));
    }
}
