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
import project.dien_may.project_java.dto.KhuyenMaiForm;
import project.dien_may.project_java.entity.LoaiGiamGia;
import project.dien_may.project_java.entity.TinhTrangKhuyenMai;
import project.dien_may.project_java.repository.KhuyenMaiRepository;
import project.dien_may.project_java.service.admin.KhuyenMaiAdminService;
import project.dien_may.project_java.service.admin.NghiepVuException;

import java.time.LocalDate;

/** Quản lý khuyến mãi (QT07). */
@Controller
@RequestMapping("/admin/khuyen-mai")
public class AdminKhuyenMaiController {

    private static final String VIEW_FORM = "admin/khuyen-mai/form";
    private static final String VE_DANH_SACH = "redirect:/admin/khuyen-mai";
    private static final String LOI_DU_LIEU = "Dữ liệu bị trùng hoặc không hợp lệ, vui lòng kiểm tra lại.";

    private final KhuyenMaiAdminService khuyenMaiService;
    private final KhuyenMaiRepository khuyenMaiRepository;

    public AdminKhuyenMaiController(KhuyenMaiAdminService khuyenMaiService, KhuyenMaiRepository khuyenMaiRepository) {
        this.khuyenMaiService = khuyenMaiService;
        this.khuyenMaiRepository = khuyenMaiRepository;
    }

    @GetMapping
    public String danhSach(@RequestParam(required = false) String tuKhoa,
                           @RequestParam(required = false) TinhTrangKhuyenMai tinhTrang,
                           @RequestParam(defaultValue = "1") int trang,
                           Model model) {
        model.addAttribute("trang", khuyenMaiRepository.timKiem(tuKhoa, tinhTrang == null ? null : tinhTrang.name(),
                LocalDate.now(), PhanTrang.tao(trang, Sort.by(Sort.Direction.DESC, "ngayBatDau", "maKhuyenMai"))));
        model.addAttribute("tuKhoa", tuKhoa);
        model.addAttribute("tinhTrang", tinhTrang);
        model.addAttribute("dsTinhTrang", TinhTrangKhuyenMai.values());
        return "admin/khuyen-mai/danh-sach";
    }

    @GetMapping("/them")
    public String formThem(Model model) {
        return hienForm(new KhuyenMaiForm(), model);
    }

    @PostMapping("/them")
    public String them(@Valid @ModelAttribute("form") KhuyenMaiForm form, BindingResult ketQua, Model model,
                       RedirectAttributes redirect) {
        form.setMaKhuyenMai(null);
        khuyenMaiService.kiemTra(form, ketQua);
        if (ketQua.hasErrors()) {
            return hienForm(form, model);
        }
        try {
            khuyenMaiService.them(form);
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("loi", LOI_DU_LIEU);
            return hienForm(form, model);
        }
        redirect.addFlashAttribute("thongBao", "Đã thêm khuyến mãi " + form.getTenKhuyenMai() + ".");
        return VE_DANH_SACH;
    }

    @GetMapping("/{ma}/sua")
    public String formSua(@PathVariable Integer ma, Model model) {
        return hienForm(khuyenMaiService.layForm(ma), model);
    }

    @PostMapping("/{ma}/sua")
    public String sua(@PathVariable Integer ma, @Valid @ModelAttribute("form") KhuyenMaiForm form, BindingResult ketQua,
                      Model model, RedirectAttributes redirect) {
        form.setMaKhuyenMai(ma);
        khuyenMaiService.kiemTra(form, ketQua);
        if (ketQua.hasErrors()) {
            return hienForm(form, model);
        }
        try {
            khuyenMaiService.capNhat(ma, form);
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("loi", LOI_DU_LIEU);
            return hienForm(form, model);
        }
        redirect.addFlashAttribute("thongBao", "Đã cập nhật khuyến mãi " + form.getTenKhuyenMai() + ".");
        return VE_DANH_SACH;
    }

    @PostMapping("/{ma}/bat-tat")
    public String batTat(@PathVariable Integer ma, RedirectAttributes redirect) {
        boolean dangBat = khuyenMaiService.batTat(ma);
        redirect.addFlashAttribute("thongBao", dangBat ? "Đã kích hoạt lại khuyến mãi." : "Đã tạm dừng khuyến mãi.");
        return VE_DANH_SACH;
    }

    @PostMapping("/{ma}/xoa")
    public String xoa(@PathVariable Integer ma, RedirectAttributes redirect) {
        try {
            khuyenMaiService.xoa(ma);
            redirect.addFlashAttribute("thongBao", "Đã xóa khuyến mãi.");
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("loi", "Không thể xóa khuyến mãi vì đang có đơn hàng sử dụng.");
        }
        return VE_DANH_SACH;
    }

    private String hienForm(KhuyenMaiForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("dsLoai", LoaiGiamGia.values());
        return VIEW_FORM;
    }
}
