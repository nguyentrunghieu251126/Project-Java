package project.dien_may.project_java.service.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.dien_may.project_java.entity.ChiTietDonHang;
import project.dien_may.project_java.entity.DonHang;
import project.dien_may.project_java.entity.PhuongThucThanhToan;
import project.dien_may.project_java.entity.TaiKhoan;
import project.dien_may.project_java.entity.ThanhToan;
import project.dien_may.project_java.entity.TrangThaiDonHang;
import project.dien_may.project_java.entity.TrangThaiThanhToan;
import project.dien_may.project_java.repository.ChiTietDonHangRepository;
import project.dien_may.project_java.repository.DonHangRepository;
import project.dien_may.project_java.repository.KhuyenMaiKhachHangRepository;
import project.dien_may.project_java.repository.TaiKhoanRepository;
import project.dien_may.project_java.repository.ThanhToanRepository;
import project.dien_may.project_java.repository.TonKhoMuaHangRepository;
import project.dien_may.project_java.service.muahang.DiemThuongService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * TV4 – Nhân viên giao hàng (NVGH01 – NVGH04).
 * Luồng: kho lập phiếu xuất (DA_XUAT_KHO) → nhân viên giao hàng tự nhận đơn (DANG_GIAO)
 * → giao thành công (DA_GIAO) hoặc giao thất bại (DA_HUY, hoàn hàng về kho).
 * Mỗi nhân viên chỉ xem và xử lý được đơn chưa ai nhận hoặc đơn do chính mình nhận.
 */
@Service
public class GiaoHangService {

    private static final int TOI_DA_LY_DO = 500;
    /** Hai cách thanh toán thu tiền ngay khi giao hàng: tiền mặt và quẹt thẻ POS. */
    private static final Set<PhuongThucThanhToan> THU_KHI_GIAO = Set.of(PhuongThucThanhToan.COD,
            PhuongThucThanhToan.THE);

    public enum TabGiaoHang {
        CHO_NHAN("Chờ nhận"), DANG_GIAO("Đang giao"), DA_XONG("Đã xong");

        private final String nhan;

        TabGiaoHang(String nhan) {
            this.nhan = nhan;
        }

        public String getNhan() {
            return nhan;
        }
    }

    public record SoDon(long choNhan, long dangGiao) {
    }

    /**
     * @param tienThuHo số tiền nhân viên phải thu khi giao (0 nếu khách đã thanh toán trước)
     * @param cuaToi    true khi đơn do chính nhân viên đang đăng nhập nhận
     */
    public record ChiTietGiao(DonHang donHang, List<ChiTietDonHang> dsChiTiet, ThanhToan thanhToan,
            BigDecimal tienThuHo, boolean cuaToi) {
    }

    private final DonHangRepository donHangRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final ThanhToanRepository thanhToanRepository;
    private final TonKhoMuaHangRepository tonKhoRepository;
    private final KhuyenMaiKhachHangRepository khuyenMaiRepository;
    private final DiemThuongService diemThuongService;

    public GiaoHangService(DonHangRepository donHangRepository, ChiTietDonHangRepository chiTietDonHangRepository,
            TaiKhoanRepository taiKhoanRepository, ThanhToanRepository thanhToanRepository,
            TonKhoMuaHangRepository tonKhoRepository, KhuyenMaiKhachHangRepository khuyenMaiRepository,
            DiemThuongService diemThuongService) {
        this.donHangRepository = donHangRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.thanhToanRepository = thanhToanRepository;
        this.tonKhoRepository = tonKhoRepository;
        this.khuyenMaiRepository = khuyenMaiRepository;
        this.diemThuongService = diemThuongService;
    }

    @Transactional(readOnly = true)
    public Page<DonHang> danhSach(String email, TabGiaoHang tab, Pageable pageable) {
        return switch (tab) {
            case CHO_NHAN -> donHangRepository.donChoNhanGiao(pageable);
            case DANG_GIAO -> donHangRepository.donCuaNhanVienGiao(nhanVien(email),
                    List.of(TrangThaiDonHang.DANG_GIAO), pageable);
            case DA_XONG -> donHangRepository.donCuaNhanVienGiao(nhanVien(email),
                    List.of(TrangThaiDonHang.DA_GIAO, TrangThaiDonHang.DA_HUY), pageable);
        };
    }

    @Transactional(readOnly = true)
    public SoDon soDon(String email) {
        return new SoDon(donHangRepository.countByTrangThaiAndNhanVienGiaoIsNull(TrangThaiDonHang.DA_XUAT_KHO),
                donHangRepository.countByNhanVienGiaoAndTrangThai(nhanVien(email), TrangThaiDonHang.DANG_GIAO));
    }

    /** NVGH01: xem đơn. Chỉ xem được đơn chưa ai nhận hoặc đơn của chính mình. */
    @Transactional(readOnly = true)
    public ChiTietGiao chiTiet(Integer maDonHang, String email) {
        DonHang donHang = timDon(maDonHang);
        TaiKhoan toi = nhanVien(email);
        boolean choNhan = donHang.getTrangThai() == TrangThaiDonHang.DA_XUAT_KHO && donHang.getNhanVienGiao() == null;
        boolean cuaToi = donHang.getNhanVienGiao() != null
                && donHang.getNhanVienGiao().getMaTaiKhoan().equals(toi.getMaTaiKhoan());
        if (!choNhan && !cuaToi) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        ThanhToan thanhToan = thanhToanRepository.findFirstByDonHangMaDonHangOrderByMaThanhToanDesc(maDonHang)
                .orElse(null);
        return new ChiTietGiao(donHang, chiTietDonHangRepository.findByDonHangMaDonHang(maDonHang), thanhToan,
                tienThuHo(thanhToan, donHang), cuaToi);
    }

    /** NVGH02: nhận đơn để đi giao, đơn chuyển sang "Đang giao". */
    @Transactional
    public void nhanDon(Integer maDonHang, String email) {
        timDon(maDonHang);
        if (donHangRepository.nhanDonGiao(maDonHang, nhanVien(email)) == 0) {
            throw new NghiepVuException("Đơn hàng #" + maDonHang
                    + " đã có nhân viên khác nhận hoặc không còn ở trạng thái \"Đã xuất kho\".");
        }
    }

    /** NVGH03: giao thành công. Đơn trả tiền khi nhận hàng (COD, quẹt thẻ) được ghi nhận đã thanh toán. */
    @Transactional
    public void daGiao(Integer maDonHang, String email) {
        timDon(maDonHang);
        LocalDateTime bayGio = LocalDateTime.now();
        if (donHangRepository.xacNhanDaGiao(maDonHang, nhanVien(email), bayGio) == 0) {
            throw new NghiepVuException("Chỉ xác nhận đã giao được đơn bạn đang giao.");
        }
        thanhToanRepository.findFirstByDonHangMaDonHangOrderByMaThanhToanDesc(maDonHang).ifPresent(tt -> {
            if (tt.getTrangThai() == TrangThaiThanhToan.CHUA_THANH_TOAN && THU_KHI_GIAO.contains(tt.getPhuongThuc())) {
                tt.setTrangThai(TrangThaiThanhToan.DA_THANH_TOAN);
                tt.setNgayThanhToan(bayGio);
            }
        });
    }

    /**
     * NVGH04: giao thất bại (khách không nhận, sai địa chỉ...). Đơn chuyển sang "Đã hủy" kèm lý do,
     * hàng được hoàn về kho, hoàn lượt mã giảm giá, hoàn tiền nếu đã thanh toán và hoàn điểm thưởng,
     * giống như khi khách tự hủy đơn. Tất cả trong một transaction.
     */
    @Transactional
    public void giaoThatBai(Integer maDonHang, String email, String lyDo) {
        String noiDung = lyDo == null ? "" : lyDo.trim();
        if (noiDung.isEmpty()) {
            throw new NghiepVuException("Vui lòng nhập lý do giao không thành công.");
        }
        if (noiDung.length() > TOI_DA_LY_DO) {
            throw new NghiepVuException("Lý do không được vượt quá " + TOI_DA_LY_DO + " ký tự.");
        }
        timDon(maDonHang);
        if (donHangRepository.baoGiaoThatBai(maDonHang, nhanVien(email), noiDung) == 0) {
            throw new NghiepVuException("Chỉ báo giao thất bại được cho đơn bạn đang giao.");
        }
        // Câu UPDATE ở trên đã xóa bộ nhớ đệm JPA, nên đọc lại đơn trước khi dùng các quan hệ của nó
        DonHang donHang = timDon(maDonHang);
        for (ChiTietDonHang ct : chiTietDonHangRepository.findByDonHangMaDonHang(maDonHang)) {
            tonKhoRepository.hoanTonKho(ct.getSanPham().getMaSanPham(), ct.getSoLuong());
        }
        if (donHang.getKhuyenMai() != null) {
            khuyenMaiRepository.hoanMotLuot(donHang.getKhuyenMai().getMaKhuyenMai());
        }
        thanhToanRepository.findFirstByDonHangMaDonHangOrderByMaThanhToanDesc(maDonHang).ifPresent(tt -> {
            if (tt.getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN) {
                tt.setTrangThai(TrangThaiThanhToan.DA_HOAN_TIEN);
            }
        });
        diemThuongService.hoanKhiHuy(donHang);
    }

    private BigDecimal tienThuHo(ThanhToan thanhToan, DonHang donHang) {
        if (thanhToan == null) {
            return donHang.getTongTien();
        }
        boolean conPhaiThu = thanhToan.getTrangThai() == TrangThaiThanhToan.CHUA_THANH_TOAN
                && THU_KHI_GIAO.contains(thanhToan.getPhuongThuc());
        return conPhaiThu ? donHang.getTongTien() : BigDecimal.ZERO;
    }

    private DonHang timDon(Integer maDonHang) {
        return donHangRepository.findById(maDonHang)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private TaiKhoan nhanVien(String email) {
        return taiKhoanRepository.findByEmailIgnoreCase(email).orElseThrow();
    }
}
