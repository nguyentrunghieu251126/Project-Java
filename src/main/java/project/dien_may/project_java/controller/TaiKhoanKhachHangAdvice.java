package project.dien_may.project_java.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import project.dien_may.project_java.service.muahang.DiemThuongService;
import java.security.Principal;

@ControllerAdvice(assignableTypes = { HoSoTaiKhoanController.class, DonHangController.class,
        DiaChiController.class, DiemThuongController.class, BaoHanhTaiKhoanController.class })
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
