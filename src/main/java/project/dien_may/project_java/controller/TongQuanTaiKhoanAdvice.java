package project.dien_may.project_java.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import project.dien_may.project_java.service.muahang.TongQuanTaiKhoanService;
import java.security.Principal;

@ControllerAdvice(assignableTypes = HoSoTaiKhoanController.class)
public class TongQuanTaiKhoanAdvice {

    private final TongQuanTaiKhoanService tongQuanService;

    public TongQuanTaiKhoanAdvice(TongQuanTaiKhoanService tongQuanService) {
        this.tongQuanService = tongQuanService;
    }

    @ModelAttribute
    public void napTongQuan(HttpServletRequest request, Principal principal, Model model) {
        String duongDan = request.getRequestURI().substring(request.getContextPath().length());
        if (principal == null || !"GET".equals(request.getMethod())
                || !(duongDan.equals("/tai-khoan") || duongDan.equals("/tai-khoan/"))) {
            return;
        }
        String email = principal.getName();
        model.addAttribute("donGanDay", tongQuanService.donGanDay(email));
        model.addAttribute("uuDaiRieng", tongQuanService.uuDai(TongQuanTaiKhoanService.SO_UU_DAI));
        model.addAttribute("baoHanhGanDay", tongQuanService.baoHanh(email));
    }
}
