package project.dien_may.project_java.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import project.dien_may.project_java.entity.KhuyenMai;
import project.dien_may.project_java.entity.LoaiGiamGia;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class KhuyenMaiForm {

    private Integer maKhuyenMai;

    @NotBlank(message = "Vui lòng nhập tên khuyến mãi.")
    @Size(max = 200, message = "Tên khuyến mãi không được vượt quá 200 ký tự.")
    private String tenKhuyenMai;

    @NotBlank(message = "Vui lòng nhập mã giảm giá.")
    @Size(max = 30, message = "Mã giảm giá không được vượt quá 30 ký tự.")
    @Pattern(regexp = "^$|^[A-Za-z0-9_-]+$", message = "Mã giảm giá chỉ gồm chữ không dấu, số, dấu - và _.")
    private String maGiamGia;

    @NotNull(message = "Vui lòng chọn loại giảm giá.")
    private LoaiGiamGia loaiGiamGia;

    @NotNull(message = "Vui lòng nhập giá trị giảm.")
    @DecimalMin(value = "1", message = "Giá trị giảm phải lớn hơn 0.")
    @Digits(integer = 15, fraction = 0, message = "Giá trị giảm phải là số nguyên, tối đa 15 chữ số.")
    private BigDecimal giaTriGiam;

    @NotNull(message = "Vui lòng chọn ngày bắt đầu.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate ngayBatDau;

    @NotNull(message = "Vui lòng chọn ngày kết thúc.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate ngayKetThuc;

    /** Để trống: không giới hạn. */
    @Min(value = 1, message = "Số lượt tối đa phải lớn hơn 0.")
    private Integer soLuotToiDa;

    public static KhuyenMaiForm tu(KhuyenMai km) {
        KhuyenMaiForm f = new KhuyenMaiForm();
        f.maKhuyenMai = km.getMaKhuyenMai();
        f.tenKhuyenMai = km.getTenKhuyenMai();
        f.maGiamGia = km.getMaGiamGia();
        f.loaiGiamGia = km.getLoaiGiamGia();
        f.giaTriGiam = km.getGiaTriGiam();
        f.ngayBatDau = km.getNgayBatDau();
        f.ngayKetThuc = km.getNgayKetThuc();
        f.soLuotToiDa = km.getSoLuotToiDa();
        return f;
    }
}
