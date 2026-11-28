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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BaoHanhDoiTraService {
    public static final int HAN_DOI_TRA_NGAY = 30;
    private static final int TOI_DA_KY_TU = 1000;
    private static final int TOI_DA_MO_TA_DOI_TRA = 800;

    /** Lý do đổi trả khách chọn trên trang yêu cầu đổi trả (KH21). */
    public static final List<String> LY_DO_DOI_TRA = List.of(
            "Sản phẩm bị lỗi, không hoạt động",
            "Hàng bị móp méo, hư hỏng khi nhận",
            "Giao sai sản phẩm, sai mẫu mã",
            "Thiếu phụ kiện, quà tặng kèm theo",
            "Sản phẩm không giống mô tả",
            "Không còn nhu cầu sử dụng");

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

    /** Một dòng sản phẩm trên trang yêu cầu đổi trả: có được chọn không, vì sao, phiếu gần nhất (nếu có). */
    public record DongDoiTra(ChiTietDonHang chiTiet, boolean duocChon, String lyDoKhong, PhieuDoiTra phieuGanNhat) {
    }

    public record TrangDoiTra(DonHang donHang, LocalDate hanDoiTra, boolean conHan, List<DongDoiTra> dsDong,
            List<PhieuDoiTra> dsPhieu) {
        public long soDongDuocChon() {
            return dsDong.stream().filter(DongDoiTra::duocChon).count();
        }
    }

    /** KH21: dữ liệu cho trang yêu cầu đổi trả của một đơn hàng. */
    @Transactional(readOnly = true)
    public TrangDoiTra trangDoiTra(String email, Integer maDonHang) {
        List<ChiTietDonHang> dsChiTiet = chiTietCuaKhach(email, maDonHang);
        DonHang donHang = dsChiTiet.get(0).getDonHang();
        boolean daGiao = donHang.getTrangThai() == TrangThaiDonHang.DA_GIAO;
        LocalDate han = hanDoiTra(dsChiTiet.get(0));
        boolean conHan = daGiao && han != null && !LocalDate.now().isAfter(han);

        List<PhieuDoiTra> dsPhieu = doiTraRepository.findByChiTietDonHangDonHangMaDonHangOrderByNgayYeuCauDesc(maDonHang);
        Map<Integer, PhieuDoiTra> phieuGanNhat = new HashMap<>();
        for (PhieuDoiTra p : dsPhieu) {
            phieuGanNhat.putIfAbsent(p.getChiTietDonHang().getMaChiTietDonHang(), p);
        }

        List<DongDoiTra> dsDong = dsChiTiet.stream().map(ct -> {
            PhieuDoiTra phieu = phieuGanNhat.get(ct.getMaChiTietDonHang());
            String lyDo = null;
            if (!daGiao) {
                lyDo = "Đơn hàng chưa giao thành công";
            } else if (!conHan) {
                lyDo = "Đã quá hạn đổi trả";
            } else if (phieu != null && DT_DANG_MO.contains(phieu.getTrangThai())) {
                lyDo = "Đang có yêu cầu đổi trả";
            }
            return new DongDoiTra(ct, lyDo == null, lyDo, phieu);
        }).toList();
        return new TrangDoiTra(donHang, han, conHan, dsDong, dsPhieu);
    }

    /**
     * KH21: gửi yêu cầu đổi trả cho nhiều sản phẩm của cùng một đơn trong một lần.
     * Mỗi sản phẩm được lập một phiếu đổi trả riêng; chạy trong một transaction nên
     * chỉ cần một sản phẩm không hợp lệ là không phiếu nào được lưu.
     */
    @Transactional
    public List<PhieuDoiTra> guiDoiTraNhieu(String email, Integer maDonHang, List<Integer> dsMaChiTiet,
            String lyDo, String moTa) {
        if (dsMaChiTiet == null || dsMaChiTiet.isEmpty()) {
            throw new NghiepVuException("Vui lòng chọn ít nhất một sản phẩm cần đổi trả.");
        }
        if (lyDo == null || !LY_DO_DOI_TRA.contains(lyDo)) {
            throw new NghiepVuException("Vui lòng chọn lý do đổi trả.");
        }
        String chiTietMoTa = moTa == null ? "" : moTa.trim();
        if (chiTietMoTa.length() > TOI_DA_MO_TA_DOI_TRA) {
            throw new NghiepVuException("Mô tả không được vượt quá " + TOI_DA_MO_TA_DOI_TRA + " ký tự.");
        }
        String noiDung = chiTietMoTa.isEmpty() ? lyDo : lyDo + ". " + chiTietMoTa;

        Map<Integer, ChiTietDonHang> theoMa = new HashMap<>();
        for (ChiTietDonHang ct : chiTietCuaKhach(email, maDonHang)) {
            theoMa.put(ct.getMaChiTietDonHang(), ct);
        }
        LocalDate homNay = LocalDate.now();
        LocalDateTime bayGio = LocalDateTime.now();
        List<PhieuDoiTra> ketQua = new ArrayList<>();
        for (Integer ma : new LinkedHashSet<>(dsMaChiTiet)) {
            ChiTietDonHang ct = theoMa.get(ma);
            if (ct == null) {
                throw new NghiepVuException("Sản phẩm đã chọn không thuộc đơn hàng này.");
            }
            if (ct.getDonHang().getTrangThai() != TrangThaiDonHang.DA_GIAO) {
                throw new NghiepVuException("Chỉ gửi yêu cầu được cho sản phẩm thuộc đơn đã giao.");
            }
            LocalDate han = hanDoiTra(ct);
            if (han == null || homNay.isAfter(han)) {
                throw new NghiepVuException("Đơn hàng đã quá " + HAN_DOI_TRA_NGAY + " ngày đổi trả.");
            }
            if (doiTraRepository.existsByChiTietDonHangMaChiTietDonHangAndTrangThaiIn(ma, DT_DANG_MO)) {
                throw new NghiepVuException("Sản phẩm " + ct.getSanPham().getTenSanPham()
                        + " đang có yêu cầu đổi trả chưa xử lý xong.");
            }
            PhieuDoiTra phieu = new PhieuDoiTra();
            phieu.setChiTietDonHang(ct);
            phieu.setNgayYeuCau(bayGio);
            phieu.setLyDoDoiTra(noiDung);
            phieu.setTrangThai(TrangThaiDoiTra.CHO_TIEP_NHAN);
            ketQua.add(doiTraRepository.save(phieu));
        }
        return ketQua;
    }

    private List<ChiTietDonHang> chiTietCuaKhach(String email, Integer maDonHang) {
        List<ChiTietDonHang> ds = maDonHang == null ? List.of()
                : chiTietDonHangRepository.findByDonHangMaDonHang(maDonHang);
        if (ds.isEmpty() || !ds.get(0).getDonHang().getTaiKhoan().getEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ds;
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
