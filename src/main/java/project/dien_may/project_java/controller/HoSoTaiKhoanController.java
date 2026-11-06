package project.dien_may.project_java.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.dto.CapNhatTaiKhoanForm;
import project.dien_may.project_java.dto.DoiMatKhauTaiKhoanForm;
import project.dien_may.project_java.service.HoSoTaiKhoanService;
import project.dien_may.project_java.service.HoSoTaiKhoanService.KetQuaDoiMatKhau;
import project.dien_may.project_java.service.HoSoTaiKhoanService.ThongTinHoSo;

import java.security.Principal;

@Controller
@RequestMapping("/tai-khoan")
public class HoSoTaiKhoanController {

    private static final Logger log = LoggerFactory.getLogger(HoSoTaiKhoanController.class);
    private static final String VIEW = "tai-khoan/TaiKhoan";
    private final HoSoTaiKhoanService hoSoTaiKhoanService;

    public HoSoTaiKhoanController(HoSoTaiKhoanService hoSoTaiKhoanService) {
        this.hoSoTaiKhoanService = hoSoTaiKhoanService;
    }

    @InitBinder("capNhatTaiKhoanForm")
    public void trimThongTin(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping({"", "/"})
    public String tongQuan(Principal principal, Model model) {
        if (!napHoSo(model, principal)) return VIEW;
        model.addAttribute("trangTaiKhoan", "tong-quan");
        return VIEW;
    }

    @GetMapping("/ho-so")
    public String hoSo(Principal principal, Model model) {
        if (!napHoSo(model, principal)) return VIEW;
        model.addAttribute("trangTaiKhoan", "ho-so");
        ThongTinHoSo hoSo = (ThongTinHoSo) model.getAttribute("hoSo");
        CapNhatTaiKhoanForm form = new CapNhatTaiKhoanForm();
        form.setHoTen(hoSo == null ? "" : hoSo.hoTen());
        form.setSoDienThoai(hoSo == null ? null : hoSo.soDienThoai());
        form.setDiaChi(hoSo == null ? null : hoSo.diaChi());
        model.addAttribute("capNhatTaiKhoanForm", form);
        return VIEW;
    }

    @PostMapping("/ho-so")
    public String luuHoSo(@Valid @ModelAttribute("capNhatTaiKhoanForm") CapNhatTaiKhoanForm form,
                          BindingResult bindingResult,
                          Principal principal, Model model, RedirectAttributes redirect) {
        model.addAttribute("trangTaiKhoan", "ho-so");
        if (!napHoSo(model, principal)) return VIEW;
        if (bindingResult.hasErrors() || model.getAttribute("hoSo") == null) return VIEW;
        try {
            hoSoTaiKhoanService.capNhatHoSo(principal.getName(), form);
            redirect.addFlashAttribute("thongBaoThanhCong", "Cập nhật hồ sơ và địa chỉ mặc định thành công.");
            return "redirect:/tai-khoan/ho-so";
        } catch (DataAccessException e) {
            log.error("Khong the cap nhat ho so", e);
            model.addAttribute("thongBaoLoi", "Không thể lưu thông tin lúc này. Vui lòng thử lại sau.");
        } catch (IllegalStateException e) {
            log.warn("Ho so khong hop le: {}", e.getMessage());
            model.addAttribute("thongBaoLoi", "Không thể cập nhật tài khoản. Vui lòng đăng nhập lại.");
        }
        return VIEW;
    }

    @GetMapping("/doi-mat-khau")
    public String doiMatKhau(Principal principal, Model model) {
        if (!napHoSo(model, principal)) return VIEW;
        model.addAttribute("trangTaiKhoan", "doi-mat-khau");
        model.addAttribute("doiMatKhauTaiKhoanForm", new DoiMatKhauTaiKhoanForm());
        return VIEW;
    }

    @PostMapping("/doi-mat-khau")
    public String luuMatKhau(@Valid @ModelAttribute("doiMatKhauTaiKhoanForm") DoiMatKhauTaiKhoanForm form,
                            BindingResult bindingResult, Principal principal,
                            Model model, RedirectAttributes redirect) {
        model.addAttribute("trangTaiKhoan", "doi-mat-khau");
        if (!napHoSo(model, principal)) return VIEW;
        if (form.getMatKhauMoi() != null && form.getXacNhanMatKhau() != null
                && !form.getMatKhauMoi().equals(form.getXacNhanMatKhau())) {
            bindingResult.rejectValue("xacNhanMatKhau", "khongKhop", "Xác nhận mật khẩu mới không khớp.");
        }
        if (bindingResult.hasErrors() || model.getAttribute("hoSo") == null) return VIEW;
        try {
            KetQuaDoiMatKhau ketQua = hoSoTaiKhoanService.doiMatKhau(
                    principal.getName(), form.getMatKhauHienTai(), form.getMatKhauMoi());
            if (ketQua == KetQuaDoiMatKhau.MAT_KHAU_HIEN_TAI_SAI) {
                bindingResult.rejectValue("matKhauHienTai", "matKhauSai", "Mật khẩu hiện tại không chính xác.");
                return VIEW;
            }
            if (ketQua == KetQuaDoiMatKhau.TRUNG_MAT_KHAU_CU) {
                bindingResult.rejectValue("matKhauMoi", "trungMatKhau", "Mật khẩu mới phải khác mật khẩu hiện tại.");
                return VIEW;
            }
            redirect.addFlashAttribute("thongBaoThanhCong", "Đổi mật khẩu thành công.");
            return "redirect:/tai-khoan/doi-mat-khau";
        } catch (DataAccessException e) {
            log.error("Khong the doi mat khau", e);
            model.addAttribute("thongBaoLoi", "Không thể đổi mật khẩu lúc này. Vui lòng thử lại sau.");
        } catch (IllegalStateException e) {
            log.warn("Khong the truy cap tai khoan de doi mat khau: {}", e.getMessage());
            model.addAttribute("thongBaoLoi", "Phiên tài khoản không hợp lệ. Vui lòng đăng nhập lại.");
        }
        return VIEW;
    }

    private boolean napHoSo(Model model, Principal principal) {
        if (principal == null) return false;
        try {
            model.addAttribute("hoSo", hoSoTaiKhoanService.layHoSo(principal.getName()));
        } catch (IllegalStateException e) {
            log.warn("Khong doc duoc thong tin tai khoan: {}", e.getMessage());
            model.addAttribute("thongBaoLoi", "Không tải được thông tin tài khoản. Vui lòng đăng nhập lại.");
        } catch (DataAccessException e) {
            log.error("Khong doc duoc tai khoan do loi CSDL", e);
            model.addAttribute("thongBaoLoi", "Tạm thời không tải được thông tin tài khoản. Vui lòng thử lại sau.");
        }
        return model.getAttribute("hoSo") != null;
    }
}
