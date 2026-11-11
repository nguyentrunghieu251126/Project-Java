package project.dien_may.project_java.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.entity.PhieuXuat;
import project.dien_may.project_java.service.admin.NghiepVuException;
import project.dien_may.project_java.service.admin.XuLyDonHangService;

import java.security.Principal;

/** TV4 – Các nút xử lý trên trang chi tiết đơn hàng (NVK03 – NVK06). Quyền khai báo ở SecurityConfig. */
@Controller
@RequestMapping("/admin/kho/don-hang/{ma}")
public class AdminXuLyDonHangController {

    private final XuLyDonHangService xuLyDonHangService;

    public AdminXuLyDonHangController(XuLyDonHangService xuLyDonHangService) {
        this.xuLyDonHangService = xuLyDonHangService;
    }

    @PostMapping("/xac-nhan")
    public String xacNhan(@PathVariable Integer ma, RedirectAttributes redirect) {
        return thucHien(ma, redirect, () -> {
            xuLyDonHangService.xacNhan(ma);
            return "Đã xác nhận đơn hàng #" + ma + ".";
        });
    }

    @PostMapping("/xuat-kho")
    public String xuatKho(@PathVariable Integer ma, @RequestParam(required = false) String ghiChu,
                          Principal nguoiDung, RedirectAttributes redirect) {
        return thucHien(ma, redirect, () -> {
            PhieuXuat phieu = xuLyDonHangService.xuatKho(ma, nguoiDung.getName(), ghiChu);
            return "Đã lập phiếu xuất #" + phieu.getMaPhieuXuat() + " cho đơn hàng #" + ma + ".";
        });
    }

    @PostMapping("/giao-hang")
    public String giaoHang(@PathVariable Integer ma, RedirectAttributes redirect) {
        return thucHien(ma, redirect, () -> {
            xuLyDonHangService.batDauGiao(ma);
            return "Đơn hàng #" + ma + " chuyển sang đang giao.";
        });
    }

    @PostMapping("/da-giao")
    public String daGiao(@PathVariable Integer ma, RedirectAttributes redirect) {
        return thucHien(ma, redirect, () -> {
            xuLyDonHangService.daGiao(ma);
            return "Đã xác nhận giao thành công đơn hàng #" + ma + ".";
        });
    }

    /** Gom phần bắt lỗi nghiệp vụ dùng chung cho các nút. */
    private String thucHien(Integer ma, RedirectAttributes redirect, ThaoTac thaoTac) {
        try {
            redirect.addFlashAttribute("thongBao", thaoTac.chay());
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        }
        return "redirect:/admin/kho/don-hang/" + ma;
    }

    @FunctionalInterface
    private interface ThaoTac {
        String chay();
    }
}
