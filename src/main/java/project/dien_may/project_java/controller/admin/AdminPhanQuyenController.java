package project.dien_may.project_java.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.service.admin.NghiepVuException;
import project.dien_may.project_java.service.admin.PhanQuyenService;

import java.util.List;

/** Phân quyền theo chức năng (QT16). */
@Controller
@RequestMapping("/admin/phan-quyen")
public class AdminPhanQuyenController {

    private final PhanQuyenService phanQuyenService;

    public AdminPhanQuyenController(PhanQuyenService phanQuyenService) {
        this.phanQuyenService = phanQuyenService;
    }

    @GetMapping
    public String hienThi(Model model) {
        model.addAttribute("dsVaiTro", phanQuyenService.dsVaiTro());
        model.addAttribute("quyenTheoNhom", phanQuyenService.quyenTheoNhom());
        model.addAttribute("daCap", phanQuyenService.quyenDaCap());
        model.addAttribute("oBiKhoa", phanQuyenService.oBiKhoa());
        model.addAttribute("vaiTroQuanTri", PhanQuyenService.VAI_TRO_QUAN_TRI);
        model.addAttribute("quyenKhongTheGo", PhanQuyenService.QUYEN_KHONG_THE_GO);
        return "admin/phan-quyen";
    }

    @PostMapping
    public String luu(@RequestParam(name = "cap", required = false) List<String> dsCap, RedirectAttributes redirect) {
        try {
            phanQuyenService.capNhat(dsCap == null ? List.of() : dsCap);
            redirect.addFlashAttribute("thongBao", "Đã lưu phân quyền. Thay đổi có hiệu lực ngay lập tức.");
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        }
        return "redirect:/admin/phan-quyen";
    }
}
