package project.dien_may.project_java.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public class QuenMatKhauForm {

    @NotBlank(message = "Vui lòng nhập email.")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự.")
    @Pattern(regexp = "^$|^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "Email không đúng định dạng.")
    private String email;

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Pattern(regexp = "^$|^0[0-9]{9,10}$", message = "Số điện thoại phải có 10 hoặc 11 chữ số và bắt đầu bằng số 0.")
    private String soDienThoai;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới.")
    @Pattern(regexp = "^$|^[\\x21-\\x7E]{8,72}$",
            message = "Mật khẩu mới phải có từ 8 đến 72 ký tự, không dấu và không có khoảng trắng.")
    private String matKhauMoi;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu mới.")
    private String xacNhanMatKhau;

    public QuenMatKhauForm() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public String getMatKhauMoi() {
        return matKhauMoi;
    }

    public void setMatKhauMoi(String matKhauMoi) {
        this.matKhauMoi = matKhauMoi;
    }

    public String getXacNhanMatKhau() {
        return xacNhanMatKhau;
    }

    public void setXacNhanMatKhau(String xacNhanMatKhau) {
        this.xacNhanMatKhau = xacNhanMatKhau;
    }
}
