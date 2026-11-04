package project.dien_may.project_java.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import project.dien_may.project_java.entity.TrangThaiDonHang;
import project.dien_may.project_java.entity.TrangThaiYeuCauNhap;
import project.dien_may.project_java.repository.DonHangRepository;
import project.dien_may.project_java.repository.YeuCauNhapHangRepository;
import project.dien_may.project_java.service.admin.BaoCaoService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** Trang tổng quan (QT08). Mỗi khối trên trang tự ẩn theo quyền của người xem. */
@Controller
public class AdminTongQuanController {

    private final BaoCaoService baoCaoService;
    private final DonHangRepository donHangRepository;
    private final YeuCauNhapHangRepository yeuCauRepository;

    public AdminTongQuanController(BaoCaoService baoCaoService, DonHangRepository donHangRepository,
                                   YeuCauNhapHangRepository yeuCauRepository) {
        this.baoCaoService = baoCaoService;
        this.donHangRepository = donHangRepository;
        this.yeuCauRepository = yeuCauRepository;
    }

    @GetMapping("/admin")
    public String tongQuan(Model model) {
        LocalDate homNay = LocalDate.now();
        BigDecimal doanhThu = baoCaoService.tongDoanhThu(homNay.minusDays(29), homNay);
        BigDecimal kyTruoc = baoCaoService.tongDoanhThu(homNay.minusDays(59), homNay.minusDays(30));

        model.addAttribute("doanhThu30Ngay", doanhThu);
        model.addAttribute("thayDoiDoanhThu", phanTramThayDoi(doanhThu, kyTruoc));
        model.addAttribute("soDonChoXacNhan", donHangRepository.countByTrangThai(TrangThaiDonHang.CHO_XAC_NHAN));
        model.addAttribute("soYeuCauChoDuyet", yeuCauRepository.countByTrangThai(TrangThaiYeuCauNhap.CHO_DUYET));
        model.addAttribute("tonKho", baoCaoService.tonKho());
        model.addAttribute("bieuDo", baoCaoService.doanhThuTheoNam(homNay.getYear()).bieuDo());
        return "admin/tong-quan";
    }

    /** null khi kỳ trước bằng 0, vì khi đó không có phần trăm so sánh. */
    private Integer phanTramThayDoi(BigDecimal hienTai, BigDecimal kyTruoc) {
        if (kyTruoc.signum() == 0) {
            return null;
        }
        return hienTai.subtract(kyTruoc).multiply(BigDecimal.valueOf(100))
                .divide(kyTruoc, 0, RoundingMode.HALF_UP).intValue();
    }
}
