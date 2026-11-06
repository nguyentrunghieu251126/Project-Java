package project.dien_may.project_java.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import project.dien_may.project_java.dto.DanhGiaHienThi;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.service.ChiTietSanPhamService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class SanPhamController {
    private final ChiTietSanPhamService chiTietSanPhamService;

    public SanPhamController(ChiTietSanPhamService chiTietSanPhamService) {
        this.chiTietSanPhamService = chiTietSanPhamService;
    }

    @GetMapping("/san-pham")
    public String danhSachSanPham() {
        return "redirect:/#product-list";
    }

    // Chỉ trả về đường dẫn ảnh của các sản phẩm đã xem. Không thay đổi SQL.
    @GetMapping("/san-pham/anh-da-xem")
    @ResponseBody
    public Map<Integer, String> anhDaXem(@RequestParam(name = "maSanPham", required = false) List<Integer> maSanPham) {
        return chiTietSanPhamService.layAnhSanPhamDaXem(maSanPham);
    }

    @GetMapping("/san-pham/{maSanPham}")
    public String chiTietSanPham(@PathVariable Integer maSanPham,
                                 Model model, HttpServletResponse response) {
        Optional<SanPham> optional = chiTietSanPhamService.laySanPham(maSanPham);
        if (optional.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return "sanpham/KhongTimThay";
        }
        SanPham sanPham = optional.get();
        List<DanhGiaHienThi> danhGia = chiTietSanPhamService.layDanhGia(maSanPham);
        Map<Integer, Integer> soLuongTheoSao = new LinkedHashMap<>();
        for (int soSao = 5; soSao >= 1; soSao--) soLuongTheoSao.put(soSao, 0);
        int tongDiem = 0;
        for (DanhGiaHienThi dg : danhGia) {
            if (dg.soSao() != null && soLuongTheoSao.containsKey(dg.soSao())) {
                soLuongTheoSao.merge(dg.soSao(), 1, Integer::sum);
                tongDiem += dg.soSao();
            }
        }

        List<SanPham> tuongTu = chiTietSanPhamService.laySanPhamTuongTu(sanPham);
        Map<Integer, String> anhTuongTu = new LinkedHashMap<>();
        for (SanPham sp : tuongTu) {
            anhTuongTu.put(sp.getMaSanPham(), chiTietSanPhamService.layAnhDaiDien(sp));
        }
        model.addAttribute("sanPham", sanPham);
        model.addAttribute("danhSachThongSo", chiTietSanPhamService.layThongSo(maSanPham));
        model.addAttribute("danhSachAnh", chiTietSanPhamService.layDanhSachAnh(sanPham));
        model.addAttribute("danhSachDanhGia", danhGia);
        model.addAttribute("tongDanhGia", danhGia.size());
        model.addAttribute("diemTrungBinh", danhGia.isEmpty() ? null : ((double) tongDiem / danhGia.size()));
        model.addAttribute("soLuongTheoSao", soLuongTheoSao);
        model.addAttribute("danhSachTuongTu", tuongTu);
        model.addAttribute("anhTuongTu", anhTuongTu);
        return "sanpham/ChiTietSanPham";
    }
}
