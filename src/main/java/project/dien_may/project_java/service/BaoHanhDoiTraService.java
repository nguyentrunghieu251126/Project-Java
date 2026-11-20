package project.dien_may.project_java.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.dien_may.project_java.entity.*;
import project.dien_may.project_java.repository.ChiTietDonHangRepository;
import project.dien_may.project_java.repository.PhieuBaoHanhRepository;
import project.dien_may.project_java.repository.PhieuDoiTraRepository;
import project.dien_may.project_java.repository.TaiKhoanRepository;
import project.dien_may.project_java.service.admin.NghiepVuException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class BaoHanhDoiTraService {
    public static final int HAN_DOI_TRA_NGAY = 30;
    private static final int TOI_DA_KY_TU = 1000;

    private static final Set<TrangThaiBaoHanh> BH_DANG_MO = Set.of(TrangThaiBaoHanh.CHO_TIEP_NHAN,
            TrangThaiBaoHanh.DANG_BAO_HANH);
    private static final Set<TrangThaiDoiTra> DT_DANG_MO = Set.of(TrangThaiDoiTra.CHO_TIEP_NHAN,
            TrangThaiDoiTra.CHAP_NHAN);

    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final PhieuBaoHanhRepository baoHanhRepository;
    private final PhieuDoiTraRepository doiTraRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    public BaoHanhDoiTraService(ChiTietDonHangRepository chiTietDonHangRepository,
            PhieuBaoHanhRepository baoHanhRepository, PhieuDoiTraRepository doiTraRepository,
            TaiKhoanRepository taiKhoanRepository) {
        this.chiTietDonHangRepository = chiTietDonHangRepository;
        this.baoHanhRepository = baoHanhRepository;
        this.doiTraRepository = doiTraRepository;
        this.taiKhoanRepository = taiKhoanRepository;
    }

    public record SanPhamDaNhan(ChiTietDonHang chiTiet, LocalDate hanBaoHanh, LocalDate hanDoiTra,
            boolean duocBaoHanh, boolean duocDoiTra, String lyDoKhongBaoHanh,
            String lyDoKhongDoiTra) {
    }

    @Transactional(readOnly = true)
    public List<SanPhamDaNhan> sanPhamDaNhan(String email) {
        LocalDate homNay = LocalDate.now();
        return chiTietDonHangRepository.sanPhamDaNhan(email).stream().map(ct -> {
            LocalDate hanBH = hanBaoHanh(ct);
            LocalDate hanDT = hanDoiTra(ct);
            String lyDoBH = null;
            if (hanBH == null || homNay.isAfter(hanBH)) {
                lyDoBH = "Hết hạn bảo hành";
            } else if (baoHanhRepository.existsByChiTietDonHangMaChiTietDonHangAndTrangThaiIn(
                    ct.getMaChiTietDonHang(), BH_DANG_MO)) {
                lyDoBH = "Đang có yêu cầu bảo hành";
            }
            String lyDoDT = null;
            if (hanDT == null || homNay.isAfter(hanDT)) {
                lyDoDT = "Quá hạn đổi trả";
            } else if (doiTraRepository.existsByChiTietDonHangMaChiTietDonHangAndTrangThaiIn(
                    ct.getMaChiTietDonHang(), DT_DANG_MO)) {
                lyDoDT = "Đang có yêu cầu đổi trả";
            }
            return new SanPhamDaNhan(ct, hanBH, hanDT, lyDoBH == null, lyDoDT == null, lyDoBH, lyDoDT);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<PhieuBaoHanh> lichSuBaoHanh(String email) {
        return baoHanhRepository.findByChiTietDonHangDonHangTaiKhoanEmailIgnoreCaseOrderByNgayYeuCauDesc(email);
    }

    @Transactional(readOnly = true)
    public List<PhieuDoiTra> lichSuDoiTra(String email) {
        return doiTraRepository.findByChiTietDonHangDonHangTaiKhoanEmailIgnoreCaseOrderByNgayYeuCauDesc(email);
    }

    @Transactional
    public PhieuBaoHanh guiBaoHanh(String email, Integer maChiTiet, String moTaLoi) {
        String moTa = kiemTraNoiDung(moTaLoi, "mô tả lỗi");
        ChiTietDonHang ct = timSanPhamCuaKhach(email, maChiTiet);
        LocalDate han = hanBaoHanh(ct);
        if (han == null || LocalDate.now().isAfter(han)) {
            throw new NghiepVuException("Sản phẩm " + ct.getSanPham().getTenSanPham() + " đã hết hạn bảo hành.");
        }
        if (baoHanhRepository.existsByChiTietDonHangMaChiTietDonHangAndTrangThaiIn(maChiTiet, BH_DANG_MO)) {
            throw new NghiepVuException("Sản phẩm này đang có yêu cầu bảo hành chưa xử lý xong.");
        }
        PhieuBaoHanh phieu = new PhieuBaoHanh();
        phieu.setChiTietDonHang(ct);
        phieu.setNgayYeuCau(LocalDateTime.now());
        phieu.setMoTaLoi(moTa);
        phieu.setTrangThai(TrangThaiBaoHanh.CHO_TIEP_NHAN);
        return baoHanhRepository.save(phieu);
    }

    @Transactional
    public PhieuDoiTra guiDoiTra(String email, Integer maChiTiet, String lyDoDoiTra) {
        String lyDo = kiemTraNoiDung(lyDoDoiTra, "lý do đổi trả");
        ChiTietDonHang ct = timSanPhamCuaKhach(email, maChiTiet);
        LocalDate han = hanDoiTra(ct);
        if (han == null || LocalDate.now().isAfter(han)) {
            throw new NghiepVuException("Sản phẩm " + ct.getSanPham().getTenSanPham()
                    + " đã quá " + HAN_DOI_TRA_NGAY + " ngày đổi trả.");
        }
        if (doiTraRepository.existsByChiTietDonHangMaChiTietDonHangAndTrangThaiIn(maChiTiet, DT_DANG_MO)) {
            throw new NghiepVuException("Sản phẩm này đang có yêu cầu đổi trả chưa xử lý xong.");
        }
        PhieuDoiTra phieu = new PhieuDoiTra();
        phieu.setChiTietDonHang(ct);
        phieu.setNgayYeuCau(LocalDateTime.now());
        phieu.setLyDoDoiTra(lyDo);
        phieu.setTrangThai(TrangThaiDoiTra.CHO_TIEP_NHAN);
        return doiTraRepository.save(phieu);
    }

    @Transactional(readOnly = true)
    public Page<PhieuBaoHanh> timBaoHanh(TrangThaiBaoHanh trangThai, Pageable pageable) {
        return baoHanhRepository.timKiem(trangThai, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PhieuDoiTra> timDoiTra(TrangThaiDoiTra trangThai, Pageable pageable) {
        return doiTraRepository.timKiem(trangThai, pageable);
    }

    @Transactional(readOnly = true)
    public long demBaoHanhChoTiepNhan() {
        return baoHanhRepository.countByTrangThai(TrangThaiBaoHanh.CHO_TIEP_NHAN);
    }

    @Transactional(readOnly = true)
    public long demDoiTraChoTiepNhan() {
        return doiTraRepository.countByTrangThai(TrangThaiDoiTra.CHO_TIEP_NHAN);
    }

    @Transactional
    public void tiepNhanBaoHanh(Integer ma, String emailNhanVien) {
        PhieuBaoHanh phieu = timBaoHanh(ma, TrangThaiBaoHanh.CHO_TIEP_NHAN);
        phieu.setNhanVien(timNhanVien(emailNhanVien));
        phieu.setNgayTiepNhan(LocalDateTime.now());
        phieu.setTrangThai(TrangThaiBaoHanh.DANG_BAO_HANH);
    }

    @Transactional
    public void hoanThanhBaoHanh(Integer ma, String emailNhanVien, String ketQua) {
        String noiDung = kiemTraNoiDung(ketQua, "kết quả xử lý");
        PhieuBaoHanh phieu = timBaoHanh(ma, TrangThaiBaoHanh.DANG_BAO_HANH);
        phieu.setNhanVien(timNhanVien(emailNhanVien));
        phieu.setKetQuaXuLy(noiDung);
        phieu.setTrangThai(TrangThaiBaoHanh.HOAN_THANH);
    }

    @Transactional
    public void tuChoiBaoHanh(Integer ma, String emailNhanVien, String lyDo) {
        String noiDung = kiemTraNoiDung(lyDo, "lý do từ chối");
        PhieuBaoHanh phieu = timBaoHanh(ma, TrangThaiBaoHanh.CHO_TIEP_NHAN);
        phieu.setNhanVien(timNhanVien(emailNhanVien));
        phieu.setNgayTiepNhan(LocalDateTime.now());
        phieu.setKetQuaXuLy(noiDung);
        phieu.setTrangThai(TrangThaiBaoHanh.TU_CHOI);
    }

    @Transactional
    public void chapNhanDoiTra(Integer ma, String emailNhanVien) {
        PhieuDoiTra phieu = timDoiTra(ma, TrangThaiDoiTra.CHO_TIEP_NHAN);
        phieu.setNhanVien(timNhanVien(emailNhanVien));
        phieu.setNgayTiepNhan(LocalDateTime.now());
        phieu.setTrangThai(TrangThaiDoiTra.CHAP_NHAN);
    }

    @Transactional
    public void hoanThanhDoiTra(Integer ma, String emailNhanVien, String ketQua) {
        String noiDung = kiemTraNoiDung(ketQua, "kết quả xử lý");
        PhieuDoiTra phieu = timDoiTra(ma, TrangThaiDoiTra.CHAP_NHAN);
        phieu.setNhanVien(timNhanVien(emailNhanVien));
        phieu.setKetQuaXuLy(noiDung);
        phieu.setTrangThai(TrangThaiDoiTra.HOAN_THANH);
    }

    @Transactional
    public void tuChoiDoiTra(Integer ma, String emailNhanVien, String lyDo) {
        String noiDung = kiemTraNoiDung(lyDo, "lý do từ chối");
        PhieuDoiTra phieu = timDoiTra(ma, TrangThaiDoiTra.CHO_TIEP_NHAN);
        phieu.setNhanVien(timNhanVien(emailNhanVien));
        phieu.setNgayTiepNhan(LocalDateTime.now());
        phieu.setKetQuaXuLy(noiDung);
        phieu.setTrangThai(TrangThaiDoiTra.TU_CHOI);
    }

    private LocalDate hanBaoHanh(ChiTietDonHang ct) {
        LocalDateTime ngayGiao = ct.getDonHang().getNgayGiao();
        Integer soThang = ct.getSanPham().getThoiGianBaoHanh();
        if (ngayGiao == null || soThang == null || soThang <= 0) {
            return null;
        }
        return ngayGiao.toLocalDate().plusMonths(soThang);
    }

    private LocalDate hanDoiTra(ChiTietDonHang ct) {
        LocalDateTime ngayGiao = ct.getDonHang().getNgayGiao();
        return ngayGiao == null ? null : ngayGiao.toLocalDate().plusDays(HAN_DOI_TRA_NGAY);
    }

    private ChiTietDonHang timSanPhamCuaKhach(String email, Integer maChiTiet) {
        ChiTietDonHang ct = maChiTiet == null ? null : chiTietDonHangRepository.findById(maChiTiet).orElse(null);
        if (ct == null || !ct.getDonHang().getTaiKhoan().getEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (ct.getDonHang().getTrangThai() != TrangThaiDonHang.DA_GIAO) {
            throw new NghiepVuException("Chỉ gửi yêu cầu được cho sản phẩm thuộc đơn đã giao.");
        }
        return ct;
    }

    private PhieuBaoHanh timBaoHanh(Integer ma, TrangThaiBaoHanh trangThaiCanCo) {
        PhieuBaoHanh phieu = baoHanhRepository.findById(ma)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (phieu.getTrangThai() != trangThaiCanCo) {
            throw new NghiepVuException("Phiếu bảo hành #" + ma + " không còn ở trạng thái \""
                    + trangThaiCanCo.getNhan() + "\".");
        }
        return phieu;
    }

    private PhieuDoiTra timDoiTra(Integer ma, TrangThaiDoiTra trangThaiCanCo) {
        PhieuDoiTra phieu = doiTraRepository.findById(ma)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (phieu.getTrangThai() != trangThaiCanCo) {
            throw new NghiepVuException("Phiếu đổi trả #" + ma + " không còn ở trạng thái \""
                    + trangThaiCanCo.getNhan() + "\".");
        }
        return phieu;
    }

    private TaiKhoan timNhanVien(String email) {
        return taiKhoanRepository.findByEmailIgnoreCase(email).orElseThrow();
    }

    private String kiemTraNoiDung(String noiDung, String tenTruong) {
        String giaTri = noiDung == null ? "" : noiDung.trim();
        if (giaTri.isEmpty()) {
            throw new NghiepVuException("Vui lòng nhập " + tenTruong + ".");
        }
        if (giaTri.length() > TOI_DA_KY_TU) {
            throw new NghiepVuException("Nội dung " + tenTruong + " không được vượt quá " + TOI_DA_KY_TU + " ký tự.");
        }
        return giaTri;
    }
}
