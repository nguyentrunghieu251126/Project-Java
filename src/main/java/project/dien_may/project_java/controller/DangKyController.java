
package project.dien_may.project_java.controller;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.dto.DangKyForm;
import project.dien_may.project_java.service.TaiKhoanService;

@Controller
@RequestMapping("/dang-ky")
public class DangKyController {

    private static final Logger logger = LoggerFactory.getLogger(DangKyController.class);

    private final TaiKhoanService taiKhoanService;

    public DangKyController(
            TaiKhoanService taiKhoanService) {

        this.taiKhoanService = taiKhoanService;
    }

    @GetMapping
    public String hienThiTrangDangKy(Model model) {

        model.addAttribute("dangKyForm", new DangKyForm());
        return "auth/DangKy";
    }

    @PostMapping
    public String xuLyDangKy(@Valid @ModelAttribute("dangKyForm") DangKyForm form, BindingResult bindingResult,
            Model model, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "auth/DangKy";
        }

        if (!form.getMatKhau().equals(form.getXacNhanMatKhau())) {

            bindingResult.rejectValue("xacNhanMatKhau", "matKhau.khongKhop", "Xác nhận mật khẩu không khớp.");
            return "auth/DangKy";
        }
        try {
            if (taiKhoanService.emailDaTonTai(form.getEmail())) {
                bindingResult.rejectValue("email", "email.daTonTai", "Email này đã được đăng ký.");
                return "auth/DangKy";
            }
            taiKhoanService.dangKy(form);
            redirectAttributes.addFlashAttribute("thongBaoDangKy", "Đăng ký tài khoản thành công. Vui lòng đăng nhập.");
            return "redirect:/dang-nhap";

        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("email", "email.daTonTai", "Email này đã được đăng ký.");
            return "auth/DangKy";
        } catch (DataIntegrityViolationException e) {
            logger.warn("Dang ky that bai do rang buoc du lieu", e);
            model.addAttribute("thongBaoLoi",
                    "Thông tin đăng ký bị trùng hoặc không hợp lệ. " + "Vui lòng kiểm tra lại.");
            return "auth/DangKy";

        } catch (DataAccessException e) {

            logger.error("Loi SQL Server khi dang ky tai khoan", e);

            model.addAttribute("thongBaoLoi", "Không thể lưu tài khoản lúc này. " + "Vui lòng thử lại sau.");
            return "auth/DangKy";

        } catch (IllegalStateException e) {

            logger.error("Loi nghiep vu khi dang ky tai khoan", e);
            model.addAttribute("thongBaoLoi", "Hệ thống chưa thể tạo tài khoản. " + "Vui lòng thử lại sau.");
            return "auth/DangKy";
        }
    }
}
