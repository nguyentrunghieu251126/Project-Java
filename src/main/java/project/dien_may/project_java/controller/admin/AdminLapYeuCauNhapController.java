package project.dien_may.project_java.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.service.admin.KhoAdminService;
import project.dien_may.project_java.service.admin.KhoAdminService.KetQuaNhap;
import project.dien_may.project_java.service.admin.NghiepVuException;

import java.security.Principal;
import java.util.List;

/** TV4 – NVK09: lập một yêu cầu nhập gồm nhiều sản phẩm, để sau đó lập một phiếu nhập nhiều món (NVK10). */
@Controller
@RequestMapping("/admin/kho/yeu-cau-nhap/lap")
public class AdminLapYeuCauNhapController {

    private static final String VIEW = "admin/kho/lap-yeu-cau-nhap";

    private final KhoAdminService khoService;

    public AdminLapYeuCauNhapController(KhoAdminService khoService) {
        this.khoService = khoService;
    }

    /** @param chon mã sản phẩm được tick sẵn khi mở từ một dòng của trang tồn kho */
    @GetMapping
    public String trang(@RequestParam(required = false) Integer chon, Model model) {
        model.addAttribute("dsDong", khoService.sanPhamCoTheYeuCauNhap());
        model.addAttribute("chon", chon);
        model.addAttribute("yeuCauCuaSanPhamChon", chon == null ? null : khoService.yeuCauDangChoTheoSanPham().get(chon));
        return VIEW;
    }

    @PostMapping
    public String lap(@RequestParam(required = false) List<Integer> maSanPham,
            @RequestParam(required = false) List<Integer> soLuong,
            Principal nguoiDung, RedirectAttributes redirect) {
        try {
            KetQuaNhap ketQua = khoService.lapYeuCauNhapNhieu(maSanPham, soLuong, nguoiDung.getName());
            int soMon = maSanPham.size();
            redirect.addFlashAttribute("thongBao", ketQua.daDuyet()
                    ? "Đã lập và duyệt yêu cầu nhập #" + ketQua.maYeuCau() + " gồm " + soMon
                            + " sản phẩm, chờ kho lập phiếu nhập."
                    : "Đã gửi yêu cầu nhập #" + ketQua.maYeuCau() + " gồm " + soMon + " sản phẩm, chờ duyệt.");
            return "redirect:/admin/kho/yeu-cau-nhap/" + ketQua.maYeuCau();
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
            return "redirect:/admin/kho/yeu-cau-nhap/lap";
        }
    }
}
