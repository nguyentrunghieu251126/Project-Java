package project.dien_may.project_java.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.entity.PhieuDoiTra;
import project.dien_may.project_java.service.BaoHanhDoiTraService;
import project.dien_may.project_java.service.HoSoTaiKhoanService;
import project.dien_may.project_java.service.admin.NghiepVuException;
import project.dien_may.project_java.service.muahang.DiemThuongService;

import java.security.Principal;
import java.util.List;

/** TV4 – KH21: khách gửi yêu cầu đổi trả cho một hoặc nhiều sản phẩm của đơn đã giao. */
@Controller
@RequestMapping("/don-hang/{maDon}/doi-tra")
public class DoiTraDonHangController {

    private static final String VIEW = "don-hang/YeuCauDoiTra";

    private final BaoHanhDoiTraService service;
    private final HoSoTaiKhoanService hoSoTaiKhoanService;
    private final DiemThuongService diemThuongService;

    public DoiTraDonHangController(BaoHanhDoiTraService service, HoSoTaiKhoanService hoSoTaiKhoanService,
            DiemThuongService diemThuongService) {
        this.service = service;
        this.hoSoTaiKhoanService = hoSoTaiKhoanService;
        this.diemThuongService = diemThuongService;
    }

    /** @param chon mã dòng sản phẩm được tick sẵn khi bấm "Đổi trả" ở một sản phẩm cụ thể */
    @GetMapping
    public String trang(@PathVariable Integer maDon, @RequestParam(required = false) Integer chon,
            Principal principal, Model model) {
        String email = principal.getName();
        model.addAttribute("menuTaiKhoan", "don-hang");
        model.addAttribute("hoSo", hoSoTaiKhoanService.layHoSo(email));
        model.addAttribute("diemThuong", diemThuongService.tongDiem(email));
        model.addAttribute("trang", service.trangDoiTra(email, maDon));
        model.addAttribute("dsLyDo", BaoHanhDoiTraService.LY_DO_DOI_TRA);
        model.addAttribute("hanDoiTraNgay", BaoHanhDoiTraService.HAN_DOI_TRA_NGAY);
        model.addAttribute("maHien", String.format("DH%06d", maDon));
        model.addAttribute("chon", chon);
        return VIEW;
    }

    @PostMapping
    public String gui(@PathVariable Integer maDon,
            @RequestParam(required = false) List<Integer> maChiTiet,
            @RequestParam(required = false) String lyDo,
            @RequestParam(required = false) String moTa,
            Principal principal, RedirectAttributes redirect) {
        try {
            List<PhieuDoiTra> dsPhieu = service.guiDoiTraNhieu(principal.getName(), maDon, maChiTiet, lyDo, moTa);
            redirect.addFlashAttribute("thongBao", "Đã gửi yêu cầu đổi trả cho " + dsPhieu.size()
                    + " sản phẩm. Cửa hàng sẽ liên hệ với bạn trong 24 giờ.");
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        }
        return "redirect:/don-hang/" + maDon + "/doi-tra";
    }
}
