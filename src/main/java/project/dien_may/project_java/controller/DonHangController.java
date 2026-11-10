package project.dien_may.project_java.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.service.HoSoTaiKhoanService;
import project.dien_may.project_java.service.admin.NghiepVuException;
import project.dien_may.project_java.service.muahang.DonHangKhachHangService;
import project.dien_may.project_java.service.muahang.GioHangService;
import project.dien_may.project_java.service.muahang.GioHangService.KetQuaMuaLai;

import java.security.Principal;
import java.util.stream.Collectors;

/** TV3 – KH16 – KH19, KH22: lịch sử mua hàng, chi tiết / hóa đơn, hủy đơn, đánh giá sản phẩm. */
@Controller
@RequestMapping("/don-hang")
public class DonHangController {

    private final DonHangKhachHangService donHangService;
    private final HoSoTaiKhoanService hoSoTaiKhoanService;
    private final GioHangService gioHangService;

    public DonHangController(DonHangKhachHangService donHangService, HoSoTaiKhoanService hoSoTaiKhoanService,
                             GioHangService gioHangService) {
        this.donHangService = donHangService;
        this.hoSoTaiKhoanService = hoSoTaiKhoanService;
        this.gioHangService = gioHangService;
    }

    @GetMapping
    public String lichSu(@RequestParam(required = false) String trangThai,
                         @RequestParam(required = false) String tuKhoa,
                         @RequestParam(defaultValue = "1") int trang,
                         Principal principal, Model model) {
        String tab = DonHangKhachHangService.chuanHoaTab(trangThai);
        model.addAttribute("menuTaiKhoan", "don-hang");
        model.addAttribute("hoSo", hoSoTaiKhoanService.layHoSo(principal.getName()));
        model.addAttribute("dsTab", donHangService.demTheoTab(principal.getName()));
        model.addAttribute("tabHienTai", tab);
        model.addAttribute("tuKhoaDon", tuKhoa);
        model.addAttribute("trangDon", donHangService.layLichSu(principal.getName(), tab, tuKhoa, trang));
        return "don-hang/DanhSachDonHang";
    }

    @GetMapping("/{maDon}")
    public String chiTiet(@PathVariable Integer maDon, Principal principal, Model model) {
        model.addAttribute("menuTaiKhoan", "don-hang");
        model.addAttribute("hoSo", hoSoTaiKhoanService.layHoSo(principal.getName()));
        model.addAttribute("ct", donHangService.layChiTiet(principal.getName(), maDon));
        return "don-hang/ChiTietDonHang";
    }

    @PostMapping("/{maDon}/huy")
    public String huy(@PathVariable Integer maDon, Principal principal, RedirectAttributes redirect) {
        try {
            donHangService.huyDon(principal.getName(), maDon);
            redirect.addFlashAttribute("thongBao", "Đã hủy đơn hàng #DH" + String.format("%06d", maDon) + ".");
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        }
        return "redirect:/don-hang/" + maDon;
    }

    /** Ý tưởng "Mua lại" của Project-Website: đưa sản phẩm của đơn vào giỏ và chọn sẵn chúng. */
    @PostMapping("/{maDon}/mua-lai")
    public String muaLai(@PathVariable Integer maDon, Principal principal, RedirectAttributes redirect) {
        try {
            KetQuaMuaLai ketQua = gioHangService.muaLai(principal.getName(), maDon);
            redirect.addFlashAttribute("thongBao", "Đã thêm " + ketQua.daThem().size()
                    + " sản phẩm của đơn #DH" + String.format("%06d", maDon) + " vào giỏ hàng.");
            if (!ketQua.hetHang().isEmpty()) {
                redirect.addFlashAttribute("loi", "Đã hết hàng: " + String.join(", ", ketQua.hetHang()) + ".");
            }
            return "redirect:/gio-hang?chon=" + ketQua.daThem().stream().map(String::valueOf)
                    .collect(Collectors.joining(","));
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
            return "redirect:/don-hang/" + maDon;
        }
    }

    @PostMapping("/{maDon}/danh-gia")
    public String danhGia(@PathVariable Integer maDon, @RequestParam Integer maSanPham,
                          @RequestParam(required = false) Integer soSao,
                          @RequestParam(required = false) String noiDungDanhGia,
                          Principal principal, RedirectAttributes redirect) {
        try {
            donHangService.danhGia(principal.getName(), maDon, maSanPham, soSao, noiDungDanhGia);
            redirect.addFlashAttribute("thongBao", "Cảm ơn bạn đã đánh giá sản phẩm!");
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            // Hai lần gửi cùng lúc: ràng buộc UQ_DanhGia chặn bản ghi thứ hai
            redirect.addFlashAttribute("loi", "Bạn đã đánh giá sản phẩm này rồi.");
        }
        return "redirect:/don-hang/" + maDon + "#san-pham";
    }
}
