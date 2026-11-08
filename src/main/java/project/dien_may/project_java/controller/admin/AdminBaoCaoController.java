package project.dien_may.project_java.controller.admin;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import project.dien_may.project_java.entity.TrangThaiYeuCauNhap;
import project.dien_may.project_java.repository.ChiTietYeuCauNhapHangRepository;
import project.dien_may.project_java.repository.DanhMucRepository;
import project.dien_may.project_java.service.admin.BaoCaoService;

import java.time.LocalDate;
import java.util.List;

/** Báo cáo doanh thu, sản phẩm bán chạy, tồn kho (QT09 – QT11). */
@Controller
@RequestMapping("/admin/bao-cao")
public class AdminBaoCaoController {

    private final BaoCaoService baoCaoService;
    private final DanhMucRepository danhMucRepository;
    private final ChiTietYeuCauNhapHangRepository chiTietYeuCauRepository;

    public AdminBaoCaoController(BaoCaoService baoCaoService, DanhMucRepository danhMucRepository,
                                 ChiTietYeuCauNhapHangRepository chiTietYeuCauRepository) {
        this.baoCaoService = baoCaoService;
        this.danhMucRepository = danhMucRepository;
        this.chiTietYeuCauRepository = chiTietYeuCauRepository;
    }

    @GetMapping("/doanh-thu")
    public String doanhThu(@RequestParam(defaultValue = "nam") String tab,
                           @RequestParam(required = false) Integer nam,
                           @RequestParam(required = false) Integer thang,
                           Model model) {
        LocalDate homNay = LocalDate.now();
        List<Integer> dsNam = baoCaoService.dsNam();
        // dsNam đi từ năm hiện tại về năm cũ nhất; năm ngoài khoảng này thì đưa về biên gần nhất
        int namXem = nam == null ? homNay.getYear()
                : Math.clamp(nam, dsNam.get(dsNam.size() - 1), dsNam.get(0));
        int thangToiDa = namXem == homNay.getYear() ? homNay.getMonthValue() : 12;

        model.addAttribute("dsNam", dsNam);
        model.addAttribute("nam", namXem);
        model.addAttribute("thangToiDa", thangToiDa);
        if ("thang".equals(tab)) {
            int thangXem = thang == null ? thangToiDa : Math.clamp(thang, 1, thangToiDa);
            model.addAttribute("tab", "thang");
            model.addAttribute("thang", thangXem);
            model.addAttribute("baoCaoThang", baoCaoService.doanhThuTheoThang(namXem, thangXem));
        } else {
            model.addAttribute("tab", "nam");
            model.addAttribute("baoCaoNam", baoCaoService.doanhThuTheoNam(namXem));
        }
        return "admin/bao-cao/doanh-thu";
    }

    @GetMapping("/ban-chay")
    public String banChay(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
                          @RequestParam(required = false) Integer maDanhMuc,
                          @RequestParam(defaultValue = "10") int top,
                          Model model) {
        KhoangNgay khoang = KhoangNgay.tu(tuNgay, denNgay);
        int soLuong = Math.clamp(top, 1, 100);
        model.addAttribute("tuNgay", khoang.tu());
        model.addAttribute("denNgay", khoang.den());
        model.addAttribute("maDanhMuc", maDanhMuc);
        model.addAttribute("top", soLuong);
        model.addAttribute("dsDanhMuc", danhMucRepository.findAllByOrderByTenDanhMucAsc());
        model.addAttribute("dsBanChay", baoCaoService.banChay(khoang.tu(), khoang.den(), maDanhMuc, soLuong));
        return "admin/bao-cao/ban-chay";
    }

    @GetMapping("/ton-kho")
    public String tonKho(Model model) {
        model.addAttribute("baoCao", baoCaoService.tonKho());
        // Sản phẩm đã có phiếu chờ duyệt thì không lập thêm phiếu, chỉ báo đang chờ
        model.addAttribute("dangChoDuyet", chiTietYeuCauRepository.maSanPhamTheoTrangThai(TrangThaiYeuCauNhap.CHO_DUYET));
        return "admin/bao-cao/ton-kho";
    }

    /** Mặc định sáu tháng gần nhất; nhập ngược chiều thì tự đảo lại. */
    private record KhoangNgay(LocalDate tu, LocalDate den) {

        static KhoangNgay tu(LocalDate tuNgay, LocalDate denNgay) {
            LocalDate den = denNgay != null ? denNgay : LocalDate.now();
            LocalDate tu = tuNgay != null ? tuNgay : den.minusMonths(5).withDayOfMonth(1);
            return tu.isAfter(den) ? new KhoangNgay(den, tu) : new KhoangNgay(tu, den);
        }
    }
}
