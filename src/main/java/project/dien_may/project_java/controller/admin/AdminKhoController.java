package project.dien_may.project_java.controller.admin;

import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.dto.baocao.DemTrangThaiDon;
import project.dien_may.project_java.entity.TinhTrangTonKho;
import project.dien_may.project_java.entity.TrangThaiDonHang;
import project.dien_may.project_java.entity.TrangThaiYeuCauNhap;
import project.dien_may.project_java.repository.ChiTietDonHangRepository;
import project.dien_may.project_java.repository.DanhMucRepository;
import project.dien_may.project_java.repository.DonHangRepository;
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.repository.YeuCauNhapHangRepository;
import project.dien_may.project_java.service.admin.KhoAdminService;
import project.dien_may.project_java.service.admin.KhoAdminService.KetQuaNhap;
import project.dien_may.project_java.service.admin.NghiepVuException;

import java.security.Principal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/** Đơn hàng (QT17), tồn kho (QT18), duyệt yêu cầu nhập hàng (QT19 – QT21). */
@Controller
@RequestMapping("/admin/kho")
public class AdminKhoController {

    private final KhoAdminService khoService;
    private final DonHangRepository donHangRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final SanPhamRepository sanPhamRepository;
    private final DanhMucRepository danhMucRepository;
    private final YeuCauNhapHangRepository yeuCauRepository;

    public AdminKhoController(KhoAdminService khoService, DonHangRepository donHangRepository,
                              ChiTietDonHangRepository chiTietDonHangRepository, SanPhamRepository sanPhamRepository,
                              DanhMucRepository danhMucRepository, YeuCauNhapHangRepository yeuCauRepository) {
        this.khoService = khoService;
        this.donHangRepository = donHangRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.danhMucRepository = danhMucRepository;
        this.yeuCauRepository = yeuCauRepository;
    }

    @GetMapping("/don-hang")
    public String donHang(@RequestParam(required = false) String tuKhoa,
                          @RequestParam(required = false) TrangThaiDonHang trangThai,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
                          @RequestParam(defaultValue = "1") int trang,
                          Model model) {
        Map<TrangThaiDonHang, Long> soDon = new EnumMap<>(TrangThaiDonHang.class);
        for (TrangThaiDonHang t : TrangThaiDonHang.values()) {
            soDon.put(t, 0L);
        }
        donHangRepository.demTheoTrangThai().forEach((DemTrangThaiDon d) -> soDon.put(d.trangThai(), d.soDon()));

        model.addAttribute("trang", khoService.timDonHang(tuKhoa, trangThai, tuNgay, denNgay,
                PhanTrang.tao(trang, Sort.by(Sort.Direction.DESC, "ngayDat", "maDonHang"))));
        model.addAttribute("soDonTheoTrangThai", soDon);
        model.addAttribute("tuKhoa", tuKhoa);
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("tuNgay", tuNgay);
        model.addAttribute("denNgay", denNgay);
        return "admin/kho/don-hang";
    }

    @GetMapping("/don-hang/{ma}")
    public String chiTietDonHang(@PathVariable Integer ma, Model model) {
        model.addAttribute("donHang", donHangRepository.findById(ma)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        model.addAttribute("dsChiTiet", chiTietDonHangRepository.findByDonHangMaDonHang(ma));
        return "admin/kho/don-hang-chi-tiet";
    }

    @GetMapping("/ton-kho")
    public String tonKho(@RequestParam(required = false) String tuKhoa,
                         @RequestParam(required = false) Integer maDanhMuc,
                         @RequestParam(required = false) TinhTrangTonKho tinhTrang,
                         @RequestParam(defaultValue = "1") int trang,
                         Model model) {
        model.addAttribute("trang", sanPhamRepository.timKiem(tuKhoa, maDanhMuc, null, null,
                tinhTrang == null ? null : tinhTrang.name(),
                PhanTrang.tao(trang, Sort.by("soLuong", "maSanPham"))));
        model.addAttribute("tuKhoa", tuKhoa);
        model.addAttribute("maDanhMuc", maDanhMuc);
        model.addAttribute("tinhTrang", tinhTrang);
        model.addAttribute("dsTinhTrang", TinhTrangTonKho.values());
        model.addAttribute("dsDanhMuc", danhMucRepository.findAllByOrderByTenDanhMucAsc());
        return "admin/kho/ton-kho";
    }

    @PostMapping("/ton-kho/{ma}/yeu-cau-nhap")
    public String lapYeuCauNhap(@PathVariable Integer ma, @RequestParam(required = false) Integer soLuong,
                                @RequestParam(required = false) String quayVe,
                                Principal nguoiDung, RedirectAttributes redirect) {
        try {
            KetQuaNhap ketQua = khoService.lapYeuCauNhap(ma, soLuong, nguoiDung.getName());
            redirect.addFlashAttribute("thongBao", ketQua.daNhapKho()
                    ? "Đã nhập thêm " + soLuong + " sản phẩm vào kho (phiếu #" + ketQua.maYeuCau() + ")."
                    : "Đã gửi yêu cầu nhập hàng #" + ketQua.maYeuCau() + ".");
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        }
        // Chỉ nhận đúng giá trị đã biết để không bị lợi dụng chuyển hướng sang trang khác
        return "bao-cao".equals(quayVe) ? "redirect:/admin/bao-cao/ton-kho" : "redirect:/admin/kho/ton-kho";
    }

    @GetMapping("/yeu-cau-nhap")
    public String yeuCauNhap(@RequestParam(required = false) TrangThaiYeuCauNhap trangThai,
                             @RequestParam(defaultValue = "1") int trang,
                             Model model) {
        model.addAttribute("trang", yeuCauRepository.timKiem(trangThai,
                PhanTrang.tao(trang, Sort.by(Sort.Direction.DESC, "ngayYeuCau", "maYeuCauNhap"))));
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("dsTrangThai", TrangThaiYeuCauNhap.values());
        model.addAttribute("soChoDuyet", yeuCauRepository.countByTrangThai(TrangThaiYeuCauNhap.CHO_DUYET));
        return "admin/kho/yeu-cau-nhap";
    }

    @GetMapping("/yeu-cau-nhap/{ma}")
    public String chiTietYeuCau(@PathVariable Integer ma, Model model) {
        model.addAttribute("yeuCau", yeuCauRepository.findById(ma)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "admin/kho/yeu-cau-nhap-chi-tiet";
    }

    @PostMapping("/yeu-cau-nhap/{ma}/duyet")
    public String duyet(@PathVariable Integer ma, Principal nguoiDung, RedirectAttributes redirect) {
        try {
            khoService.duyet(ma, nguoiDung.getName());
            redirect.addFlashAttribute("thongBao", "Đã duyệt yêu cầu nhập hàng #" + ma + ".");
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        }
        return "redirect:/admin/kho/yeu-cau-nhap/" + ma;
    }

    @PostMapping("/yeu-cau-nhap/{ma}/tu-choi")
    public String tuChoi(@PathVariable Integer ma, @RequestParam(required = false) String lyDo, Principal nguoiDung,
                         RedirectAttributes redirect) {
        try {
            khoService.tuChoi(ma, nguoiDung.getName(), lyDo);
            redirect.addFlashAttribute("thongBao", "Đã từ chối yêu cầu nhập hàng #" + ma + ".");
        } catch (NghiepVuException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        }
        return "redirect:/admin/kho/yeu-cau-nhap/" + ma;
    }
}
