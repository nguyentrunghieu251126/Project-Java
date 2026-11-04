package project.dien_may.project_java.service.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.dien_may.project_java.entity.DonHang;
import project.dien_may.project_java.entity.TaiKhoan;
import project.dien_may.project_java.entity.TrangThaiDonHang;
import project.dien_may.project_java.entity.TrangThaiYeuCauNhap;
import project.dien_may.project_java.entity.YeuCauNhapHang;
import project.dien_may.project_java.repository.DonHangRepository;
import project.dien_may.project_java.repository.TaiKhoanRepository;
import project.dien_may.project_java.repository.YeuCauNhapHangRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Việc của quản trị viên ở khâu kho: xem đơn hàng và duyệt yêu cầu nhập (QT17, QT19 – QT21). */
@Service
public class KhoAdminService {

    private final DonHangRepository donHangRepository;
    private final YeuCauNhapHangRepository yeuCauRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    public KhoAdminService(DonHangRepository donHangRepository, YeuCauNhapHangRepository yeuCauRepository,
                           TaiKhoanRepository taiKhoanRepository) {
        this.donHangRepository = donHangRepository;
        this.yeuCauRepository = yeuCauRepository;
        this.taiKhoanRepository = taiKhoanRepository;
    }

    @Transactional(readOnly = true)
    public Page<DonHang> timDonHang(String tuKhoa, TrangThaiDonHang trangThai, LocalDate tuNgay, LocalDate denNgay,
                                    Pageable pageable) {
        Integer maDon = tuKhoa != null && tuKhoa.matches("\\d{1,9}") ? Integer.valueOf(tuKhoa) : null;
        LocalDateTime tu = tuNgay == null ? null : tuNgay.atStartOfDay();
        LocalDateTime den = denNgay == null ? null : denNgay.plusDays(1).atStartOfDay();
        return donHangRepository.timKiem(tuKhoa, maDon, trangThai, tu, den, pageable);
    }

    @Transactional
    public void duyet(Integer ma, String emailNguoiDuyet) {
        xuLy(ma, TrangThaiYeuCauNhap.DA_DUYET, emailNguoiDuyet, null);
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
