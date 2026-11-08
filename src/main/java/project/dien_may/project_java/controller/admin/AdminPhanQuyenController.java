package project.dien_may.project_java.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.entity.TaiKhoan;
import project.dien_may.project_java.service.admin.NghiepVuException;
import project.dien_may.project_java.service.admin.PhanQuyenService;

import java.util.List;
import java.util.Set;

/** Phân quyền theo chức năng (QT16): cho toàn thể nhân viên và cho từng nhân viên. */
@Controller
@RequestMapping("/admin/phan-quyen")
public class AdminPhanQuyenController {

    private final PhanQuyenService phanQuyenService;

    public AdminPhanQuyenController(PhanQuyenService phanQuyenService) {
        this.phanQuyenService = phanQuyenService;
    }

    @GetMapping
    public String toanThe(Model model) {
        model.addAttribute("quyenTheoNhom", phanQuyenService.quyenTheoNhom());
        model.addAttribute("daChon", phanQuyenService.quyenToanTheNhanVien());
        model.addAttribute("chinhRieng", Set.of());
        return "admin/phan-quyen";
    }

    @PostMapping
    public String luuToanThe(@RequestParam(name = "quyen", required = false) List<Integer> dsMaQuyen,
                             RedirectAttributes redirect) {
        try {
            phanQuyenService.capNhatToanTheNhanVien(dsMaQuyen == null ? List.of() : dsMaQuyen);
            redirect.addFlashAttribute("thongBao", "Đã lưu phân quyền. Thay đổi có hiệu lực ngay lập tức.");
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        }
        return "redirect:/admin/phan-quyen";
    }

    @GetMapping("/nhan-vien")
    public String dsNhanVien(Model model) {
        model.addAttribute("dsNhanVien", phanQuyenService.dsNhanVien());
        return "admin/phan-quyen-nhan-vien";
    }

    @GetMapping("/nhan-vien/{ma}")
    public String quyenNhanVien(@PathVariable Integer ma, Model model) {
        TaiKhoan nhanVien = phanQuyenService.nhanVien(ma);
        model.addAttribute("nhanVien", nhanVien);
        model.addAttribute("quyenTheoNhom", phanQuyenService.quyenTheoNhom());
        model.addAttribute("daChon", phanQuyenService.quyenHieuLuc(nhanVien));
        model.addAttribute("chinhRieng", phanQuyenService.quyenChinhRieng(ma));
        return "admin/phan-quyen-chi-tiet";
    }

    @PostMapping("/nhan-vien/{ma}")
    public String luuNhanVien(@PathVariable Integer ma,
                              @RequestParam(name = "quyen", required = false) List<Integer> dsMaQuyen,
                              RedirectAttributes redirect) {
        try {
            phanQuyenService.capNhatNhanVien(ma, dsMaQuyen == null ? List.of() : dsMaQuyen);
            redirect.addFlashAttribute("thongBao", "Đã lưu quyền của nhân viên. Thay đổi có hiệu lực ngay lập tức.");
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        }
        return "redirect:/admin/phan-quyen/nhan-vien/" + ma;
    }
}
