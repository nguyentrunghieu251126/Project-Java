package project.dien_may.project_java.controller.admin;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.service.admin.GiaoHangService;
import project.dien_may.project_java.service.admin.GiaoHangService.TabGiaoHang;
import project.dien_may.project_java.service.admin.NghiepVuException;

import java.security.Principal;

/** TV4 – Nhân viên giao hàng: nhận đơn đã xuất kho, giao thành công hoặc báo giao thất bại (NVGH01 – NVGH04). */
@Controller
@RequestMapping("/admin/giao-hang")
public class AdminGiaoHangController {

    private static final String VE_DANH_SACH = "redirect:/admin/giao-hang";

    private final GiaoHangService giaoHangService;

    public AdminGiaoHangController(GiaoHangService giaoHangService) {
        this.giaoHangService = giaoHangService;
    }

    @GetMapping
    public String danhSach(@RequestParam(defaultValue = "CHO_NHAN") TabGiaoHang tab,
            @RequestParam(defaultValue = "1") int trang,
            Principal nguoiDung, Model model) {
        // Chờ nhận: đơn cũ nhất lên trước; đang giao và đã xong: đơn mới nhất lên trước
        Sort sapXep = tab == TabGiaoHang.CHO_NHAN
                ? Sort.by(Sort.Direction.ASC, "ngayDat", "maDonHang")
                : Sort.by(Sort.Direction.DESC, "ngayDat", "maDonHang");
        model.addAttribute("tab", tab);
        model.addAttribute("dsTab", TabGiaoHang.values());
        model.addAttribute("soDon", giaoHangService.soDon(nguoiDung.getName()));
        model.addAttribute("trang", giaoHangService.danhSach(nguoiDung.getName(), tab, PhanTrang.tao(trang, sapXep)));
        return "admin/giao-hang/danh-sach";
    }

    @GetMapping("/{ma}")
    public String chiTiet(@PathVariable Integer ma, Principal nguoiDung, Model model) {
        model.addAttribute("ct", giaoHangService.chiTiet(ma, nguoiDung.getName()));
        return "admin/giao-hang/chi-tiet";
    }

    @PostMapping("/{ma}/nhan")
    public String nhan(@PathVariable Integer ma, Principal nguoiDung, RedirectAttributes redirect) {
        try {
            giaoHangService.nhanDon(ma, nguoiDung.getName());
            redirect.addFlashAttribute("thongBao", "Đã nhận đơn hàng #" + ma + ", đơn chuyển sang đang giao.");
            return "redirect:/admin/giao-hang/" + ma;
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
            return VE_DANH_SACH;
        }
    }

    @PostMapping("/{ma}/da-giao")
    public String daGiao(@PathVariable Integer ma, Principal nguoiDung, RedirectAttributes redirect) {
        try {
            giaoHangService.daGiao(ma, nguoiDung.getName());
            redirect.addFlashAttribute("thongBao", "Đã xác nhận giao thành công đơn hàng #" + ma + ".");
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        }
        return "redirect:/admin/giao-hang/" + ma;
    }

    @PostMapping("/{ma}/that-bai")
    public String thatBai(@PathVariable Integer ma, @RequestParam(required = false) String lyDo,
            Principal nguoiDung, RedirectAttributes redirect) {
        try {
            giaoHangService.giaoThatBai(ma, nguoiDung.getName(), lyDo);
            redirect.addFlashAttribute("thongBao", "Đã ghi nhận giao thất bại đơn hàng #" + ma
                    + ", hàng được hoàn về kho.");
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        }
        return "redirect:/admin/giao-hang/" + ma;
    }
}
