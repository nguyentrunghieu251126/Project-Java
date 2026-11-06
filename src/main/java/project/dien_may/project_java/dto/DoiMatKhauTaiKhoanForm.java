package project.dien_may.project_java.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class DoiMatKhauTaiKhoanForm {

    @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại.")
    private String matKhauHienTai;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới.")
    @Pattern(regexp = "^[\\x21-\\x7E]{8,72}$",
            message = "Mật khẩu mới phải có 8–72 ký tự, không chứa dấu cách hoặc ký tự có dấu.")
    private String matKhauMoi;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu mới.")
    private String xacNhanMatKhau;

    public String getMatKhauHienTai() { return matKhauHienTai; }
    public void setMatKhauHienTai(String matKhauHienTai) { this.matKhauHienTai = matKhauHienTai; }
    public String getMatKhauMoi() { return matKhauMoi; }
    public void setMatKhauMoi(String matKhauMoi) { this.matKhauMoi = matKhauMoi; }
    public String getXacNhanMatKhau() { return xacNhanMatKhau; }
    public void setXacNhanMatKhau(String xacNhanMatKhau) { this.xacNhanMatKhau = xacNhanMatKhau; }
}
