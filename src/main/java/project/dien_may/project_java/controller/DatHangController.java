package project.dien_may.project_java.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.dto.muahang.DatHangForm;
import project.dien_may.project_java.dto.muahang.TrangThanhToan;
import project.dien_may.project_java.entity.SoDiaChi;
import project.dien_may.project_java.service.admin.NghiepVuException;
import project.dien_may.project_java.service.muahang.DatHangService;
import project.dien_may.project_java.service.muahang.DonHangKhachHangService;
import project.dien_may.project_java.service.muahang.SoDiaChiService;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TV3 – KH12 – KH15: trang thanh toán, đặt hàng, trang đặt hàng thành công.
 * Địa chỉ nhận hàng chọn từ sổ địa chỉ (SoDiaChi); thêm / sửa / xóa địa chỉ ở DiaChiController.
 */
@Controller
@RequestMapping("/dat-hang")
public class DatHangController {

    private static final Logger log = LoggerFactory.getLogger(DatHangController.class);
    private static final String VIEW = "dat-hang/ThongTinGiaoHang";

    private final DatHangService datHangService;
    private final DonHangKhachHangService donHangService;
    private final SoDiaChiService soDiaChiService;

    public DatHangController(DatHangService datHangService, DonHangKhachHangService donHangService,
                             SoDiaChiService soDiaChiService) {
        this.datHangService = datHangService;
        this.donHangService = donHangService;
        this.soDiaChiService = soDiaChiService;
    }

    /** diaChi: địa chỉ vừa thêm / sửa ở trang Sổ địa chỉ thì chọn sẵn. */
    @GetMapping
    public String trangThanhToan(@RequestParam(name = "sanPham", required = false) List<Integer> dsChon,
                                 @RequestParam(name = "diaChi", required = false) Integer maDiaChi,
                                 Principal principal, Model model, RedirectAttributes redirect) {
        DatHangForm form = new DatHangForm();
        if (dsChon != null) {
            form.setSanPham(dsChon);
        }
        TrangThanhToan trang = datHangService.layTrangThanhToan(principal.getName(), form.getSanPham());
        if (trang.dong().isEmpty()) {
            redirect.addFlashAttribute("loi", "Hãy chọn ít nhất một sản phẩm trong giỏ để thanh toán.");
            return "redirect:/gio-hang";
        }
        form.setSanPham(trang.dong().stream().map(d -> d.maSanPham()).toList());
        form.setMaDiaChi(maDiaChi);
        return hienTrang(model, principal, trang, form);
    }

    @PostMapping
    public String datHang(@Valid @ModelAttribute("datHangForm") DatHangForm form, BindingResult ketQua,
                          Principal principal, Model model) {
        if (!ketQua.hasErrors()) {
            try {
                // Người nhận lấy từ địa chỉ trong sổ (chỉ địa chỉ của chính khách)
                SoDiaChi dc = soDiaChiService.timCuaKhach(principal.getName(), form.getMaDiaChi());
                form.setHoTenNguoiNhan(dc.getTenNguoiNhan());
                form.setSoDienThoaiNguoiNhan(dc.getSoDienThoai());
                form.setDiaChiGiaoHang(dc.getDiaChiDayDu());
                Integer maDon = datHangService.datHang(principal.getName(), form);
                return "redirect:/dat-hang/thanh-cong/" + maDon;
            } catch (NghiepVuException e) {
                model.addAttribute("loi", e.getMessage());
            } catch (DataAccessException e) {
                log.error("Khong the dat hang", e);
                model.addAttribute("loi", "Không thể đặt hàng lúc này. Vui lòng thử lại sau.");
            }
        }
        // Lỗi: dữ liệu đã được rollback, nạp lại giỏ để hiển thị đúng tồn kho mới nhất
        return hienTrang(model, principal,
                datHangService.layTrangThanhToan(principal.getName(), form.getSanPham()), form);
    }

    @GetMapping("/thanh-cong/{maDon}")
    public String thanhCong(@PathVariable Integer maDon, Principal principal, Model model) {
        model.addAttribute("ct", donHangService.layChiTiet(principal.getName(), maDon));
        return "dat-hang/DatHangThanhCong";
    }

    private String hienTrang(Model model, Principal principal, TrangThanhToan trang, DatHangForm form) {
        List<SoDiaChi> dsDiaChi = soDiaChiService.danhSach(principal.getName());
        boolean hopLe = form.getMaDiaChi() != null
                && dsDiaChi.stream().anyMatch(d -> d.getMaDiaChi().equals(form.getMaDiaChi()));
        if (!hopLe) {
            // Chưa chọn (hoặc địa chỉ vừa bị xóa): chọn địa chỉ mặc định, đã xếp đầu danh sách
            form.setMaDiaChi(dsDiaChi.isEmpty() ? null : dsDiaChi.get(0).getMaDiaChi());
        }
        model.addAttribute("trang", trang);
        model.addAttribute("datHangForm", form);
        model.addAttribute("dsDiaChi", dsDiaChi);
        // Trang Sổ địa chỉ lưu xong sẽ quay lại đúng trang thanh toán này
        model.addAttribute("urlQuayLai", "/dat-hang?" + form.getSanPham().stream()
                .map(ma -> "sanPham=" + ma).collect(Collectors.joining("&")));
        return VIEW;
    }
}
