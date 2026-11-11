package project.dien_may.project_java.controller;

import jakarta.validation.Valid;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.dto.muahang.HoTroForm;
import project.dien_may.project_java.service.HoSoTaiKhoanService;
import project.dien_may.project_java.service.admin.NghiepVuException;
import project.dien_may.project_java.service.muahang.HoTroService;

import java.security.Principal;

/**
 * TV3 – KH23: liên hệ hỗ trợ bằng form web, lưu vào bảng YeuCauHoTro.
 * Nằm dưới /tai-khoan/** nên SecurityConfig chỉ cho KHACH_HANG.
 */
@Controller
@RequestMapping("/tai-khoan/ho-tro")
public class HoTroController {

    private static final String VIEW = "tai-khoan/HoTro";

    private final HoTroService hoTroService;
    private final HoSoTaiKhoanService hoSoTaiKhoanService;

    public HoTroController(HoTroService hoTroService, HoSoTaiKhoanService hoSoTaiKhoanService) {
        this.hoTroService = hoTroService;
        this.hoSoTaiKhoanService = hoSoTaiKhoanService;
    }

    @InitBinder("hoTroForm")
    public void catKhoangTrang(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    /** maDon: mở từ trang chi tiết đơn thì chọn sẵn đơn đó. */
    @GetMapping
    public String trangHoTro(@RequestParam(required = false) Integer maDon, Principal principal, Model model) {
        HoTroForm form = new HoTroForm();
        if (hoTroService.laDonCuaKhach(principal.getName(), maDon)) {
            form.setMaDon(maDon);
        }
        return hienTrang(model, principal, form);
    }

    @PostMapping
    public String gui(@Valid @ModelAttribute("hoTroForm") HoTroForm form, BindingResult ketQua,
                      Principal principal, Model model, RedirectAttributes redirect) {
        if (ketQua.hasErrors()) {
            return hienTrang(model, principal, form);
        }
        try {
            hoTroService.gui(principal.getName(), form);
        } catch (NghiepVuException e) {
            model.addAttribute("loi", e.getMessage());
            return hienTrang(model, principal, form);
        }
        redirect.addFlashAttribute("thongBao", "Đã gửi yêu cầu hỗ trợ. Cửa hàng sẽ phản hồi trong vòng 24 giờ.");
        return "redirect:/tai-khoan/ho-tro";
    }

    private String hienTrang(Model model, Principal principal, HoTroForm form) {
        model.addAttribute("menuTaiKhoan", "ho-tro");
        model.addAttribute("hoSo", hoSoTaiKhoanService.layHoSo(principal.getName()));
        model.addAttribute("hoTroForm", form);
        model.addAttribute("dsChuDe", HoTroForm.DS_CHU_DE);
        model.addAttribute("dsDon", hoTroService.donGanDay(principal.getName()));
        model.addAttribute("dsYeuCau", hoTroService.danhSach(principal.getName()));
        return VIEW;
    }
}
