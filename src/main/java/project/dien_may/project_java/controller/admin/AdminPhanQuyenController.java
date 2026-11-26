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

@Controller
@RequestMapping("/admin/phan-quyen")
public class AdminPhanQuyenController {

    private final PhanQuyenService phanQuyenService;

    public AdminPhanQuyenController(PhanQuyenService phanQuyenService) {
        this.phanQuyenService = phanQuyenService;
    }

    /** vaiTro: NHAN_VIEN_KHO (mặc định) hoặc NHAN_VIEN_BAO_HANH. */
    @GetMapping
    public String toanThe(@RequestParam(required = false) String vaiTro, Model model) {
        String ten = PhanQuyenService.chuanHoaVaiTro(vaiTro);
        model.addAttribute("quyenTheoNhom", phanQuyenService.quyenTheoNhom());
        model.addAttribute("daChon", phanQuyenService.quyenToanTheNhanVien(ten));
        model.addAttribute("chinhRieng", Set.of());
        model.addAttribute("vaiTroHienTai", ten);
        model.addAttribute("dsVaiTroNhanVien", phanQuyenService.dsVaiTroNhanVien());
        return "admin/phan-quyen";
    }

    @PostMapping
    public String luuToanThe(@RequestParam(required = false) String vaiTro,
            @RequestParam(name = "quyen", required = false) List<Integer> dsMaQuyen,
            RedirectAttributes redirect) {
        String ten = PhanQuyenService.chuanHoaVaiTro(vaiTro);
        try {
            phanQuyenService.capNhatToanTheNhanVien(ten, dsMaQuyen == null ? List.of() : dsMaQuyen);
            redirect.addFlashAttribute("thongBao", "Đã lưu phân quyền. Thay đổi có hiệu lực ngay lập tức.");
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        }
        return "redirect:/admin/phan-quyen?vaiTro=" + ten;
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
