package project.dien_may.project_java.service.muahang;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.dien_may.project_java.dto.muahang.BuocDonHang;
import project.dien_may.project_java.dto.muahang.ChiTietDonHangKhach;
import project.dien_may.project_java.dto.muahang.DonHangTomTat;
import project.dien_may.project_java.dto.muahang.TabDonHang;
import project.dien_may.project_java.entity.*;
import project.dien_may.project_java.repository.DanhGiaKhachHangRepository;
import project.dien_may.project_java.repository.DonHangKhachHangRepository;
import project.dien_may.project_java.repository.KhuyenMaiKhachHangRepository;
import project.dien_may.project_java.repository.TaiKhoanRepository;
import project.dien_may.project_java.repository.ThanhToanRepository;
import project.dien_may.project_java.repository.TonKhoMuaHangRepository;
import project.dien_may.project_java.service.admin.NghiepVuException;

import java.util.*;

/** TV3 – KH16 xem đơn, KH17 theo dõi, KH18 hủy + hoàn tồn kho, KH19 lịch sử mua hàng, KH22 đánh giá. */
@Service
public class DonHangKhachHangService {

    public static final int SO_DON_MOI_TRANG = 5;
    public static final String TAB_TAT_CA = "TAT_CA";

    /** Tab lọc trên trang lịch sử; "Đang xử lý" gộp hai trạng thái ở khâu kho. */
    private static final Map<String, List<TrangThaiDonHang>> TAB = new LinkedHashMap<>();
    private static final Map<String, String> NHAN_TAB = new LinkedHashMap<>();

    static {
        TAB.put(TAB_TAT_CA, List.of(TrangThaiDonHang.values()));
        TAB.put("CHO_XAC_NHAN", List.of(TrangThaiDonHang.CHO_XAC_NHAN));
        TAB.put("DANG_XU_LY", List.of(TrangThaiDonHang.DA_XAC_NHAN, TrangThaiDonHang.DA_XUAT_KHO));
        TAB.put("DANG_GIAO", List.of(TrangThaiDonHang.DANG_GIAO));
        TAB.put("DA_GIAO", List.of(TrangThaiDonHang.DA_GIAO));
        TAB.put("DA_HUY", List.of(TrangThaiDonHang.DA_HUY));
        NHAN_TAB.put(TAB_TAT_CA, "Tất cả");
        NHAN_TAB.put("CHO_XAC_NHAN", "Chờ xác nhận");
        NHAN_TAB.put("DANG_XU_LY", "Đang xử lý");
        NHAN_TAB.put("DANG_GIAO", "Đang giao");
        NHAN_TAB.put("DA_GIAO", "Đã giao");
        NHAN_TAB.put("DA_HUY", "Đã hủy");
    }

    /** Các bước theo dõi (không gồm DA_HUY), theo thứ tự xử lý. */
    private static final List<TrangThaiDonHang> CAC_BUOC = List.of(TrangThaiDonHang.CHO_XAC_NHAN,
            TrangThaiDonHang.DA_XAC_NHAN, TrangThaiDonHang.DA_XUAT_KHO, TrangThaiDonHang.DANG_GIAO,
            TrangThaiDonHang.DA_GIAO);

    private final DonHangKhachHangRepository donHangRepository;
    private final ThanhToanRepository thanhToanRepository;
    private final DanhGiaKhachHangRepository danhGiaRepository;
    private final TonKhoMuaHangRepository tonKhoRepository;
    private final KhuyenMaiKhachHangRepository khuyenMaiRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final DiemThuongService diemThuongService;

    public DonHangKhachHangService(DonHangKhachHangRepository donHangRepository,
                                   ThanhToanRepository thanhToanRepository,
                                   DanhGiaKhachHangRepository danhGiaRepository,
                                   TonKhoMuaHangRepository tonKhoRepository,
                                   KhuyenMaiKhachHangRepository khuyenMaiRepository,
                                   TaiKhoanRepository taiKhoanRepository,
                                   DiemThuongService diemThuongService) {
        this.donHangRepository = donHangRepository;
        this.thanhToanRepository = thanhToanRepository;
        this.danhGiaRepository = danhGiaRepository;
        this.tonKhoRepository = tonKhoRepository;
        this.khuyenMaiRepository = khuyenMaiRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.diemThuongService = diemThuongService;
    }

    public static String chuanHoaTab(String tab) {
        return tab != null && TAB.containsKey(tab) ? tab : TAB_TAT_CA;
    }

    @Transactional(readOnly = true)
    public List<TabDonHang> demTheoTab(String email) {
        Map<TrangThaiDonHang, Long> dem = new EnumMap<>(TrangThaiDonHang.class);
        for (Object[] dong : donHangRepository.demTheoTrangThai(email)) {
            dem.put((TrangThaiDonHang) dong[0], ((Number) dong[1]).longValue());
        }
        List<TabDonHang> ketQua = new ArrayList<>();
        TAB.forEach((ma, dsTrangThai) -> ketQua.add(new TabDonHang(ma, NHAN_TAB.get(ma),
                dsTrangThai.stream().mapToLong(t -> dem.getOrDefault(t, 0L)).sum())));
        return ketQua;
    }

    /** @param trang số trang trên URL, tính từ 1 */
    @Transactional(readOnly = true)
    public Page<DonHangTomTat> layLichSu(String email, String tab, String tuKhoa, int trang) {
        String tuKhoaSach = tuKhoa == null || tuKhoa.isBlank() ? null : tuKhoa.trim();
        Integer maDon = null;
        if (tuKhoaSach != null) {
            String so = tuKhoaSach.replaceFirst("(?i)^#?DH0*", "");
            if (so.matches("\\d{1,9}")) maDon = Integer.valueOf(so);
        }
        Pageable trangYeuCau = PageRequest.of(Math.max(trang, 1) - 1, SO_DON_MOI_TRANG);
        Page<DonHang> page = donHangRepository.timCuaKhach(email, TAB.get(chuanHoaTab(tab)), tuKhoaSach, maDon,
                trangYeuCau);
        if (page.isEmpty()) {
            return new PageImpl<>(List.of(), trangYeuCau, page.getTotalElements());
        }
        Map<Integer, List<ChiTietDonHang>> theoDon = new HashMap<>();
        List<Integer> dsMa = page.getContent().stream().map(DonHang::getMaDonHang).toList();
        for (ChiTietDonHang ct : donHangRepository.chiTietCuaCacDon(dsMa)) {
            theoDon.computeIfAbsent(ct.getDonHang().getMaDonHang(), k -> new ArrayList<>()).add(ct);
        }
        return page.map(d -> new DonHangTomTat(d, theoDon.getOrDefault(d.getMaDonHang(), List.of())));
    }

    @Transactional(readOnly = true)
    public ChiTietDonHangKhach layChiTiet(String email, Integer maDon) {
        DonHang donHang = timDon(email, maDon);
        List<ChiTietDonHang> chiTiet = donHangRepository.chiTietCuaCacDon(List.of(maDon));
        ThanhToan thanhToan = thanhToanRepository.findFirstByDonHangMaDonHangOrderByMaThanhToanDesc(maDon)
                .orElse(null);
        Map<Integer, Integer> soSao = new HashMap<>();
        if (!chiTiet.isEmpty()) {
            List<Integer> dsSanPham = chiTiet.stream().map(ct -> ct.getSanPham().getMaSanPham()).toList();
            for (Object[] dong : danhGiaRepository.soSaoDaDanhGia(donHang.getTaiKhoan().getMaTaiKhoan(), dsSanPham)) {
                soSao.put((Integer) dong[0], ((Number) dong[1]).intValue());
            }
        }
        return new ChiTietDonHangKhach(donHang, chiTiet, thanhToan, dungCacBuoc(donHang), soSao,
                diemThuongService.theoDon(maDon));
    }

    /** KH18: chỉ hủy khi đơn còn chờ xác nhận; hoàn tồn kho, hoàn lượt mã giảm giá và tiền (nếu đã trả). */
    @Transactional
    public void huyDon(String email, Integer maDon) {
        DonHang donHang = timDon(email, maDon);
        if (donHang.getTrangThai() != TrangThaiDonHang.CHO_XAC_NHAN
                || donHangRepository.huyNeuDangCho(maDon, TrangThaiDonHang.CHO_XAC_NHAN, TrangThaiDonHang.DA_HUY) == 0) {
            throw new NghiepVuException("Đơn #DH" + String.format("%06d", maDon)
                    + " đã được cửa hàng xác nhận nên không thể hủy. Vui lòng gọi hotline 1800 1234 để được hỗ trợ.");
        }
        donHang.setTrangThai(TrangThaiDonHang.DA_HUY);
        for (ChiTietDonHang ct : donHangRepository.chiTietCuaCacDon(List.of(maDon))) {
            tonKhoRepository.hoanTonKho(ct.getSanPham().getMaSanPham(), ct.getSoLuong());
        }
        if (donHang.getKhuyenMai() != null) {
            khuyenMaiRepository.hoanMotLuot(donHang.getKhuyenMai().getMaKhuyenMai());
        }
        thanhToanRepository.findFirstByDonHangMaDonHangOrderByMaThanhToanDesc(maDon).ifPresent(tt -> {
            if (tt.getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN) {
                tt.setTrangThai(TrangThaiThanhToan.DA_HOAN_TIEN);
            }
        });
        // Điểm thưởng: hoàn điểm đã dùng, thu hồi điểm đã tích từ đơn này
        diemThuongService.hoanKhiHuy(donHang);
    }

    /** KH22: mỗi khách đánh giá một sản phẩm một lần, chỉ với sản phẩm trong đơn đã giao. */
    @Transactional
    public void danhGia(String email, Integer maDon, Integer maSanPham, Integer soSao, String noiDung) {
        if (soSao == null || soSao < 1 || soSao > 5) {
            throw new NghiepVuException("Vui lòng chọn từ 1 đến 5 sao.");
        }
        String noiDungSach = noiDung == null || noiDung.isBlank() ? null : noiDung.trim();
        if (noiDungSach != null && noiDungSach.length() > 1000) {
            throw new NghiepVuException("Nhận xét không được vượt quá 1000 ký tự.");
        }
        DonHang donHang = timDon(email, maDon);
        if (donHang.getTrangThai() != TrangThaiDonHang.DA_GIAO) {
            throw new NghiepVuException("Chỉ đánh giá được sản phẩm trong đơn đã giao.");
        }
        boolean coTrongDon = donHangRepository.chiTietCuaCacDon(List.of(maDon)).stream()
                .anyMatch(ct -> ct.getSanPham().getMaSanPham().equals(maSanPham));
        if (!coTrongDon) {
            throw new NghiepVuException("Sản phẩm không thuộc đơn hàng này.");
        }
        Integer maTaiKhoan = taiKhoanRepository.findByEmailIgnoreCase(email).orElseThrow().getMaTaiKhoan();
        if (danhGiaRepository.existsByTaiKhoanMaTaiKhoanAndSanPhamMaSanPham(maTaiKhoan, maSanPham)) {
            throw new NghiepVuException("Bạn đã đánh giá sản phẩm này rồi.");
        }
        danhGiaRepository.themDanhGia(maTaiKhoan, maSanPham, soSao, noiDungSach == null ? "" : noiDungSach);
    }

    private DonHang timDon(String email, Integer maDon) {
        return donHangRepository.timCuaKhach(maDon, email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** CSDL chỉ lưu ngày đặt và ngày giao nên các bước giữa không có giờ cụ thể. */
    private static List<BuocDonHang> dungCacBuoc(DonHang d) {
        int viTri = CAC_BUOC.indexOf(d.getTrangThai());
        String[] ten = {"Đặt hàng", "Đã xác nhận", "Đã xuất kho", "Đang giao", "Đã giao"};
        String[] icon = {"bi-receipt", "bi-check2", "bi-box-seam", "bi-truck", "bi-house-check"};
        List<BuocDonHang> ketQua = new ArrayList<>();
        for (int i = 0; i < CAC_BUOC.size(); i++) {
            boolean xong = viTri >= i;
            java.time.LocalDateTime moc = i == 0 ? d.getNgayDat() : (i == CAC_BUOC.size() - 1 ? d.getNgayGiao() : null);
            ketQua.add(new BuocDonHang(ten[i], icon[i], xong, viTri == i, xong ? moc : null));
        }
        return ketQua;
    }
}
