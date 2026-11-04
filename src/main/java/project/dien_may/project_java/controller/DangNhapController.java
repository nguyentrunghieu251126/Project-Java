package project.dien_may.project_java.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DangNhapController {

    @GetMapping("/dang-nhap")
    public String hienThiTrangDangNhap(
            @RequestParam(value = "error", required = false) String error,
            Model model) {

        if (error != null) {
            model.addAttribute("thongBaoLoi", "Email hoặc mật khẩu không chính xác");
        }
        return "auth/DangNhap";
    }
}
