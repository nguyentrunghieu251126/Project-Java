package project.dien_may.project_java.controller.admin;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.dto.TaiKhoanForm;
import project.dien_may.project_java.repository.TaiKhoanRepository;
import project.dien_may.project_java.repository.VaiTroRepository;
import project.dien_may.project_java.service.admin.NghiepVuException;
import project.dien_may.project_java.service.admin.TaiKhoanAdminService;

import java.security.Principal;

/** Quản lý tài khoản người dùng (QT12 – QT15). */
@Controller
@RequestMapping("/admin/tai-khoan")
public class AdminTaiKhoanController {

    private static final String VIEW_FORM = "admin/tai-khoan/form";
    private static final String VE_DANH_SACH = "redirect:/admin/tai-khoan";

    private final TaiKhoanAdminService taiKhoanService;
    private final TaiKhoanRepository taiKhoanRepository;
    private final VaiTroRepository vaiTroRepository;

    public AdminTaiKhoanController(TaiKhoanAdminService taiKhoanService, TaiKhoanRepository taiKhoanRepository,
                                   VaiTroRepository vaiTroRepository) {
        this.taiKhoanService = taiKhoanService;
        this.taiKhoanRepository = taiKhoanRepository;
        this.vaiTroRepository = vaiTroRepository;
    }

    @GetMapping
    public String danhSach(@RequestParam(required = false) String tuKhoa,
                           @RequestParam(required = false) Integer maVaiTro,
                           @RequestParam(required = false) Boolean trangThai,
                           @RequestParam(defaultValue = "1") int trang,
                           Principal nguoiDung, Model model) {
        model.addAttribute("trang", taiKhoanRepository.timKiem(tuKhoa, maVaiTro, trangThai,
                PhanTrang.tao(trang, Sort.by("maTaiKhoan"))));
        model.addAttribute("tuKhoa", tuKhoa);
        model.addAttribute("maVaiTro", maVaiTro);
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("dsVaiTro", vaiTroRepository.findAll(Sort.by("maVaiTro")));
        model.addAttribute("emailHienTai", nguoiDung.getName());
        return "admin/tai-khoan/danh-sach";
    }

    @GetMapping("/them")
    public String formThem(Principal nguoiDung, Model model) {
        return hienForm(new TaiKhoanForm(), nguoiDung, model);
    }

    @PostMapping("/them")
    public String them(@Valid @ModelAttribute("form") TaiKhoanForm form, BindingResult ketQua, Principal nguoiDung,
                       Model model, RedirectAttributes redirect) {
        form.setMaTaiKhoan(null);
        taiKhoanService.kiemTra(form, ketQua);
        if (ketQua.hasErrors()) {
            return hienForm(form, nguoiDung, model);
        }
        try {
            taiKhoanService.them(form);
        } catch (NghiepVuException | DataIntegrityViolationException e) {
            return baoLoiForm(e, form, nguoiDung, model);
        }
        redirect.addFlashAttribute("thongBao", "Đã thêm tài khoản " + form.getEmail() + ".");
        return VE_DANH_SACH;
    }

    @GetMapping("/{ma}/sua")
    public String formSua(@PathVariable Integer ma, Principal nguoiDung, Model model) {
        return hienForm(taiKhoanService.layForm(ma), nguoiDung, model);
    }

    @PostMapping("/{ma}/sua")
    public String sua(@PathVariable Integer ma, @Valid @ModelAttribute("form") TaiKhoanForm form, BindingResult ketQua,
                      Principal nguoiDung, Model model, RedirectAttributes redirect) {
        form.setMaTaiKhoan(ma);
        taiKhoanService.kiemTra(form, ketQua);
        if (ketQua.hasErrors()) {
            return hienForm(form, nguoiDung, model);
        }
        try {
            taiKhoanService.capNhat(ma, form, nguoiDung.getName());
        } catch (NghiepVuException | DataIntegrityViolationException e) {
            return baoLoiForm(e, form, nguoiDung, model);
        }
        redirect.addFlashAttribute("thongBao", "Đã cập nhật tài khoản " + form.getEmail() + ".");
        return VE_DANH_SACH;
    }

    @PostMapping("/{ma}/khoa")
    public String khoa(@PathVariable Integer ma, Principal nguoiDung, RedirectAttributes redirect) {
        try {
            boolean hoatDong = taiKhoanService.khoaHoacMoKhoa(ma, nguoiDung.getName());
            redirect.addFlashAttribute("thongBao", hoatDong ? "Đã mở khóa tài khoản." : "Đã khóa tài khoản.");
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        }
        return VE_DANH_SACH;
    }

    @PostMapping("/{ma}/xoa")
    public String xoa(@PathVariable Integer ma, Principal nguoiDung, RedirectAttributes redirect) {
        try {
            taiKhoanService.xoa(ma, nguoiDung.getName());
            redirect.addFlashAttribute("thongBao", "Đã xóa tài khoản.");
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("loi", "Không thể xóa tài khoản vì đang có dữ liệu liên quan. Hãy khóa tài khoản này.");
        }
        return VE_DANH_SACH;
    }

    private String hienForm(TaiKhoanForm form, Principal nguoiDung, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("dsVaiTro", vaiTroRepository.findAll(Sort.by("maVaiTro")));
        model.addAttribute("laChinhMinh", form.getMaTaiKhoan() != null && taiKhoanRepository
                .findByEmailIgnoreCase(nguoiDung.getName())
                .filter(tk -> tk.getMaTaiKhoan().equals(form.getMaTaiKhoan())).isPresent());
        return VIEW_FORM;
    }

    private String baoLoiForm(RuntimeException e, TaiKhoanForm form, Principal nguoiDung, Model model) {
        model.addAttribute("loi", e instanceof NghiepVuException ? e.getMessage()
                : "Dữ liệu bị trùng hoặc không hợp lệ, vui lòng kiểm tra lại.");
        return hienForm(form, nguoiDung, model);
    }
}
