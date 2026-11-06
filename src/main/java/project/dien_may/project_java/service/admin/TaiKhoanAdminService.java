package project.dien_may.project_java.service.admin;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.web.server.ResponseStatusException;
import project.dien_may.project_java.dto.TaiKhoanForm;
import project.dien_may.project_java.entity.TaiKhoan;
import project.dien_may.project_java.entity.VaiTro;
import project.dien_may.project_java.repository.TaiKhoanRepository;
import project.dien_may.project_java.repository.VaiTroRepository;

import java.util.Locale;

/**
 * Quản trị viên không được khóa, xóa hay đổi vai trò của chính mình. Nhờ vậy người đang
 * thao tác luôn còn quyền, hệ thống không thể rơi vào tình trạng không còn quản trị viên.
 */
@Service
public class TaiKhoanAdminService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final VaiTroRepository vaiTroRepository;
    private final PasswordEncoder passwordEncoder;

    public TaiKhoanAdminService(TaiKhoanRepository taiKhoanRepository, VaiTroRepository vaiTroRepository,
                                PasswordEncoder passwordEncoder) {
        this.taiKhoanRepository = taiKhoanRepository;
        this.vaiTroRepository = vaiTroRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public TaiKhoanForm layForm(Integer ma) {
        return TaiKhoanForm.tu(timTheoMa(ma));
    }

    @Transactional(readOnly = true)
    public void kiemTra(TaiKhoanForm form, Errors errors) {
        if (form.getMaTaiKhoan() == null && form.getMatKhau() == null) {
            errors.rejectValue("matKhau", "matKhau.batBuoc", "Vui lòng nhập mật khẩu.");
        }
        if (form.getEmail() == null) {
            return;
        }
        String email = chuanHoaEmail(form.getEmail());
        boolean trung = form.getMaTaiKhoan() == null
                ? taiKhoanRepository.daTonTaiEmail(email)
                : taiKhoanRepository.existsByEmailIgnoreCaseAndMaTaiKhoanNot(email, form.getMaTaiKhoan());
        if (trung) {
            errors.rejectValue("email", "email.trung", "Email này đã được sử dụng.");
        }
    }

    @Transactional
    public void them(TaiKhoanForm form) {
        TaiKhoan tk = new TaiKhoan();
        tk.setEmail(chuanHoaEmail(form.getEmail()));
        tk.setVaiTro(timVaiTro(form.getMaVaiTro()));
        tk.setMatKhau(passwordEncoder.encode(form.getMatKhau()));
        ganThongTin(tk, form);
        taiKhoanRepository.save(tk);
    }

    @Transactional
    public void capNhat(Integer ma, TaiKhoanForm form, String emailNguoiThaoTac) {
        TaiKhoan tk = timTheoMa(ma);
        String email = chuanHoaEmail(form.getEmail());
        if (laChinhMinh(tk, emailNguoiThaoTac)) {
            if (!tk.getVaiTro().getMaVaiTro().equals(form.getMaVaiTro())) {
                throw new NghiepVuException("Bạn không thể tự đổi vai trò của mình.");
            }
            if (!tk.getEmail().equalsIgnoreCase(email)) {
                throw new NghiepVuException("Bạn không thể tự đổi email đăng nhập của mình.");
            }
        }
        tk.setEmail(email);
        tk.setVaiTro(timVaiTro(form.getMaVaiTro()));
        if (form.getMatKhau() != null) {
            tk.setMatKhau(passwordEncoder.encode(form.getMatKhau()));
        }
        ganThongTin(tk, form);
    }

    /** @return trạng thái mới: true là đang hoạt động */
    @Transactional
    public boolean khoaHoacMoKhoa(Integer ma, String emailNguoiThaoTac) {
        TaiKhoan tk = timTheoMa(ma);
        if (laChinhMinh(tk, emailNguoiThaoTac)) {
            throw new NghiepVuException("Bạn không thể tự khóa tài khoản của mình.");
        }
        tk.setTrangThai(!tk.isTrangThai());
        return tk.isTrangThai();
    }

    @Transactional
    public void xoa(Integer ma, String emailNguoiThaoTac) {
        TaiKhoan tk = timTheoMa(ma);
        if (laChinhMinh(tk, emailNguoiThaoTac)) {
            throw new NghiepVuException("Bạn không thể tự xóa tài khoản của mình.");
        }
        if (taiKhoanRepository.daPhatSinhChungTu(ma) == 1) {
            throw new NghiepVuException("Tài khoản " + tk.getEmail()
                    + " đã có đơn hàng hoặc chứng từ nên không thể xóa. Hãy khóa tài khoản này.");
        }
        taiKhoanRepository.xoaGioHang(ma);
        taiKhoanRepository.delete(tk);
    }

    private void ganThongTin(TaiKhoan tk, TaiKhoanForm form) {
        tk.setHoTen(form.getHoTen());
        tk.setSoDienThoai(form.getSoDienThoai());
        tk.setDiaChi(form.getDiaChi());
    }

    private boolean laChinhMinh(TaiKhoan tk, String emailNguoiThaoTac) {
        return tk.getEmail().equalsIgnoreCase(emailNguoiThaoTac);
    }

    private String chuanHoaEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private VaiTro timVaiTro(Integer ma) {
        return vaiTroRepository.findById(ma)
                .orElseThrow(() -> new NghiepVuException("Vai trò không tồn tại."));
    }

    private TaiKhoan timTheoMa(Integer ma) {
        return taiKhoanRepository.findById(ma)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
