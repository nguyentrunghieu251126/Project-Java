package project.dien_may.project_java.dto.muahang;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import project.dien_may.project_java.entity.SoDiaChi;

/** TV3 – form thêm / sửa địa chỉ trong sổ địa chỉ. */
@Getter
@Setter
public class DiaChiForm {

    /** null khi thêm mới. */
    private Integer maDiaChi;

    @NotBlank(message = "Vui lòng nhập họ tên người nhận.")
    @Size(max = 100, message = "Họ tên người nhận không được vượt quá 100 ký tự.")
    private String tenNguoiNhan;

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Pattern(regexp = "0\\d{9}", message = "Số điện thoại gồm 10 chữ số và bắt đầu bằng 0.")
    private String soDienThoai;

    @NotBlank(message = "Vui lòng chọn tỉnh / thành phố.")
    private String khuVuc;

    @NotBlank(message = "Vui lòng nhập địa chỉ chi tiết.")
    @Size(max = 200, message = "Địa chỉ chi tiết không được vượt quá 200 ký tự.")
    private String diaChiChiTiet;

    private String loaiDiaChi = SoDiaChi.NHA_RIENG;

    private boolean laMacDinh;

    public static DiaChiForm tu(SoDiaChi dc) {
        DiaChiForm form = new DiaChiForm();
        form.setMaDiaChi(dc.getMaDiaChi());
        form.setTenNguoiNhan(dc.getTenNguoiNhan());
        form.setSoDienThoai(dc.getSoDienThoai());
        form.setKhuVuc(dc.getKhuVuc());
        form.setDiaChiChiTiet(dc.getDiaChiChiTiet());
        form.setLoaiDiaChi(dc.getLoaiDiaChi());
        form.setLaMacDinh(dc.isLaMacDinh());
        return form;
    }
}
