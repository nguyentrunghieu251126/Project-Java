package project.dien_may.project_java.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.service.SoSanhSanPhamService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SoSanhSanPhamController {
    private final SoSanhSanPhamService soSanhSanPhamService;

    public SoSanhSanPhamController(SoSanhSanPhamService soSanhSanPhamService) {
        this.soSanhSanPhamService = soSanhSanPhamService;
    }

    @GetMapping("/so-sanh")
    public String soSanh(@RequestParam(name = "maSanPham", required = false) List<Integer> maSanPham,
                         Model model) {
        var kq = soSanhSanPhamService.layDuLieu(maSanPham);
        List<Integer> ids = kq.sanPhamDaChon().stream().map(SanPham::getMaSanPham).toList();
        Map<Integer, String> themLink = new LinkedHashMap<>();
        Map<Integer, String> xoaLink = new LinkedHashMap<>();
        for (SanPham sp : kq.sanPhamDeXuat()) {
            List<Integer> ds = new ArrayList<>(ids);
            if (ds.size() < 3) ds.add(sp.getMaSanPham());
            themLink.put(sp.getMaSanPham(), duongDan(ds));
        }
        for (Integer id : ids) {
            List<Integer> ds = new ArrayList<>(ids);
            ds.remove(id);
            xoaLink.put(id, duongDan(ds));
        }
        model.addAttribute("sanPhamDaChon", kq.sanPhamDaChon());
        model.addAttribute("sanPhamDeXuat", kq.sanPhamDeXuat());
        model.addAttribute("tenThongSo", kq.tenThongSo());
        // KH10: moi hang co dung 2-3 gia tri theo thu tu sanPhamDaChon.
        // View khong can truy cap Map hai tang bang bieu thuc Thymeleaf.
        List<List<String>> giaTriTheoDong = new ArrayList<>();
        for (String ten : kq.tenThongSo()) {
            List<String> giaTri = new ArrayList<>();
            for (SanPham sp : kq.sanPhamDaChon()) {
                Map<String, String> thongSo = kq.thongSoTheoSanPham().get(sp.getMaSanPham());
                String giaTriThuc = thongSo == null ? null : thongSo.get(ten);
                giaTri.add(giaTriThuc == null || giaTriThuc.isBlank() ? "—" : giaTriThuc);
            }
            giaTriTheoDong.add(List.copyOf(giaTri));
        }
        model.addAttribute("giaTriTheoDong", giaTriTheoDong);
        model.addAttribute("thongSoTheoSanPham", kq.thongSoTheoSanPham());
        model.addAttribute("anhSanPham", kq.anhSanPham());
        model.addAttribute("themLink", themLink);
        model.addAttribute("xoaLink", xoaLink);
        model.addAttribute("soLuongDaChon", ids.size());
        model.addAttribute("thongBao", kq.thongBao());
        return "sanpham/SoSanhSanPham";
    }

    private String duongDan(List<Integer> ids) {
        if (ids.isEmpty()) return "/so-sanh";
        StringBuilder url = new StringBuilder("/so-sanh?");
        for (Integer id : ids) {
            if (url.charAt(url.length() - 1) != '?') url.append('&');
            url.append("maSanPham=").append(id);
        }
        return url.toString();
    }
}
