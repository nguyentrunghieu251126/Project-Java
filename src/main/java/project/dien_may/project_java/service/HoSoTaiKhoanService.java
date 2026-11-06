package project.dien_may.project_java.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.dien_may.project_java.dto.CapNhatTaiKhoanForm;
import project.dien_may.project_java.entity.TaiKhoan;
import project.dien_may.project_java.repository.TaiKhoanRepository;

@Service
public class HoSoTaiKhoanService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final PasswordEncoder passwordEncoder;

    public HoSoTaiKhoanService(TaiKhoanRepository taiKhoanRepository, PasswordEncoder passwordEncoder) {
        this.taiKhoanRepository = taiKhoanRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Chi tra ve cac cot can hien thi; khong dua MatKhau ra view.
    public record ThongTinHoSo(String hoTen, String email, String soDienThoai, String diaChi) { }

    public enum KetQuaDoiMatKhau {
        THANH_CONG, MAT_KHAU_HIEN_TAI_SAI, TRUNG_MAT_KHAU_CU
    }

    @Transactional(readOnly = true)
    public ThongTinHoSo layHoSo(String emailDangNhap) {
        TaiKhoan tk = taiKhoanHienTai(emailDangNhap);
        return new ThongTinHoSo(tk.getHoTen(), tk.getEmail(), tk.getSoDienThoai(), tk.getDiaChi());
    }

    @Transactional
    public void capNhatHoSo(String emailDangNhap, CapNhatTaiKhoanForm form) {
        TaiKhoan tk = taiKhoanHienTai(emailDangNhap);
        tk.setHoTen(form.getHoTen().trim());
        tk.setSoDienThoai(chuanHoaTuyChon(form.getSoDienThoai()));
        tk.setDiaChi(chuanHoaTuyChon(form.getDiaChi()));
        taiKhoanRepository.save(tk);
    }

    @Transactional
    public KetQuaDoiMatKhau doiMatKhau(String emailDangNhap,
                                      String matKhauHienTai, String matKhauMoi) {
        TaiKhoan tk = taiKhoanHienTai(emailDangNhap);
        if (!passwordEncoder.matches(matKhauHienTai, tk.getMatKhau())) {
            return KetQuaDoiMatKhau.MAT_KHAU_HIEN_TAI_SAI;
        }
        if (passwordEncoder.matches(matKhauMoi, tk.getMatKhau())) {
            return KetQuaDoiMatKhau.TRUNG_MAT_KHAU_CU;
        }
        tk.setMatKhau(passwordEncoder.encode(matKhauMoi));
        taiKhoanRepository.save(tk);
        return KetQuaDoiMatKhau.THANH_CONG;
    }

    private TaiKhoan taiKhoanHienTai(String emailDangNhap) {
        if (emailDangNhap == null || emailDangNhap.isBlank()) {
            throw new IllegalStateException("Không tìm thấy phiên đăng nhập hợp lệ.");
        }
        TaiKhoan tk = taiKhoanRepository.findByEmailIgnoreCase(emailDangNhap)
                .orElseThrow(() -> new IllegalStateException("Tài khoản không còn tồn tại."));
        if (!tk.isTrangThai()) {
            throw new IllegalStateException("Tài khoản đang bị khóa.");
        }
        return tk;
    }

    private String chuanHoaTuyChon(String giaTri) {
        return giaTri == null || giaTri.isBlank() ? null : giaTri.trim();
    }
}
