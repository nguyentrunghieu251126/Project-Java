package project.dien_may.project_java.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import project.dien_may.project_java.service.admin.BaoCaoService;
import project.dien_may.project_java.service.admin.BaoCaoService.BaoCaoNam;

import java.time.YearMonth;

/** Trang tổng quan (QT08): số liệu doanh thu và biểu đồ của năm hiện tại, cùng một nguồn dữ liệu. */
@Controller
public class AdminTongQuanController {

    private final BaoCaoService baoCaoService;

    public AdminTongQuanController(BaoCaoService baoCaoService) {
        this.baoCaoService = baoCaoService;
    }

    @GetMapping("/admin")
    public String tongQuan(Model model) {
        YearMonth thangNay = YearMonth.now();
        BaoCaoNam baoCao = baoCaoService.doanhThuTheoNam(thangNay.getYear());

        model.addAttribute("tomTat", baoCaoService.tomTatDoanhThu(baoCao, thangNay.getMonthValue()));
        model.addAttribute("bieuDo", baoCao.bieuDo());
        return "admin/tong-quan";
    }
}
