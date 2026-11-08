package project.dien_may.project_java.controller.admin;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import project.dien_may.project_java.service.admin.BaoCaoService;
import project.dien_may.project_java.service.admin.BaoCaoService.BaoCaoNam;

import project.dien_may.project_java.config.TabKhuQuanLy;

import java.time.YearMonth;
import java.util.Optional;

/** Trang tổng quan (QT08): số liệu doanh thu và biểu đồ của năm hiện tại, cùng một nguồn dữ liệu. */
@Controller
public class AdminTongQuanController {

    private final BaoCaoService baoCaoService;

    public AdminTongQuanController(BaoCaoService baoCaoService) {
        this.baoCaoService = baoCaoService;
    }

    @GetMapping("/admin")
    public String tongQuan(Authentication nguoiDung, Model model) {
        // /admin là cửa vào chung của khu quản lý (logo, trang 403...). Không có quyền xem tổng quan thì
        // chuyển sang tab đầu tiên được phép; không được cấp tab nào thì chỉ hiện lời chào, không truy vấn số liệu
        boolean xemTongQuan = nguoiDung.getAuthorities().stream()
                .anyMatch(a -> "TONGQUAN_XEM".equals(a.getAuthority()));
        if (!xemTongQuan) {
            Optional<String> tabDauTien = TabKhuQuanLy.tabDauTien(nguoiDung.getAuthorities());
            if (tabDauTien.isPresent()) {
                return "redirect:" + tabDauTien.get();
            }
            model.addAttribute("tieuDeTrang", "Trang quản lý");
            return "admin/tong-quan";
        }

        model.addAttribute("tieuDeTrang", "Tổng quan");
        YearMonth thangNay = YearMonth.now();
        BaoCaoNam baoCao = baoCaoService.doanhThuTheoNam(thangNay.getYear());
        model.addAttribute("tomTat", baoCaoService.tomTatDoanhThu(baoCao, thangNay.getMonthValue()));
        model.addAttribute("bieuDo", baoCao.bieuDo());
        return "admin/tong-quan";
    }
}
