package project.dien_may.project_java.dto.muahang;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** TV3 – KH23: form liên hệ hỗ trợ. Tiêu đề lưu vào CSDL = chủ đề (+ mã đơn nếu khách chọn). */
@Getter
@Setter
public class HoTroForm {

    public static final List<String> DS_CHU_DE = List.of(
            "Đơn hàng & giao hàng",
            "Thanh toán & hoàn tiền",
            "Mã giảm giá",
            "Sản phẩm & bảo hành",
            "Đổi trả sản phẩm",
            "Góp ý khác");

    @NotBlank(message = "Vui lòng chọn chủ đề.")
    private String chuDe = DS_CHU_DE.get(0);

    /** Đơn hàng liên quan (không bắt buộc). */
    private Integer maDon;

    @NotBlank(message = "Vui lòng nhập nội dung cần hỗ trợ.")
    @Size(min = 10, max = 2000, message = "Nội dung cần từ 10 đến 2000 ký tự.")
    private String noiDung;
}
