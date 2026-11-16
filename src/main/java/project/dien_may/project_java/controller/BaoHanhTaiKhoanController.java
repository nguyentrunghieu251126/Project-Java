package project.dien_may.project_java.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.entity.PhieuBaoHanh;
import project.dien_may.project_java.service.HoSoTaiKhoanService;
import project.dien_may.project_java.service.admin.NghiepVuException;
import project.dien_may.project_java.service.muahang.BaoHanhTaiKhoanService;

import java.security.Principal;
import java.time.LocalDate;

/**
 * TV3 – /tai-khoan/bao-hanh: thông tin bảo hành các sản phẩm khách đã nhận, nằm trong khu "Tài khoản của tôi".
 * Lọc theo thời gian mua, sắp xếp theo ngày mua / ngày hết hạn; gửi yêu cầu bảo hành từ ô "Xem chi tiết".
 */
@Controller
@RequestMapping("/tai-khoan/bao-hanh")
public class BaoHanhTaiKhoanController {

    private final BaoHanhTaiKhoanService baoHanhService;
    private final HoSoTaiKhoanService hoSoTaiKhoanService;

    public BaoHanhTaiKhoanController(BaoHanhTaiKhoanService baoHanhService, HoSoTaiKhoanService hoSoTaiKhoanService) {
        this.baoHanhService = baoHanhService;
        this.hoSoTaiKhoanService = hoSoTaiKhoanService;
    }

    @GetMapping
    public String trang(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
                        @RequestParam(required = false) String sapXep,
                        Principal principal, Model model) {
        String email = principal.getName();
        // Nhập ngược khoảng ngày thì đổi chỗ cho đúng.
        if (tuNgay != null && denNgay != null && tuNgay.isAfter(denNgay)) {
            LocalDate tam = tuNgay;
            tuNgay = denNgay;
            denNgay = tam;
        }
        model.addAttribute("menuTaiKhoan", "bao-hanh");
        model.addAttribute("hoSo", hoSoTaiKhoanService.layHoSo(email));
        model.addAttribute("dsBaoHanh", baoHanhService.danhSach(email, tuNgay, denNgay, sapXep));
        model.addAttribute("tuNgay", tuNgay);
        model.addAttribute("denNgay", denNgay);
        model.addAttribute("sapXep", BaoHanhTaiKhoanService.chuanHoaSapXep(sapXep));
        model.addAttribute("toiDaKyTu", BaoHanhTaiKhoanService.TOI_DA_KY_TU);
        return "tai-khoan/BaoHanh";
    }

    /** Gửi yêu cầu bảo hành: tạo phiếu "Chờ tiếp nhận" để nhân viên kho xử lý ở /admin/kho/bao-hanh. */
    @PostMapping("/yeu-cau")
    public String guiYeuCau(@RequestParam(required = false) Integer maChiTiet,
                            @RequestParam(required = false) String moTaLoi,
                            Principal principal, RedirectAttributes redirect) {
        try {
            PhieuBaoHanh phieu = baoHanhService.guiYeuCau(principal.getName(), maChiTiet, moTaLoi);
            redirect.addFlashAttribute("thongBao", "Đã gửi yêu cầu bảo hành #" + phieu.getMaPhieuBaoHanh()
                    + ". Nhân viên cửa hàng sẽ tiếp nhận và liên hệ với bạn sớm.");
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
            redirect.addFlashAttribute("moTaLoiCu", moTaLoi);
        }
        // Mở lại đúng sản phẩm vừa thao tác
        redirect.addFlashAttribute("moChiTiet", maChiTiet);
        return "redirect:/tai-khoan/bao-hanh" + (maChiTiet == null ? "" : "#bh-" + maChiTiet);
    }
}
