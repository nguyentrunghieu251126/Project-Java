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

/** TV3 – KH11: giỏ hàng. Quyền KHACH_HANG đã khai báo trong SecurityConfig (/gio-hang/**). */
@Controller
@RequestMapping("/gio-hang")
public class GioHangController {

    private static final String VIEW = "gio-hang/GioHang";
    private static final String VE_GIO_HANG = "redirect:/gio-hang";
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

    /** chon: danh sách mã sản phẩm được tick sẵn (sau khi "Mua lại"); rỗng thì tick mọi dòng mua được. */
    @GetMapping
    public String gioHang(@RequestParam(name = "chon", required = false) List<Integer> chon,
                          Principal principal, Model model) {
        List<DongGioHang> dong = gioHangService.layGioHang(principal.getName());
        Set<Integer> trongGio = dong.stream().map(DongGioHang::maSanPham).collect(Collectors.toSet());
        // Gợi ý mua kèm: sản phẩm mới nhất còn hàng, chưa có trong giỏ (phần lớn là phụ kiện)
        List<SanPham> goiY = sanPhamRepository.findTop5ByDangKinhDoanhTrueOrderByMaSanPhamDesc().stream()
                .filter(sp -> !trongGio.contains(sp.getMaSanPham()) && sp.getSoLuong() > 0)
                .limit(SO_GOI_Y).toList();
        Map<Integer, String> anhGoiY = new LinkedHashMap<>();
        goiY.forEach(sp -> anhGoiY.put(sp.getMaSanPham(), chiTietSanPhamService.layAnhDaiDien(sp)));
        model.addAttribute("dsDong", dong);
        model.addAttribute("dsChon", chon == null ? List.of() : chon);
        model.addAttribute("goiY", goiY);
        model.addAttribute("anhGoiY", anhGoiY);
        return VIEW;
    }

    /**
     * Form thường (không có JavaScript): muaNgay = true thì sang thẳng thanh toán với riêng sản phẩm này,
     * ngược lại thêm vào giỏ rồi quay về đúng trang khách đang xem.
     */
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
                // Đã đủ số lượng trong giỏ: vẫn cho mua ngay sản phẩm này
                return "redirect:/dat-hang?sanPham=" + maSanPham;
            }
            redirect.addFlashAttribute("loi", e.getMessage());
        }
        return "redirect:" + duongDanQuayLai(trangTruoc, request);
    }

    /**
     * TV3: nút giỏ hàng trên thẻ sản phẩm gọi bằng fetch (gioHangNhanh.js) để khách ở nguyên trang.
     * Trả về thông báo và số sản phẩm mới trong giỏ để cập nhật huy hiệu trên header.
     */
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

    /** Chỉ quay về trang trong cùng website (chống chuyển hướng ra ngoài); không rõ thì về giỏ hàng. */
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

    /** Gọi bằng fetch từ nút +/-; trả về số lượng thực tế sau khi giới hạn theo tồn kho. */
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
    public String xoa(@RequestParam Integer maSanPham, Principal principal, RedirectAttributes redirect) {
        gioHangService.xoa(principal.getName(), List.of(maSanPham));
        redirect.addFlashAttribute("thongBao", "Đã xóa sản phẩm khỏi giỏ hàng.");
        return VE_GIO_HANG;
    }

    @PostMapping("/xoa-nhieu")
    public String xoaNhieu(@RequestParam(name = "maSanPham", required = false) List<Integer> dsMaSanPham,
                           Principal principal, RedirectAttributes redirect) {
        int soDong = gioHangService.xoa(principal.getName(), dsMaSanPham);
        if (soDong > 0) {
            redirect.addFlashAttribute("thongBao", "Đã xóa " + soDong + " sản phẩm khỏi giỏ hàng.");
        }
        return VE_GIO_HANG;
    }
}
