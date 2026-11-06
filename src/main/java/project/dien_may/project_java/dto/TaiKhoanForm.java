package project.dien_may.project_java.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import project.dien_may.project_java.entity.TaiKhoan;

@Getter
@Setter
public class TaiKhoanForm {

    private Integer maTaiKhoan;

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự.")
    private String hoTen;

    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Email không đúng định dạng.")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự.")
    private String email;

    @Pattern(regexp = "^$|^0[0-9]{9,10}$", message = "Số điện thoại phải có 10 hoặc 11 chữ số và bắt đầu bằng 0.")
    private String soDienThoai;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự.")
    private String diaChi;

    @NotNull(message = "Vui lòng chọn vai trò.")
    private Integer maVaiTro;

    /** Bắt buộc khi thêm mới; khi sửa, để trống nghĩa là giữ nguyên mật khẩu. */
    @Pattern(regexp = "^$|^[\\x21-\\x7E]{8,72}$",
            message = "Mật khẩu phải có từ 8 đến 72 ký tự, không dấu và không có khoảng trắng.")
    private String matKhau;

    public static TaiKhoanForm tu(TaiKhoan tk) {
        TaiKhoanForm f = new TaiKhoanForm();
        f.maTaiKhoan = tk.getMaTaiKhoan();
        f.hoTen = tk.getHoTen();
        f.email = tk.getEmail();
        f.soDienThoai = tk.getSoDienThoai();
        f.diaChi = tk.getDiaChi();
        f.maVaiTro = tk.getVaiTro().getMaVaiTro();
        return f;
    }
}
