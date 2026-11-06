package project.dien_may.project_java.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CapNhatTaiKhoanForm {

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(min = 2, max = 100, message = "Họ và tên phải có từ 2 đến 100 ký tự.")
    private String hoTen;

    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải có 10 chữ số và bắt đầu bằng 0.")
    private String soDienThoai;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự.")
    private String diaChi;

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
}
