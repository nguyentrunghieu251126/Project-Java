package project.dien_may.project_java.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.dto.QuenMatKhauForm;
import project.dien_may.project_java.service.TaiKhoanService;
import project.dien_may.project_java.service.TaiKhoanService.KetQuaDatLaiMatKhau;

@Controller
@RequestMapping("/quen-mat-khau")
public class DoiMatKhauController {

    private static final Logger logger = LoggerFactory.getLogger(DoiMatKhauController.class);

    private static final String VIEW_QUEN_MAT_KHAU = "auth/QuenMatKhau";

    private final TaiKhoanService taiKhoanService;

    @Value("${app.password-reset.allow-lan-demo:false}")
    private boolean choPhepDemoLan;

    private boolean laLocalhost(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return "127.0.0.1".equals(ip) || "::1".equals(ip)
                || "0:0:0:0:0:0:0:1".equals(ip);
    }

    public DoiMatKhauController(TaiKhoanService taiKhoanService) {
        this.taiKhoanService = taiKhoanService;
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        StringTrimmerEditor trim = new StringTrimmerEditor(true);
        dataBinder.registerCustomEditor(String.class, "email", trim);
        dataBinder.registerCustomEditor(String.class, "soDienThoai", trim);
    }

    @GetMapping
    public String hienThiQuenMatKhau(Model model) {
        model.addAttribute("quenMatKhauForm", new QuenMatKhauForm());
        return VIEW_QUEN_MAT_KHAU;
    }

    @PostMapping
    public String xuLyQuenMatKhau(
            @Valid @ModelAttribute("quenMatKhauForm") QuenMatKhauForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        if (!choPhepDemoLan && !laLocalhost(request)) {
            model.addAttribute("thongBaoLoi",
                    "Chức năng đặt lại mật khẩu thử nghiệm hiện chỉ khả dụng tại máy chủ.");
            return VIEW_QUEN_MAT_KHAU;
        }

        if (form.getMatKhauMoi() != null && form.getXacNhanMatKhau() != null
                && !form.getMatKhauMoi().equals(form.getXacNhanMatKhau())) {
            bindingResult.rejectValue("xacNhanMatKhau", "khongKhop", "Xác nhận mật khẩu mới không khớp.");
        }

        if (bindingResult.hasErrors()) {
            return VIEW_QUEN_MAT_KHAU;
        }

        try {
            KetQuaDatLaiMatKhau ketQua = taiKhoanService.datLaiMatKhau(
                    form.getEmail(), form.getSoDienThoai(), form.getMatKhauMoi());

            switch (ketQua) {
                case SAI_THONG_TIN:
                    model.addAttribute("thongBaoLoi", "Thông tin tài khoản không khớp.");
                    return VIEW_QUEN_MAT_KHAU;

                case TRUNG_MAT_KHAU_CU:
                    bindingResult.rejectValue("matKhauMoi", "trungMatKhauCu",
                            "Mật khẩu mới phải khác mật khẩu hiện tại.");
                    return VIEW_QUEN_MAT_KHAU;

                default:
                    redirectAttributes.addFlashAttribute("thongBaoDoiMatKhau",
                            "Đặt lại mật khẩu thành công! Vui lòng đăng nhập bằng mật khẩu mới.");
                    return "redirect:/dang-nhap";
            }

        } catch (DataIntegrityViolationException e) {
            logger.warn("Loi rang buoc khi dat lai mat khau", e);
            model.addAttribute("thongBaoLoi",
                    "Không thể cập nhật thông tin tài khoản. Vui lòng kiểm tra lại.");
            return VIEW_QUEN_MAT_KHAU;

        } catch (DataAccessException e) {
            logger.error("Loi SQL Server khi dat lai mat khau", e);
            model.addAttribute("thongBaoLoi",
                    "Hệ thống chưa thể cập nhật mật khẩu. Vui lòng thử lại sau.");
            return VIEW_QUEN_MAT_KHAU;
        }
    }
}
