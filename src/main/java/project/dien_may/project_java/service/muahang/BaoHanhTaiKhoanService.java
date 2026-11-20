package project.dien_may.project_java.service.muahang;

import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.dien_may.project_java.dto.muahang.ThongTinBaoHanh;
import project.dien_may.project_java.entity.ChiTietDonHang;
import project.dien_may.project_java.entity.PhieuBaoHanh;
import project.dien_may.project_java.entity.TrangThaiBaoHanh;
import project.dien_may.project_java.entity.TrangThaiDonHang;
import project.dien_may.project_java.repository.ChiTietDonHangRepository;
import project.dien_may.project_java.repository.PhieuBaoHanhRepository;
import project.dien_may.project_java.service.admin.NghiepVuException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BaoHanhTaiKhoanService {

    public static final String MUA_GAN_NHAT = "mua-gan-nhat";
    public static final String HET_HAN_GAN_NHAT = "bao-hanh-gan-nhat";
    public static final String HET_HAN_XA_NHAT = "bao-hanh-xa-nhat";
    public static final int TOI_DA_KY_TU = 1000;
    private static final Set<TrangThaiBaoHanh> CHO_TIEP_NHAN = Set.of(TrangThaiBaoHanh.CHO_TIEP_NHAN);

    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final PhieuBaoHanhRepository phieuBaoHanhRepository;

    public BaoHanhTaiKhoanService(ChiTietDonHangRepository chiTietDonHangRepository,
            PhieuBaoHanhRepository phieuBaoHanhRepository) {
        this.chiTietDonHangRepository = chiTietDonHangRepository;
        this.phieuBaoHanhRepository = phieuBaoHanhRepository;
    }

    public static String chuanHoaSapXep(String sapXep) {
        return HET_HAN_GAN_NHAT.equals(sapXep) || HET_HAN_XA_NHAT.equals(sapXep) ? sapXep : MUA_GAN_NHAT;
    }

    @Transactional(readOnly = true)
    public List<ThongTinBaoHanh> danhSach(String email, LocalDate tuNgay, LocalDate denNgay, String sapXep) {
        List<PhieuBaoHanh> tatCaPhieu = phieuBaoHanhRepository
                .findByChiTietDonHangDonHangTaiKhoanEmailIgnoreCaseOrderByNgayYeuCauDesc(email);

        tatCaPhieu.forEach(p -> Hibernate.initialize(p.getNhanVien()));
        Map<Integer, List<PhieuBaoHanh>> phieuTheoSanPham = tatCaPhieu.stream()
                .collect(Collectors.groupingBy(p -> p.getChiTietDonHang().getMaChiTietDonHang()));

        Comparator<ThongTinBaoHanh> thuTu = switch (chuanHoaSapXep(sapXep)) {
            case HET_HAN_GAN_NHAT -> Comparator.comparing(ThongTinBaoHanh::ngayHetHan);
            case HET_HAN_XA_NHAT -> Comparator.comparing(ThongTinBaoHanh::ngayHetHan).reversed();
            default -> Comparator.comparing(ThongTinBaoHanh::ngayMua).reversed();
        };

        return chiTietDonHangRepository.sanPhamDaNhan(email).stream()
                .filter(ct -> trongKhoang(ct.getDonHang().getNgayDat().toLocalDate(), tuNgay, denNgay))
                .map(ct -> chuyen(ct, phieuTheoSanPham.getOrDefault(ct.getMaChiTietDonHang(), List.of())))
                .sorted(thuTu.thenComparing(ThongTinBaoHanh::maChiTiet))
                .toList();
    }

    @Transactional
    public PhieuBaoHanh guiYeuCau(String email, Integer maChiTiet, String moTaLoi) {
        String moTa = moTaLoi == null ? "" : moTaLoi.trim();
        if (moTa.isEmpty()) {
            throw new NghiepVuException("Vui lòng mô tả lỗi của sản phẩm.");
        }
        if (moTa.length() > TOI_DA_KY_TU) {
            throw new NghiepVuException("Mô tả lỗi không được vượt quá " + TOI_DA_KY_TU + " ký tự.");
        }
        ChiTietDonHang ct = maChiTiet == null ? null : chiTietDonHangRepository.findById(maChiTiet).orElse(null);

        if (ct == null || !ct.getDonHang().getTaiKhoan().getEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (ct.getDonHang().getTrangThai() != TrangThaiDonHang.DA_GIAO) {
            throw new NghiepVuException("Chỉ gửi yêu cầu bảo hành cho sản phẩm thuộc đơn đã giao.");
        }
        ThongTinBaoHanh tt = chuyen(ct, List.of());
        if (!tt.conHan()) {
            throw new NghiepVuException("Sản phẩm " + tt.tenSanPham() + " đã hết hạn bảo hành.");
        }

        if (phieuBaoHanhRepository.existsByChiTietDonHangMaChiTietDonHangAndTrangThaiIn(maChiTiet, CHO_TIEP_NHAN)) {
            throw new NghiepVuException("Sản phẩm này đang có yêu cầu bảo hành chờ nhân viên tiếp nhận.");
        }
        PhieuBaoHanh phieu = new PhieuBaoHanh();
        phieu.setChiTietDonHang(ct);
        phieu.setNgayYeuCau(LocalDateTime.now());
        phieu.setMoTaLoi(moTa);
        phieu.setTrangThai(TrangThaiBaoHanh.CHO_TIEP_NHAN);
        return phieuBaoHanhRepository.save(phieu);
    }

    private static boolean trongKhoang(LocalDate ngay, LocalDate tu, LocalDate den) {
        return (tu == null || !ngay.isBefore(tu)) && (den == null || !ngay.isAfter(den));
    }

    private ThongTinBaoHanh chuyen(ChiTietDonHang ct, List<PhieuBaoHanh> lichSu) {
        var d = ct.getDonHang();
        var sp = ct.getSanPham();
        LocalDateTime ngayNhan = d.getNgayGiao() != null ? d.getNgayGiao() : d.getNgayDat();
        int chinhHang = sp.getThoiGianBaoHanh() == null ? 0 : sp.getThoiGianBaoHanh();
        int moRong = sp.getThoiGianBaoHanhMoRong() == null ? 0 : sp.getThoiGianBaoHanhMoRong();
        return new ThongTinBaoHanh(ct.getMaChiTietDonHang(), d.getMaDonHang(), sp.getTenSanPham(), sp.getHinhAnh(),
                ct.getSoSeri(), d.getNgayDat(), ngayNhan, chinhHang, moRong,
                ngayNhan.plusMonths(chinhHang + moRong), lichSu);
    }
}
