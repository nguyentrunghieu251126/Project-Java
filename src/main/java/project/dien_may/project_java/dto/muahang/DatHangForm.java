package project.dien_may.project_java.dto.muahang;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import project.dien_may.project_java.entity.PhuongThucThanhToan;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DatHangForm {
    @NotNull(message = "Vui lòng chọn hoặc thêm địa chỉ nhận hàng.")
    private Integer maDiaChi;

    private String hoTenNguoiNhan;
    private String soDienThoaiNguoiNhan;
    private String diaChiGiaoHang;

    @NotNull(message = "Vui lòng chọn phương thức thanh toán.")
    private PhuongThucThanhToan phuongThucThanhToan = PhuongThucThanhToan.COD;

    @Size(max = 30, message = "Mã giảm giá không được vượt quá 30 ký tự.")
    private String maGiamGia;

    private List<Integer> sanPham = new ArrayList<>();

    @jakarta.validation.constraints.Min(value = 0, message = "Số điểm không hợp lệ.")
    private Integer diemSuDung;
}
