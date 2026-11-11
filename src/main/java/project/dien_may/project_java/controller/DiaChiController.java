package project.dien_may.project_java.controller;

import jakarta.validation.Valid;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.dto.muahang.DiaChiForm;
import project.dien_may.project_java.dto.muahang.TinhThanh;
import project.dien_may.project_java.entity.SoDiaChi;
import project.dien_may.project_java.service.HoSoTaiKhoanService;
import project.dien_may.project_java.service.admin.NghiepVuException;
import project.dien_may.project_java.service.muahang.SoDiaChiService;

import java.security.Principal;

/**
 * TV3 – sổ địa chỉ: thêm, sửa, xóa, đặt mặc định (bảng SoDiaChi), theo ý tưởng Project-Website.
 * quayLai: trang quay về sau khi lưu (trang Thanh toán hoặc trang Hồ sơ); chỉ nhận đường dẫn trong website.
 * Nằm dưới /tai-khoan/** nên SecurityConfig chỉ cho KHACH_HANG.
 */
@Controller
@RequestMapping("/tai-khoan/dia-chi")
public class DiaChiController {

    private static final String VIEW = "tai-khoan/DiaChi";
    private static final String MAC_DINH_QUAY_LAI = "/tai-khoan/ho-so";

    private final SoDiaChiService soDiaChiService;
    private final HoSoTaiKhoanService hoSoTaiKhoanService;

    public DiaChiController(SoDiaChiService soDiaChiService, HoSoTaiKhoanService hoSoTaiKhoanService) {
        this.soDiaChiService = soDiaChiService;
        this.hoSoTaiKhoanService = hoSoTaiKhoanService;
    }

    @InitBinder("diaChiForm")
    public void catKhoangTrang(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping("/them")
    public String formThem(@RequestParam(required = false) String quayLai, Principal principal, Model model) {
        return hienForm(model, principal, soDiaChiService.formMoi(principal.getName()), quayLai);
    }

    @GetMapping("/{maDiaChi}/sua")
    public String formSua(@PathVariable Integer maDiaChi, @RequestParam(required = false) String quayLai,
                          Principal principal, Model model, RedirectAttributes redirect) {
        try {
            SoDiaChi dc = soDiaChiService.timCuaKhach(principal.getName(), maDiaChi);
            return hienForm(model, principal, DiaChiForm.tu(dc), quayLai);
        } catch (NghiepVuException e) {
            baoLoi(redirect, quayLai, e.getMessage());
            return "redirect:" + duongDanVe(quayLai);
        }
    }

    @PostMapping("/luu")
    public String luu(@Valid @ModelAttribute("diaChiForm") DiaChiForm form, BindingResult ketQua,
                      @RequestParam(required = false) String quayLai,
                      Principal principal, Model model, RedirectAttributes redirect) {
        if (ketQua.hasErrors()) {
            return hienForm(model, principal, form, quayLai);
        }
        Integer ma;
        try {
            ma = soDiaChiService.luu(principal.getName(), form);
        } catch (NghiepVuException e) {
            model.addAttribute("loi", e.getMessage());
            return hienForm(model, principal, form, quayLai);
        }
        String ve = duongDanVe(quayLai);
        String thongBao = form.getMaDiaChi() == null ? "Đã thêm địa chỉ mới." : "Đã cập nhật địa chỉ.";
        if (ve.startsWith("/dat-hang")) {
            // Quay lại trang Thanh toán: chọn sẵn địa chỉ vừa lưu
            ve = ve.replaceAll("([?&])diaChi=\\d*&?", "$1").replaceAll("[?&]$", "");
            ve += (ve.contains("?") ? "&" : "?") + "diaChi=" + ma;
            redirect.addFlashAttribute("thongBao", thongBao + " Địa chỉ đã được chọn để giao hàng.");
        } else {
            redirect.addFlashAttribute("thongBaoThanhCong", thongBao);
        }
        return "redirect:" + ve;
    }

    @PostMapping("/{maDiaChi}/mac-dinh")
    public String datMacDinh(@PathVariable Integer maDiaChi, @RequestParam(required = false) String quayLai,
                             Principal principal, RedirectAttributes redirect) {
        try {
            soDiaChiService.datMacDinh(principal.getName(), maDiaChi);
            baoThanhCong(redirect, quayLai, "Đã đặt làm địa chỉ mặc định.");
        } catch (NghiepVuException e) {
            baoLoi(redirect, quayLai, e.getMessage());
        }
        return "redirect:" + duongDanVe(quayLai);
    }

    @PostMapping("/{maDiaChi}/xoa")
    public String xoa(@PathVariable Integer maDiaChi, @RequestParam(required = false) String quayLai,
                      Principal principal, RedirectAttributes redirect) {
        try {
            soDiaChiService.xoa(principal.getName(), maDiaChi);
            baoThanhCong(redirect, quayLai, "Đã xóa địa chỉ.");
        } catch (NghiepVuException e) {
            baoLoi(redirect, quayLai, e.getMessage());
        }
        String ve = duongDanVe(quayLai);
        // Vừa xóa địa chỉ đang chọn ở trang Thanh toán: bỏ tham số diaChi để chọn lại mặc định
        return "redirect:" + ve.replaceAll("([?&])diaChi=\\d*&?", "$1").replaceAll("[?&]$", "");
    }

    private String hienForm(Model model, Principal principal, DiaChiForm form, String quayLai) {
        model.addAttribute("menuTaiKhoan", "ho-so");
        model.addAttribute("hoSo", hoSoTaiKhoanService.layHoSo(principal.getName()));
        model.addAttribute("diaChiForm", form);
        model.addAttribute("dsThanhPho", TinhThanh.THANH_PHO);
        model.addAttribute("dsTinh", TinhThanh.TINH);
        model.addAttribute("quayLai", duongDanVe(quayLai));
        return VIEW;
    }

    /** Trang Thanh toán dùng thongBao / loi; trang Hồ sơ (TV2) dùng thongBaoThanhCong / thongBaoLoi. */
    private static void baoThanhCong(RedirectAttributes redirect, String quayLai, String noiDung) {
        redirect.addFlashAttribute(duongDanVe(quayLai).startsWith("/dat-hang") ? "thongBao" : "thongBaoThanhCong", noiDung);
    }

    private static void baoLoi(RedirectAttributes redirect, String quayLai, String noiDung) {
        redirect.addFlashAttribute(duongDanVe(quayLai).startsWith("/dat-hang") ? "loi" : "thongBaoLoi", noiDung);
    }

    /** Chỉ quay về trang Thanh toán hoặc trang tài khoản trong website (chống chuyển hướng ra ngoài). */
    static String duongDanVe(String quayLai) {
        if (quayLai != null && !quayLai.startsWith("//")
                && (quayLai.startsWith("/dat-hang") || quayLai.startsWith("/tai-khoan"))) {
            return quayLai;
        }
        return MAC_DINH_QUAY_LAI;
    }
}
