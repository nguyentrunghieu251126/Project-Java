package project.dien_may.project_java.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.service.BaoHanhDoiTraService;
import project.dien_may.project_java.service.admin.NghiepVuException;

import java.security.Principal;

/** TV4 – Khách hàng gửi yêu cầu bảo hành (KH20) và đổi trả (KH21). Chỉ ROLE_KHACH_HANG vào được. */
@Controller
@RequestMapping("/bao-hanh-doi-tra")
public class BaoHanhDoiTraController {

    private static final String VIEW = "bao-hanh-doi-tra/BaoHanhDoiTra";
    private static final String VE_TRANG = "redirect:/bao-hanh-doi-tra";

    private final BaoHanhDoiTraService service;

    public BaoHanhDoiTraController(BaoHanhDoiTraService service) {
        this.service = service;
    }

    @GetMapping
    public String trang(Principal principal, Model model) {
        String email = principal.getName();
        model.addAttribute("dsSanPham", service.sanPhamDaNhan(email));
        model.addAttribute("dsBaoHanh", service.lichSuBaoHanh(email));
        model.addAttribute("dsDoiTra", service.lichSuDoiTra(email));
        model.addAttribute("hanDoiTraNgay", BaoHanhDoiTraService.HAN_DOI_TRA_NGAY);
        return VIEW;
    }

    @PostMapping("/bao-hanh")
    public String guiBaoHanh(@RequestParam(required = false) Integer maChiTiet,
                             @RequestParam(required = false) String moTaLoi,
                             Principal principal, RedirectAttributes redirect) {
        try {
            var phieu = service.guiBaoHanh(principal.getName(), maChiTiet, moTaLoi);
            redirect.addFlashAttribute("thongBao", "Đã gửi yêu cầu bảo hành #" + phieu.getMaPhieuBaoHanh()
                    + ". Cửa hàng sẽ liên hệ với bạn sớm.");
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        }
        return VE_TRANG;
    }

    @PostMapping("/doi-tra")
    public String guiDoiTra(@RequestParam(required = false) Integer maChiTiet,
                            @RequestParam(required = false) String lyDoDoiTra,
                            Principal principal, RedirectAttributes redirect) {
        try {
            var phieu = service.guiDoiTra(principal.getName(), maChiTiet, lyDoDoiTra);
            redirect.addFlashAttribute("thongBao", "Đã gửi yêu cầu đổi trả #" + phieu.getMaPhieuDoiTra()
                    + ". Cửa hàng sẽ liên hệ với bạn sớm.");
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        }
        return VE_TRANG;
    }
}
