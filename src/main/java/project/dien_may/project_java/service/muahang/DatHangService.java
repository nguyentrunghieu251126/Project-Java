package project.dien_may.project_java.service.muahang;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.dien_may.project_java.dto.muahang.DatHangForm;
import project.dien_may.project_java.dto.muahang.DongGioHang;
import project.dien_may.project_java.dto.muahang.LuaChonThanhToan;
import project.dien_may.project_java.dto.muahang.MaUuDai;
import project.dien_may.project_java.dto.muahang.TrangThanhToan;
import project.dien_may.project_java.dto.muahang.UuDaiThanhToan;
import project.dien_may.project_java.entity.*;
import project.dien_may.project_java.repository.*;
import project.dien_may.project_java.service.admin.NghiepVuException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * TV3 – KH12 – KH15: trang thanh toán và đặt hàng.
 * Đặt hàng chạy trong một giao dịch (@Transactional): lỗi ở bất kỳ bước nào thì tồn kho,
 * lượt mã giảm giá và giỏ hàng đều quay về như cũ.
 */
@Service
public class DatHangService {

    /** Mã sắp diễn ra trong khoảng này mới hiển thị cho khách xem trước. */
    private static final int SO_NGAY_XEM_TRUOC_MA = 14;
    /** Mã hết hạn trong bao nhiêu ngày gần đây thì vẫn hiện (mờ) ở cuối danh sách. */
    private static final int SO_NGAY_XEM_MA_HET_HAN = 30;

    private final GioHangService gioHangService;
    private final ChiTietGioHangRepository chiTietGioHangRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final TonKhoMuaHangRepository tonKhoRepository;
    private final KhuyenMaiKhachHangRepository khuyenMaiRepository;
    private final DonHangKhachHangRepository donHangRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final ThanhToanRepository thanhToanRepository;

    public DatHangService(GioHangService gioHangService, ChiTietGioHangRepository chiTietGioHangRepository,
                          TaiKhoanRepository taiKhoanRepository, TonKhoMuaHangRepository tonKhoRepository,
                          KhuyenMaiKhachHangRepository khuyenMaiRepository,
                          DonHangKhachHangRepository donHangRepository,
                          ChiTietDonHangRepository chiTietDonHangRepository,
                          ThanhToanRepository thanhToanRepository) {
        this.gioHangService = gioHangService;
        this.chiTietGioHangRepository = chiTietGioHangRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.tonKhoRepository = tonKhoRepository;
        this.khuyenMaiRepository = khuyenMaiRepository;
        this.donHangRepository = donHangRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
        this.thanhToanRepository = thanhToanRepository;
    }

    /** @param dsChon mã sản phẩm được tick ở giỏ; rỗng thì lấy cả giỏ */
    @Transactional(readOnly = true)
    public TrangThanhToan layTrangThanhToan(String email, List<Integer> dsChon) {
        List<DongGioHang> dong = gioHangService.layGioHang(email).stream()
                .filter(d -> dsChon == null || dsChon.isEmpty() || dsChon.contains(d.maSanPham()))
                .toList();
        BigDecimal tamTinh = dong.stream().map(DongGioHang::thanhTien).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate homNay = LocalDate.now();

        // Thứ tự hiển thị: 1) mã dùng được, giảm nhiều tiền trước  2) chưa đủ điều kiện (chưa tới ngày)
        //                  3) đã hết hạn hoặc hết lượt
        List<MaUuDai> dungDuoc = khuyenMaiRepository.dangChay(homNay).stream()
                .map(km -> chuyenMa(km, true, tinhTienGiam(km, tamTinh), false))
                .sorted(Comparator.comparing(MaUuDai::tienGiam).reversed()
                        .thenComparing(MaUuDai::ngayKetThuc))
                .toList();
        List<MaUuDai> sapXep = new ArrayList<>();
        for (int i = 0; i < dungDuoc.size(); i++) {
            MaUuDai m = dungDuoc.get(i);
            // Mã đầu tiên (giảm nhiều nhất) gắn nhãn "Tiết kiệm nhất"
            boolean laTotNhat = i == 0 && m.tienGiam().signum() > 0;
            sapXep.add(new MaUuDai(m.maGiamGia(), m.tenKhuyenMai(), m.loai(), m.giaTriGiam(), m.ngayBatDau(),
                    m.ngayKetThuc(), m.soLuotConLai(), true, m.tienGiam(), laTotNhat));
        }

        List<MaUuDai> sapCo = khuyenMaiRepository.sapDienRa(homNay, homNay.plusDays(SO_NGAY_XEM_TRUOC_MA)).stream()
                .map(km -> chuyenMa(km, false, tinhTienGiam(km, tamTinh), false))
                .sorted(Comparator.comparing(MaUuDai::tienGiam).reversed()
                        .thenComparing(MaUuDai::ngayBatDau))
                .toList();
        List<MaUuDai> hetHan = khuyenMaiRepository
                .hetHanHoacHetLuot(homNay, homNay.minusDays(SO_NGAY_XEM_MA_HET_HAN)).stream()
                .map(km -> chuyenMa(km, false, tinhTienGiam(km, tamTinh), false))
                .sorted(Comparator.comparing(MaUuDai::tienGiam).reversed())
                .toList();
        // Phương thức thanh toán + ưu đãi lần đầu (đã dùng ở đơn chưa hủy thì không hiện nữa)
        BigDecimal tienLaptop = dong.stream().filter(DongGioHang::laLaptop)
                .map(DongGioHang::thanhTien).reduce(BigDecimal.ZERO, BigDecimal::add);
        Set<PhuongThucThanhToan> daDung = Set.copyOf(thanhToanRepository.daDungUuDai(email));
        List<LuaChonThanhToan> phuongThuc = new ArrayList<>();
        for (PhuongThucThanhToan pt : PhuongThucThanhToan.values()) {
            boolean conUuDai = !daDung.contains(pt);
            phuongThuc.add(new LuaChonThanhToan(pt,
                    conUuDai ? UuDaiThanhToan.dieuKien(pt) : List.of(),
                    conUuDai ? UuDaiThanhToan.tinh(pt, tamTinh, tienLaptop).tienGiam() : BigDecimal.ZERO));
        }
        return new TrangThanhToan(dong, tamTinh, sapXep, sapCo, hetHan, phuongThuc);
    }

    /** @return mã đơn hàng vừa tạo */
    @Transactional
    public Integer datHang(String email, DatHangForm form) {
        Set<Integer> dsChon = Set.copyOf(form.getSanPham() == null ? List.of() : form.getSanPham());
        if (dsChon.isEmpty()) {
            throw new NghiepVuException("Vui lòng chọn ít nhất một sản phẩm để đặt hàng.");
        }
        TaiKhoan taiKhoan = taiKhoanRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy tài khoản, vui lòng đăng nhập lại."));
        List<ChiTietGioHang> dongMua = chiTietGioHangRepository.timTheoEmail(email).stream()
                .filter(ct -> dsChon.contains(ct.getSanPham().getMaSanPham()))
                .toList();
        if (dongMua.isEmpty()) {
            throw new NghiepVuException("Các sản phẩm đã chọn không còn trong giỏ hàng.");
        }

        // 1. Trừ tồn kho có điều kiện cho từng sản phẩm (không bán vượt tồn – NFR06)
        BigDecimal tamTinh = BigDecimal.ZERO;
        for (ChiTietGioHang ct : dongMua) {
            SanPham sp = ct.getSanPham();
            if (tonKhoRepository.truTonKhoNeuDu(sp.getMaSanPham(), ct.getSoLuong()) == 0) {
                throw new NghiepVuException(sp.isDangKinhDoanh() && sp.getSoLuong() > 0
                        ? sp.getTenSanPham() + " chỉ còn " + sp.getSoLuong() + " sản phẩm, vui lòng giảm số lượng."
                        : sp.getTenSanPham() + " đã hết hàng, vui lòng bỏ khỏi đơn.");
            }
            tamTinh = tamTinh.add(sp.getDonGia().multiply(BigDecimal.valueOf(ct.getSoLuong())));
        }

        // 2. Mã giảm giá: chỉ nhận mã đang bật, còn hạn, còn lượt (KH15)
        KhuyenMai khuyenMai = null;
        BigDecimal tienGiam = BigDecimal.ZERO;
        String ma = form.getMaGiamGia() == null ? "" : form.getMaGiamGia().trim();
        if (!ma.isEmpty()) {
            khuyenMai = khuyenMaiRepository.findByMaGiamGiaIgnoreCase(ma)
                    .orElseThrow(() -> new NghiepVuException("Mã giảm giá " + ma.toUpperCase() + " không tồn tại."));
            if (khuyenMaiRepository.dungMotLuot(khuyenMai.getMaKhuyenMai(), LocalDate.now()) == 0) {
                throw new NghiepVuException("Mã " + khuyenMai.getMaGiamGia()
                        + " đã hết hạn, hết lượt hoặc đang tạm dừng.");
            }
            tienGiam = tinhTienGiam(khuyenMai, tamTinh);
        }

        // 2b. Ưu đãi lần đầu của phương thức thanh toán (mỗi phương thức một lần; đơn bị hủy thì được dùng lại)
        PhuongThucThanhToan phuongThuc = form.getPhuongThucThanhToan();
        UuDaiThanhToan.KetQua uuDai = UuDaiThanhToan.KetQua.KHONG;
        if (!thanhToanRepository.daDungUuDai(email).contains(phuongThuc)) {
            BigDecimal tienLaptop = dongMua.stream()
                    .filter(ct -> ct.getSanPham().getDanhMuc() != null
                            && "Laptop".equalsIgnoreCase(ct.getSanPham().getDanhMuc().getTenDanhMuc().trim()))
                    .map(ChiTietGioHang::getThanhTien).reduce(BigDecimal.ZERO, BigDecimal::add);
            uuDai = UuDaiThanhToan.tinh(phuongThuc, tamTinh, tienLaptop);
        }
        // Không giảm quá số tiền còn lại sau mã giảm giá
        BigDecimal tienGiamThanhToan = uuDai.tienGiam().min(tamTinh.subtract(tienGiam));

        // 3. Lưu đơn hàng, chi tiết (giá chốt tại thời điểm đặt) và thanh toán
        DonHang donHang = new DonHang();
        donHang.setTaiKhoan(taiKhoan);
        donHang.setKhuyenMai(khuyenMai);
        donHang.setNgayDat(LocalDateTime.now());
        donHang.setHoTenNguoiNhan(form.getHoTenNguoiNhan().trim());
        donHang.setSoDienThoaiNguoiNhan(form.getSoDienThoaiNguoiNhan().trim());
        donHang.setDiaChiGiaoHang(form.getDiaChiGiaoHang().trim());
        donHang.setTienGiam(tienGiam);
        donHang.setTongTien(tamTinh.subtract(tienGiam).subtract(tienGiamThanhToan));
        donHang.setTrangThai(TrangThaiDonHang.CHO_XAC_NHAN);
        donHangRepository.save(donHang);

        for (ChiTietGioHang ct : dongMua) {
            ChiTietDonHang chiTiet = new ChiTietDonHang();
            chiTiet.setDonHang(donHang);
            chiTiet.setSanPham(ct.getSanPham());
            chiTiet.setSoLuong(ct.getSoLuong());
            chiTiet.setDonGia(ct.getSanPham().getDonGia());
            chiTiet.setThanhTien(ct.getThanhTien());
            chiTietDonHangRepository.save(chiTiet);
        }

        ThanhToan thanhToan = new ThanhToan();
        thanhToan.setDonHang(donHang);
        thanhToan.setPhuongThuc(phuongThuc);
        if (tienGiamThanhToan.signum() > 0) {
            thanhToan.setUuDai(uuDai.dieuKien().moTa());
            thanhToan.setTienGiamUuDai(tienGiamThanhToan);
        }
        thanhToan.setSoTien(donHang.getTongTien());
        thanhToan.setTrangThai(TrangThaiThanhToan.CHUA_THANH_TOAN);
        thanhToanRepository.save(thanhToan);

        // 4. Bỏ các sản phẩm đã mua khỏi giỏ
        chiTietGioHangRepository.xoaCacDong(dongMua.get(0).getGioHang().getMaGioHang(), dsChon);
        return donHang.getMaDonHang();
    }

    /** PHAN_TRAM: làm tròn đến đồng; SO_TIEN: không giảm quá giá trị đơn. */
    public static BigDecimal tinhTienGiam(KhuyenMai km, BigDecimal tamTinh) {
        if (km == null || tamTinh == null || tamTinh.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (km.getLoaiGiamGia() == LoaiGiamGia.PHAN_TRAM) {
            return tamTinh.multiply(km.getGiaTriGiam()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        }
        return km.getGiaTriGiam().min(tamTinh);
    }

    private static MaUuDai chuyenMa(KhuyenMai km, boolean dungDuoc, BigDecimal tienGiam, boolean totNhat) {
        Integer conLai = km.getSoLuotToiDa() == null ? null : km.getSoLuotToiDa() - km.getSoLuotDaDung();
        return new MaUuDai(km.getMaGiamGia(), km.getTenKhuyenMai(), km.getLoaiGiamGia(), km.getGiaTriGiam(),
                km.getNgayBatDau(), km.getNgayKetThuc(), conLai, dungDuoc, tienGiam, totNhat);
    }
}
