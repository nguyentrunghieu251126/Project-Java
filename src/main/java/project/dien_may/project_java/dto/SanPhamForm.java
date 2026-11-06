package project.dien_may.project_java.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import project.dien_may.project_java.entity.SanPham;

import java.math.BigDecimal;

@Getter
@Setter
public class SanPhamForm {

    private Integer maSanPham;

    /** Phiên bản lúc mở form, để phát hiện người khác đã sửa trong lúc đang nhập. */
    private Integer phienBan;

    @NotBlank(message = "Vui lòng nhập mã model.")
    @Size(max = 50, message = "Mã model không được vượt quá 50 ký tự.")
    // Cột VARCHAR: chỉ nhận ký tự ASCII để không bị mất dấu khi lưu
    @Pattern(regexp = "^$|^[\\x21-\\x7E]+$", message = "Mã model không dấu và không có khoảng trắng.")
    private String maModel;

    @NotBlank(message = "Vui lòng nhập tên sản phẩm.")
    @Size(max = 200, message = "Tên sản phẩm không được vượt quá 200 ký tự.")
    private String tenSanPham;

    @NotBlank(message = "Vui lòng nhập thương hiệu.")
    @Size(max = 50, message = "Thương hiệu không được vượt quá 50 ký tự.")
    private String thuongHieu;

    @NotNull(message = "Vui lòng chọn danh mục.")
    private Integer maDanhMuc;

    @NotNull(message = "Vui lòng nhập đơn giá.")
    @DecimalMin(value = "0", message = "Đơn giá không được âm.")
    @Digits(integer = 15, fraction = 0, message = "Đơn giá phải là số nguyên, tối đa 15 chữ số.")
    private BigDecimal donGia;

    /** Chỉ dùng khi thêm mới; sau đó tồn kho chỉ thay đổi qua phiếu nhập, phiếu xuất. */
    @Min(value = 0, message = "Số lượng không được âm.")
    @Max(value = 1000000, message = "Số lượng không được vượt quá 1.000.000.")
    private int soLuong;

    @Min(value = 0, message = "Mức tồn tối thiểu không được âm.")
    @Max(value = 1000000, message = "Mức tồn tối thiểu không được vượt quá 1.000.000.")
    private int mucTonToiThieu = 5;

    @Min(value = 0, message = "Thời gian bảo hành không được âm.")
    @Max(value = 240, message = "Thời gian bảo hành không được vượt quá 240 tháng.")
    private int thoiGianBaoHanh = 12;

    @Size(max = 255, message = "Đường dẫn ảnh không được vượt quá 255 ký tự.")
    @Pattern(regexp = "^$|^[\\x21-\\x7E]+$", message = "Đường dẫn ảnh không dấu và không có khoảng trắng.")
    private String hinhAnh;

    @Size(max = 4000, message = "Mô tả không được vượt quá 4000 ký tự.")
    private String moTa;

    private boolean dangKinhDoanh = true;

    public static SanPhamForm tu(SanPham sp) {
        SanPhamForm f = new SanPhamForm();
        f.maSanPham = sp.getMaSanPham();
        f.phienBan = sp.getPhienBan();
        f.maModel = sp.getMaModel();
        f.tenSanPham = sp.getTenSanPham();
        f.thuongHieu = sp.getThuongHieu();
        f.maDanhMuc = sp.getDanhMuc().getMaDanhMuc();
        f.donGia = sp.getDonGia();
        f.soLuong = sp.getSoLuong();
        f.mucTonToiThieu = sp.getMucTonToiThieu();
        f.thoiGianBaoHanh = sp.getThoiGianBaoHanh();
        f.hinhAnh = sp.getHinhAnh();
        f.moTa = sp.getMoTa();
        f.dangKinhDoanh = sp.isDangKinhDoanh();
        return f;
    }
}
