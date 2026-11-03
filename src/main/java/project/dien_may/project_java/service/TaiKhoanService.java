
package project.dien_may.project_java.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.dien_may.project_java.dto.DangKyForm;
import project.dien_may.project_java.entity.TaiKhoan;
import project.dien_may.project_java.entity.VaiTro;
import project.dien_may.project_java.repository.TaiKhoanRepository;
import project.dien_may.project_java.repository.VaiTroRepository;

import java.util.Locale;
import java.util.Optional;

@Service
public class TaiKhoanService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final VaiTroRepository vaiTroRepository;
    private final PasswordEncoder passwordEncoder;

    public TaiKhoanService(TaiKhoanRepository taiKhoanRepository, VaiTroRepository vaiTroRepository, PasswordEncoder passwordEncoder) {

        this.taiKhoanRepository = taiKhoanRepository;
        this.vaiTroRepository = vaiTroRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public boolean emailDaTonTai(String email) {

        if (email == null || email.isBlank()) {
            return false;
        }

        return taiKhoanRepository.daTonTaiEmail(email.trim().toLowerCase(Locale.ROOT)
        );
    }

    @Transactional
    public void dangKy(DangKyForm form) {

        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);

        if (taiKhoanRepository.daTonTaiEmail(email)) {throw new IllegalArgumentException("Email đã được sử dụng.");
        }

        VaiTro khachHang = vaiTroRepository.findByTenVaiTro("KHACH_HANG").orElseThrow(() -> new IllegalStateException("Không tìm thấy vai trò KHACH_HANG."));
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setHoTen(form.getHoTen().trim());
        taiKhoan.setEmail(email);
        String soDienThoai = form.getSoDienThoai();
        taiKhoan.setSoDienThoai(soDienThoai == null || soDienThoai.isBlank() ? null : soDienThoai.trim()
        );
        taiKhoan.setMatKhau(passwordEncoder.encode(form.getMatKhau())
        );
        taiKhoan.setVaiTro(khachHang);
        taiKhoan.setTrangThai(true);
        taiKhoanRepository.save(taiKhoan);
    }

    /** Kết quả của chức năng quên mật khẩu. */
    public enum KetQuaDatLaiMatKhau {
        THANH_CONG,
        SAI_THONG_TIN,
        TRUNG_MAT_KHAU_CU
    }

    /**
     * Quên mật khẩu: chỉ đổi khi email tồn tại, tài khoản đang hoạt động và số điện thoại khớp.
     * Không phân biệt sai email hay sai số điện thoại để người ngoài không dò được tài khoản.
     */
    @Transactional
    public KetQuaDatLaiMatKhau datLaiMatKhau(String email, String soDienThoai, String matKhauMoi) {

        if (email == null || soDienThoai == null || matKhauMoi == null
                || !matKhauMoi.matches("^[\\x21-\\x7E]{8,72}$")) {
            return KetQuaDatLaiMatKhau.SAI_THONG_TIN;
        }

        Optional<TaiKhoan> ketQua = taiKhoanRepository.findByEmailIgnoreCase(email.trim());
        if (ketQua.isEmpty()) {
            return KetQuaDatLaiMatKhau.SAI_THONG_TIN;
        }

        TaiKhoan taiKhoan = ketQua.get();
        if (!taiKhoan.isTrangThai()
                || taiKhoan.getSoDienThoai() == null
                || !taiKhoan.getSoDienThoai().trim().equals(soDienThoai.trim())) {
            return KetQuaDatLaiMatKhau.SAI_THONG_TIN;
        }

        if (passwordEncoder.matches(matKhauMoi, taiKhoan.getMatKhau())) {
            return KetQuaDatLaiMatKhau.TRUNG_MAT_KHAU_CU;
        }

        taiKhoan.setMatKhau(passwordEncoder.encode(matKhauMoi)); // lưu BCrypt (NFR04)
        taiKhoanRepository.save(taiKhoan);
        return KetQuaDatLaiMatKhau.THANH_CONG;
    }
}
