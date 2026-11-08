package project.dien_may.project_java.service.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.dien_may.project_java.entity.ChiTietYeuCauNhapHang;
import project.dien_may.project_java.entity.DonHang;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.entity.TaiKhoan;
import project.dien_may.project_java.entity.TinhTrangTonKho;
import project.dien_may.project_java.entity.TrangThaiDonHang;
import project.dien_may.project_java.entity.TrangThaiYeuCauNhap;
import project.dien_may.project_java.entity.YeuCauNhapHang;
import project.dien_may.project_java.repository.ChiTietYeuCauNhapHangRepository;
import project.dien_may.project_java.repository.DonHangRepository;
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.repository.TaiKhoanRepository;
import project.dien_may.project_java.repository.YeuCauNhapHangRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Việc ở khâu kho: xem đơn hàng, lập và duyệt yêu cầu nhập (QT17, QT19 – QT21, NVK09). */
@Service
public class KhoAdminService {

    private final DonHangRepository donHangRepository;
    private final YeuCauNhapHangRepository yeuCauRepository;
    private final ChiTietYeuCauNhapHangRepository chiTietYeuCauRepository;
    private final SanPhamRepository sanPhamRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    public KhoAdminService(DonHangRepository donHangRepository, YeuCauNhapHangRepository yeuCauRepository,
                           ChiTietYeuCauNhapHangRepository chiTietYeuCauRepository,
                           SanPhamRepository sanPhamRepository, TaiKhoanRepository taiKhoanRepository) {
        this.donHangRepository = donHangRepository;
        this.yeuCauRepository = yeuCauRepository;
        this.chiTietYeuCauRepository = chiTietYeuCauRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.taiKhoanRepository = taiKhoanRepository;
    }

    private static final String VAI_TRO_QUAN_TRI = "QUAN_TRI";

    /** @param daNhapKho true khi phiếu được duyệt ngay và số lượng đã cộng vào tồn kho */
    public record KetQuaNhap(Integer maYeuCau, boolean daNhapKho) {
    }

    /**
     * Lập phiếu yêu cầu nhập cho một sản phẩm đang ở mức cảnh báo (sắp hết hoặc hết hàng).
     * Nhân viên kho gửi thì phiếu chờ duyệt; quản trị viên lập thì phiếu được duyệt luôn và hàng vào kho ngay.
     */
    @Transactional
    public KetQuaNhap lapYeuCauNhap(Integer maSanPham, Integer soLuong, String emailNguoiGui) {
        SanPham sanPham = sanPhamRepository.findById(maSanPham)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (sanPham.getTinhTrangTonKho() == TinhTrangTonKho.DU) {
            throw new NghiepVuException("Sản phẩm " + sanPham.getTenSanPham() + " vẫn đủ hàng, chưa cần yêu cầu nhập.");
        }
        if (soLuong == null || soLuong <= 0) {
            throw new NghiepVuException("Số lượng yêu cầu nhập phải lớn hơn 0.");
        }
        if (chiTietYeuCauRepository.existsBySanPhamMaSanPhamAndYeuCauTrangThai(maSanPham, TrangThaiYeuCauNhap.CHO_DUYET)) {
            throw new NghiepVuException("Sản phẩm " + sanPham.getTenSanPham() + " đã có yêu cầu nhập đang chờ duyệt.");
        }

        TaiKhoan nguoiGui = taiKhoanRepository.findByEmailIgnoreCase(emailNguoiGui).orElseThrow();
        boolean nhapNgay = VAI_TRO_QUAN_TRI.equals(nguoiGui.getVaiTro().getTenVaiTro());
        LocalDateTime bayGio = LocalDateTime.now();

        YeuCauNhapHang yeuCau = new YeuCauNhapHang();
        yeuCau.setTaiKhoan(nguoiGui);
        yeuCau.setNgayYeuCau(bayGio);
        if (nhapNgay) {
            yeuCau.setTrangThai(TrangThaiYeuCauNhap.DA_DUYET);
            yeuCau.setNguoiDuyet(nguoiGui);
            yeuCau.setNgayDuyet(bayGio);
        } else {
            yeuCau.setTrangThai(TrangThaiYeuCauNhap.CHO_DUYET);
        }
        yeuCauRepository.save(yeuCau);

        ChiTietYeuCauNhapHang chiTiet = new ChiTietYeuCauNhapHang();
        chiTiet.setYeuCau(yeuCau);
        chiTiet.setSanPham(sanPham);
        chiTiet.setSoLuongYeuCau(soLuong);
        chiTietYeuCauRepository.save(chiTiet);

        Integer maYeuCau = yeuCau.getMaYeuCauNhap();
        if (nhapNgay) {
            sanPhamRepository.congTonKho(maSanPham, soLuong);
        }
        return new KetQuaNhap(maYeuCau, nhapNgay);
    }

    @Transactional(readOnly = true)
    public Page<DonHang> timDonHang(String tuKhoa, TrangThaiDonHang trangThai, LocalDate tuNgay, LocalDate denNgay,
                                    Pageable pageable) {
        Integer maDon = tuKhoa != null && tuKhoa.matches("\\d{1,9}") ? Integer.valueOf(tuKhoa) : null;
        LocalDateTime tu = tuNgay == null ? null : tuNgay.atStartOfDay();
        LocalDateTime den = denNgay == null ? null : denNgay.plusDays(1).atStartOfDay();
        return donHangRepository.timKiem(tuKhoa, maDon, trangThai, tu, den, pageable);
    }

    /** Duyệt xong thì hàng trong phiếu được cộng vào tồn kho. */
    @Transactional
    public void duyet(Integer ma, String emailNguoiDuyet) {
        xuLy(ma, TrangThaiYeuCauNhap.DA_DUYET, emailNguoiDuyet, null);
        for (ChiTietYeuCauNhapHang chiTiet : chiTietYeuCauRepository.findByYeuCauMaYeuCauNhap(ma)) {
            sanPhamRepository.congTonKho(chiTiet.getSanPham().getMaSanPham(), chiTiet.getSoLuongYeuCau());
        }
    }

    @Transactional
    public void tuChoi(Integer ma, String emailNguoiDuyet, String lyDo) {
        if (lyDo == null) {
            throw new NghiepVuException("Vui lòng nhập lý do từ chối.");
        }
        if (lyDo.length() > 500) {
            throw new NghiepVuException("Lý do từ chối không được vượt quá 500 ký tự.");
        }
        xuLy(ma, TrangThaiYeuCauNhap.TU_CHOI, emailNguoiDuyet, lyDo);
    }

    private void xuLy(Integer ma, TrangThaiYeuCauNhap trangThai, String emailNguoiDuyet, String lyDo) {
        YeuCauNhapHang yeuCau = yeuCauRepository.findById(ma)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        TaiKhoan nguoiDuyet = taiKhoanRepository.findByEmailIgnoreCase(emailNguoiDuyet).orElseThrow();
        int soDong = yeuCauRepository.xuLy(yeuCau.getMaYeuCauNhap(), trangThai, nguoiDuyet, LocalDateTime.now(), lyDo);
        if (soDong == 0) {
            throw new NghiepVuException("Yêu cầu #" + ma + " đã được xử lý trước đó.");
        }
    }
}
