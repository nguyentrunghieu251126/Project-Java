package project.dien_may.project_java.service.muahang;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.dien_may.project_java.dto.muahang.HoTroForm;
import project.dien_may.project_java.entity.DonHang;
import project.dien_may.project_java.entity.TaiKhoan;
import project.dien_may.project_java.entity.TrangThaiHoTro;
import project.dien_may.project_java.entity.YeuCauHoTro;
import project.dien_may.project_java.repository.DonHangKhachHangRepository;
import project.dien_may.project_java.repository.TaiKhoanRepository;
import project.dien_may.project_java.repository.YeuCauHoTroRepository;
import project.dien_may.project_java.service.admin.NghiepVuException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** TV3 – KH23: khách gửi và xem lại các yêu cầu hỗ trợ của mình. */
@Service
public class HoTroService {

    /** Chống gửi dồn dập: tối đa số yêu cầu mỗi ngày cho một tài khoản. */
    public static final int TOI_DA_MOI_NGAY = 5;

    private final YeuCauHoTroRepository yeuCauRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final DonHangKhachHangRepository donHangRepository;

    public HoTroService(YeuCauHoTroRepository yeuCauRepository, TaiKhoanRepository taiKhoanRepository,
                        DonHangKhachHangRepository donHangRepository) {
        this.yeuCauRepository = yeuCauRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.donHangRepository = donHangRepository;
    }

    @Transactional(readOnly = true)
    public List<YeuCauHoTro> danhSach(String email) {
        return yeuCauRepository.cuaKhach(email);
    }

    /** Các đơn gần đây để khách chọn trong ô "Đơn hàng liên quan". */
    @Transactional(readOnly = true)
    public List<DonHang> donGanDay(String email) {
        return donHangRepository.ganDayCuaKhach(email, PageRequest.of(0, 10));
    }

    /** @return true nếu đơn thuộc về khách (dùng khi điền sẵn mã đơn từ trang chi tiết đơn) */
    @Transactional(readOnly = true)
    public boolean laDonCuaKhach(String email, Integer maDon) {
        return maDon != null && donHangRepository.timCuaKhach(maDon, email).isPresent();
    }

    @Transactional
    public Integer gui(String email, HoTroForm form) {
        if (!HoTroForm.DS_CHU_DE.contains(form.getChuDe())) {
            throw new NghiepVuException("Chủ đề không hợp lệ.");
        }
        if (yeuCauRepository.demTuLuc(email, LocalDate.now().atStartOfDay()) >= TOI_DA_MOI_NGAY) {
            throw new NghiepVuException("Bạn đã gửi " + TOI_DA_MOI_NGAY
                    + " yêu cầu hôm nay. Vui lòng chờ phản hồi hoặc gọi hotline 1800 1234.");
        }
        String tieuDe = form.getChuDe();
        if (form.getMaDon() != null) {
            if (!laDonCuaKhach(email, form.getMaDon())) {
                throw new NghiepVuException("Đơn hàng đã chọn không thuộc tài khoản của bạn.");
            }
            tieuDe += " – đơn #" + String.format("DH%06d", form.getMaDon());
        }
        TaiKhoan taiKhoan = taiKhoanRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy tài khoản, vui lòng đăng nhập lại."));
        YeuCauHoTro yeuCau = new YeuCauHoTro();
        yeuCau.setTaiKhoan(taiKhoan);
        yeuCau.setTieuDe(tieuDe);
        yeuCau.setNoiDung(form.getNoiDung().trim());
        yeuCau.setNgayGui(LocalDateTime.now());
        yeuCau.setTrangThai(TrangThaiHoTro.MOI);
        return yeuCauRepository.save(yeuCau).getMaYeuCauHoTro();
    }
}
