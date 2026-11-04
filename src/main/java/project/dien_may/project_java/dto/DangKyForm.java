
package project.dien_may.project_java.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DangKyForm {

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(
            max = 100,
            message = "Họ và tên không được vượt quá 100 ký tự."
    )
    private String hoTen;
    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Email không đúng định dạng.")
    @Size(
            max = 100,
            message = "Email không được vượt quá 100 ký tự."
    )
    private String email;
    @Pattern(
            regexp = "^$|^0[0-9]{9,10}$",
            message = "Số điện thoại phải có 10 hoặc 11 chữ số và bắt đầu bằng 0."
    )
    private String soDienThoai;
    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    // Chỉ nhận ký tự ASCII (1 ký tự = 1 byte) để không vượt giới hạn 72 BYTE của BCrypt
    @Pattern(
            regexp = "^$|^[\\x21-\\x7E]{8,72}$",
            message = "Mật khẩu phải có từ 8 đến 72 ký tự, không dấu và không có khoảng trắng."
    )
    private String matKhau;
    @NotBlank(message = "Vui lòng xác nhận mật khẩu.")
    private String xacNhanMatKhau;
    public DangKyForm() {
    }
    public String getHoTen() {
        return hoTen;
    }
    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
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
    public String getMatKhau() {
        return matKhau;
    }
    public void setMatKhau(String matKhau) {
        this.matKhau = matKhau;
    }
    public String getXacNhanMatKhau() {
        return xacNhanMatKhau;
    }
    public void setXacNhanMatKhau(String xacNhanMatKhau) {
        this.xacNhanMatKhau = xacNhanMatKhau;
    }
}
