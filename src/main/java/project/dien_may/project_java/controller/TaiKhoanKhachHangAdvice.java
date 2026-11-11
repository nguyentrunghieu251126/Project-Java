package project.dien_may.project_java.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import project.dien_may.project_java.service.muahang.DiemThuongService;
import project.dien_may.project_java.service.muahang.TongQuanTaiKhoanService;

import java.security.Principal;

/**
 * TV3 – dữ liệu dùng chung cho các trang tài khoản khách hàng:
 * số điểm thưởng ở thẻ điểm bên trái, và nội dung trang Tổng quan (đơn gần đây, ưu đãi, bảo hành)
 * mà không phải sửa HoSoTaiKhoanController của TV2.
 */
@ControllerAdvice(assignableTypes = {HoSoTaiKhoanController.class, DonHangController.class,
        DiaChiController.class, DiemThuongController.class})
public class TaiKhoanKhachHangAdvice {

    private final DiemThuongService diemThuongService;

    public TaiKhoanKhachHangAdvice(DiemThuongService diemThuongService) {
        this.diemThuongService = diemThuongService;
    }

    @ModelAttribute("diemThuong")
    public Long diemThuong(Principal principal) {
        return principal == null ? 0L : diemThuongService.tongDiem(principal.getName());
    }
}
