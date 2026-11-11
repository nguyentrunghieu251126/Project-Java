package project.dien_may.project_java.dto.muahang;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import project.dien_may.project_java.entity.PhuongThucThanhToan;

import java.util.ArrayList;
import java.util.List;

/** TV3 – KH12 – KH15: dữ liệu khách gửi lên khi bấm "Đặt hàng". Người nhận lấy từ sổ địa chỉ. */
@Getter
@Setter
public class DatHangForm {

    /** Địa chỉ chọn trong sổ địa chỉ (bảng SoDiaChi). */
    @NotNull(message = "Vui lòng chọn hoặc thêm địa chỉ nhận hàng.")
    private Integer maDiaChi;

    /** Ba trường dưới do server điền từ địa chỉ đã chọn, khớp cột DonHang. */
    private String hoTenNguoiNhan;
    private String soDienThoaiNguoiNhan;
    private String diaChiGiaoHang;

    @NotNull(message = "Vui lòng chọn phương thức thanh toán.")
    private PhuongThucThanhToan phuongThucThanhToan = PhuongThucThanhToan.COD;

    @Size(max = 30, message = "Mã giảm giá không được vượt quá 30 ký tự.")
    private String maGiamGia;

    /** Mã các sản phẩm được tick ở giỏ hàng. */
    private List<Integer> sanPham = new ArrayList<>();

    /** Số điểm thưởng muốn dùng (0 hoặc null = không dùng); server kiểm tra lại. */
    @jakarta.validation.constraints.Min(value = 0, message = "Số điểm không hợp lệ.")
    private Integer diemSuDung;
}
