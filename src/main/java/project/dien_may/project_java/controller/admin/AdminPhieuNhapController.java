package project.dien_may.project_java.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.entity.PhieuNhap;
import project.dien_may.project_java.service.admin.NghiepVuException;
import project.dien_may.project_java.service.admin.PhieuNhapService;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

/** TV4 – NVK10: Nhân viên kho lập phiếu nhập cho yêu cầu đã duyệt. Quyền KHO_NHAP_HANG khai báo ở SecurityConfig. */
@Controller
public class AdminPhieuNhapController {

    private final PhieuNhapService phieuNhapService;

    public AdminPhieuNhapController(PhieuNhapService phieuNhapService) {
        this.phieuNhapService = phieuNhapService;
    }

    @PostMapping("/admin/kho/yeu-cau-nhap/{ma}/lap-phieu-nhap")
    public String lapPhieuNhap(@PathVariable Integer ma,
                               @RequestParam(required = false) List<Integer> maChiTiet,
                               @RequestParam(required = false) List<BigDecimal> donGia,
                               @RequestParam(required = false) String ghiChu,
                               Principal nguoiDung, RedirectAttributes redirect) {
        try {
            PhieuNhap phieu = phieuNhapService.lapPhieuNhap(ma, nguoiDung.getName(), maChiTiet, donGia, ghiChu);
            redirect.addFlashAttribute("thongBao", "Đã lập phiếu nhập #" + phieu.getMaPhieuNhap()
                    + ", hàng đã được cộng vào tồn kho.");
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        }
        return "redirect:/admin/kho/yeu-cau-nhap/" + ma;
    }
}
