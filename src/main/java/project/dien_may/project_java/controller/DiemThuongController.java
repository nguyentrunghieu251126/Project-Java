package project.dien_may.project_java.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import project.dien_may.project_java.dto.muahang.MaUuDai;
import project.dien_may.project_java.service.HoSoTaiKhoanService;
import project.dien_may.project_java.service.muahang.DiemThuongService;
import project.dien_may.project_java.service.muahang.TongQuanTaiKhoanService;

import java.security.Principal;
import java.util.List;

/**
 * TV3 – 3 trang riêng của khu điểm thưởng:
 * /tai-khoan/diem-thuong (số điểm, cách tích / dùng, thể lệ),
 * /tai-khoan/diem-thuong/lich-su (lịch sử cộng / trừ điểm, lọc theo loại),
 * /tai-khoan/uu-dai (mã giảm giá đang dùng được, lọc theo kiểu giảm).
 */
@Controller
@RequestMapping("/tai-khoan")
public class DiemThuongController {

    private static final int SO_UU_DAI_TOI_DA = 50;
    private static final int SO_GIAO_DICH_GAN_DAY = 3;

    private final DiemThuongService diemThuongService;
    private final TongQuanTaiKhoanService tongQuanService;
    private final HoSoTaiKhoanService hoSoTaiKhoanService;

    public DiemThuongController(DiemThuongService diemThuongService, TongQuanTaiKhoanService tongQuanService,
                                HoSoTaiKhoanService hoSoTaiKhoanService) {
        this.diemThuongService = diemThuongService;
        this.tongQuanService = tongQuanService;
        this.hoSoTaiKhoanService = hoSoTaiKhoanService;
    }

    @GetMapping("/diem-thuong")
    public String diemThuong(Principal principal, Model model) {
        String email = principal.getName();
        model.addAttribute("menuTaiKhoan", "diem-thuong");
        model.addAttribute("hoSo", hoSoTaiKhoanService.layHoSo(email));
        model.addAttribute("thongKe", diemThuongService.thongKe(email));
        model.addAttribute("ganDay", diemThuongService.ganDay(email, SO_GIAO_DICH_GAN_DAY));
        return "tai-khoan/DiemThuong";
    }

    @GetMapping("/diem-thuong/lich-su")
    public String lichSuDiem(@RequestParam(required = false) String loai,
                             @RequestParam(defaultValue = "1") int trang, Principal principal, Model model) {
        String email = principal.getName();
        model.addAttribute("menuTaiKhoan", "lich-su-diem");
        model.addAttribute("hoSo", hoSoTaiKhoanService.layHoSo(email));
        model.addAttribute("thongKe", diemThuongService.thongKe(email));
        model.addAttribute("loaiLoc", DiemThuongService.chuanHoaLoai(loai) == null ? "" : loai.toLowerCase());
        model.addAttribute("lichSuDiem", diemThuongService.lichSu(email, loai, trang));
        return "tai-khoan/LichSuDiemThuong";
    }

    @GetMapping("/uu-dai")
    public String uuDai(@RequestParam(required = false) String loai, Principal principal, Model model) {
        List<MaUuDai> tatCa = tongQuanService.uuDai(SO_UU_DAI_TOI_DA);
        String loc = "phan-tram".equals(loai) || "tien".equals(loai) ? loai : "";
        model.addAttribute("menuTaiKhoan", "uu-dai");
        model.addAttribute("hoSo", hoSoTaiKhoanService.layHoSo(principal.getName()));
        model.addAttribute("loaiLoc", loc);
        model.addAttribute("soTatCa", tatCa.size());
        model.addAttribute("soPhanTram", tatCa.stream().filter(MaUuDai::phanTram).count());
        model.addAttribute("soSapHetHan", tatCa.stream().filter(MaUuDai::sapHetHan).count());
        model.addAttribute("dsUuDai", tatCa.stream()
                .filter(m -> loc.isEmpty() || (loc.equals("phan-tram") == m.phanTram()))
                .toList());
        return "tai-khoan/UuDai";
    }
}
