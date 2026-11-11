package project.dien_may.project_java.controller.admin;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.entity.TrangThaiBaoHanh;
import project.dien_may.project_java.entity.TrangThaiDoiTra;
import project.dien_may.project_java.service.BaoHanhDoiTraService;
import project.dien_may.project_java.service.admin.NghiepVuException;

import java.security.Principal;

/** TV4 – Nhân viên kho tiếp nhận và xử lý bảo hành (NVK11, NVK13), đổi trả (NVK12, NVK14). */
@Controller
@RequestMapping("/admin/kho")
public class AdminBaoHanhDoiTraController {

    private static final Sort MOI_NHAT = Sort.by(Sort.Direction.DESC, "ngayYeuCau");

    private final BaoHanhDoiTraService service;

    public AdminBaoHanhDoiTraController(BaoHanhDoiTraService service) {
        this.service = service;
    }

    // ===== Bảo hành =====

    @GetMapping("/bao-hanh")
    public String baoHanh(@RequestParam(required = false) TrangThaiBaoHanh trangThai,
                          @RequestParam(defaultValue = "1") int trang, Model model) {
        model.addAttribute("trang", service.timBaoHanh(trangThai, PhanTrang.tao(trang, MOI_NHAT)));
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("dsTrangThai", TrangThaiBaoHanh.values());
        model.addAttribute("soChoTiepNhan", service.demBaoHanhChoTiepNhan());
        return "admin/kho/bao-hanh";
    }

    @PostMapping("/bao-hanh/{ma}/tiep-nhan")
    public String tiepNhanBaoHanh(@PathVariable Integer ma, Principal nguoiDung, RedirectAttributes redirect) {
        return thucHien(redirect, "redirect:/admin/kho/bao-hanh", () -> {
            service.tiepNhanBaoHanh(ma, nguoiDung.getName());
            return "Đã tiếp nhận phiếu bảo hành #" + ma + ".";
        });
    }

    @PostMapping("/bao-hanh/{ma}/hoan-thanh")
    public String hoanThanhBaoHanh(@PathVariable Integer ma, @RequestParam(required = false) String ketQua,
                                   Principal nguoiDung, RedirectAttributes redirect) {
        return thucHien(redirect, "redirect:/admin/kho/bao-hanh", () -> {
            service.hoanThanhBaoHanh(ma, nguoiDung.getName(), ketQua);
            return "Đã hoàn thành phiếu bảo hành #" + ma + ".";
        });
    }

    @PostMapping("/bao-hanh/{ma}/tu-choi")
    public String tuChoiBaoHanh(@PathVariable Integer ma, @RequestParam(required = false) String ketQua,
                                Principal nguoiDung, RedirectAttributes redirect) {
        return thucHien(redirect, "redirect:/admin/kho/bao-hanh", () -> {
            service.tuChoiBaoHanh(ma, nguoiDung.getName(), ketQua);
            return "Đã từ chối phiếu bảo hành #" + ma + ".";
        });
    }

    // ===== Đổi trả =====

    @GetMapping("/doi-tra")
    public String doiTra(@RequestParam(required = false) TrangThaiDoiTra trangThai,
                         @RequestParam(defaultValue = "1") int trang, Model model) {
        model.addAttribute("trang", service.timDoiTra(trangThai, PhanTrang.tao(trang, MOI_NHAT)));
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("dsTrangThai", TrangThaiDoiTra.values());
        model.addAttribute("soChoTiepNhan", service.demDoiTraChoTiepNhan());
        return "admin/kho/doi-tra";
    }

    @PostMapping("/doi-tra/{ma}/chap-nhan")
    public String chapNhanDoiTra(@PathVariable Integer ma, Principal nguoiDung, RedirectAttributes redirect) {
        return thucHien(redirect, "redirect:/admin/kho/doi-tra", () -> {
            service.chapNhanDoiTra(ma, nguoiDung.getName());
            return "Đã chấp nhận phiếu đổi trả #" + ma + ".";
        });
    }

    @PostMapping("/doi-tra/{ma}/hoan-thanh")
    public String hoanThanhDoiTra(@PathVariable Integer ma, @RequestParam(required = false) String ketQua,
                                  Principal nguoiDung, RedirectAttributes redirect) {
        return thucHien(redirect, "redirect:/admin/kho/doi-tra", () -> {
            service.hoanThanhDoiTra(ma, nguoiDung.getName(), ketQua);
            return "Đã hoàn thành phiếu đổi trả #" + ma + ".";
        });
    }

    @PostMapping("/doi-tra/{ma}/tu-choi")
    public String tuChoiDoiTra(@PathVariable Integer ma, @RequestParam(required = false) String ketQua,
                               Principal nguoiDung, RedirectAttributes redirect) {
        return thucHien(redirect, "redirect:/admin/kho/doi-tra", () -> {
            service.tuChoiDoiTra(ma, nguoiDung.getName(), ketQua);
            return "Đã từ chối phiếu đổi trả #" + ma + ".";
        });
    }

    private String thucHien(RedirectAttributes redirect, String veTrang, ThaoTac thaoTac) {
        try {
            redirect.addFlashAttribute("thongBao", thaoTac.chay());
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        }
        return veTrang;
    }

    @FunctionalInterface
    private interface ThaoTac {
        String chay();
    }
}
