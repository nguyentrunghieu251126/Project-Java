package project.dien_may.project_java.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.dto.muahang.DongGioHang;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.service.ChiTietSanPhamService;
import project.dien_may.project_java.service.admin.NghiepVuException;
import project.dien_may.project_java.service.muahang.GioHangService;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/gio-hang")
public class GioHangController {

    private static final String VIEW = "gio-hang/GioHang";
    private static final String VE_GIO_HANG = "redirect:/gio-hang";
    private static final String TU_TAI_KHOAN = "tai-khoan";
    private static final int SO_GOI_Y = 5;

    private final GioHangService gioHangService;
    private final SanPhamRepository sanPhamRepository;
    private final ChiTietSanPhamService chiTietSanPhamService;

    public GioHangController(GioHangService gioHangService, SanPhamRepository sanPhamRepository,
            ChiTietSanPhamService chiTietSanPhamService) {
        this.gioHangService = gioHangService;
        this.sanPhamRepository = sanPhamRepository;
        this.chiTietSanPhamService = chiTietSanPhamService;
    }

    @GetMapping
    public String gioHang(@RequestParam(name = "chon", required = false) List<Integer> chon,
            @RequestParam(name = "tu", required = false) String tu,
            Principal principal, Model model) {
        List<DongGioHang> dong = gioHangService.layGioHang(principal.getName());
        Set<Integer> trongGio = dong.stream().map(DongGioHang::maSanPham).collect(Collectors.toSet());
        List<SanPham> goiY = sanPhamRepository.findTop5ByDangKinhDoanhTrueOrderByMaSanPhamDesc().stream()
                .filter(sp -> !trongGio.contains(sp.getMaSanPham()) && sp.getSoLuong() > 0)
                .limit(SO_GOI_Y).toList();
        Map<Integer, String> anhGoiY = new LinkedHashMap<>();
        goiY.forEach(sp -> anhGoiY.put(sp.getMaSanPham(), chiTietSanPhamService.layAnhDaiDien(sp)));
        model.addAttribute("dsDong", dong);
        model.addAttribute("dsChon", chon == null ? List.of() : chon);
        model.addAttribute("goiY", goiY);
        model.addAttribute("anhGoiY", anhGoiY);
        model.addAttribute("tuTaiKhoan", TU_TAI_KHOAN.equals(tu));
        return VIEW;
    }

    @PostMapping("/them")
    public String them(@RequestParam Integer maSanPham,
            @RequestParam(defaultValue = "1") int soLuong,
            @RequestParam(defaultValue = "false") boolean muaNgay,
            @RequestHeader(name = "Referer", required = false) String trangTruoc,
            HttpServletRequest request, Principal principal, RedirectAttributes redirect) {
        try {
            String ten = gioHangService.them(principal.getName(), maSanPham, soLuong);
            if (muaNgay) {
                return "redirect:/dat-hang?sanPham=" + maSanPham;
            }
            redirect.addFlashAttribute("thongBao", "Đã thêm " + ten + " vào giỏ hàng.");
        } catch (NghiepVuException e) {
            if (muaNgay && e.getMessage() != null && e.getMessage().startsWith("Giỏ hàng đã có")) {
                return "redirect:/dat-hang?sanPham=" + maSanPham;
            }
            redirect.addFlashAttribute("loi", e.getMessage());
        }
        return "redirect:" + duongDanQuayLai(trangTruoc, request);
    }

    @PostMapping("/them-nhanh")
    @ResponseBody
    public Map<String, Object> themNhanh(@RequestParam Integer maSanPham,
            @RequestParam(defaultValue = "1") int soLuong,
            Principal principal) {
        Map<String, Object> ketQua = new LinkedHashMap<>();
        try {
            String ten = gioHangService.them(principal.getName(), maSanPham, soLuong);
            ketQua.put("thanhCong", true);
            ketQua.put("thongBao", "Đã thêm " + ten + " vào giỏ hàng.");
        } catch (NghiepVuException e) {
            ketQua.put("thanhCong", false);
            ketQua.put("thongBao", e.getMessage());
        }
        ketQua.put("soLuongGioHang", gioHangService.demSanPham(principal.getName()));
        return ketQua;
    }

    private static String duongDanQuayLai(String trangTruoc, HttpServletRequest request) {
        String goc = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        if (trangTruoc != null && trangTruoc.startsWith(goc)) {
            String duongDan = trangTruoc.substring(goc.length());
            String ctx = request.getContextPath();
            if (!ctx.isEmpty() && duongDan.startsWith(ctx)) {
                duongDan = duongDan.substring(ctx.length());
            }
            if (duongDan.startsWith("/") && !duongDan.startsWith("//")) {
                return duongDan;
            }
        }
        return "/gio-hang";
    }

    @PostMapping("/cap-nhat")
    @ResponseBody
    public Map<String, Object> capNhat(@RequestParam Integer maSanPham, @RequestParam int soLuong,
            Principal principal) {
        Map<String, Object> ketQua = new LinkedHashMap<>();
        try {
            ketQua.put("soLuong", gioHangService.capNhatSoLuong(principal.getName(), maSanPham, soLuong));
            ketQua.put("soLuongGioHang", gioHangService.demSanPham(principal.getName()));
            ketQua.put("thanhCong", true);
        } catch (NghiepVuException e) {
            ketQua.put("thanhCong", false);
            ketQua.put("thongBao", e.getMessage());
        }
        return ketQua;
    }

    @PostMapping("/xoa")
    public String xoa(@RequestParam Integer maSanPham, HttpServletRequest request, Principal principal,
            RedirectAttributes redirect) {
        gioHangService.xoa(principal.getName(), List.of(maSanPham));
        redirect.addFlashAttribute("thongBao", "Đã xóa sản phẩm khỏi giỏ hàng.");
        return veGioHang(request);
    }

    @PostMapping("/xoa-nhieu")
    public String xoaNhieu(@RequestParam(name = "maSanPham", required = false) List<Integer> dsMaSanPham,
            HttpServletRequest request, Principal principal, RedirectAttributes redirect) {
        int soDong = gioHangService.xoa(principal.getName(), dsMaSanPham);
        if (soDong > 0) {
            redirect.addFlashAttribute("thongBao", "Đã xóa " + soDong + " sản phẩm khỏi giỏ hàng.");
        }
        return veGioHang(request);
    }

    private static String veGioHang(HttpServletRequest request) {
        String trangTruoc = request.getHeader("Referer");
        return trangTruoc != null && trangTruoc.contains("tu=" + TU_TAI_KHOAN)
                ? VE_GIO_HANG + "?tu=" + TU_TAI_KHOAN
                : VE_GIO_HANG;
    }
}
