package project.dien_may.project_java.controller.admin;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.dien_may.project_java.dto.SanPhamForm;
import project.dien_may.project_java.entity.TinhTrangTonKho;
import project.dien_may.project_java.repository.DanhMucRepository;
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.service.admin.NghiepVuException;
import project.dien_may.project_java.service.admin.SanPhamAdminService;

/** Quản lý sản phẩm (QT03 – QT06). */
@Controller
@RequestMapping("/admin/san-pham")
public class AdminSanPhamController {

    private static final String VIEW_FORM = "admin/san-pham/form";
    private static final String VE_DANH_SACH = "redirect:/admin/san-pham";

    private final SanPhamAdminService sanPhamService;
    private final SanPhamRepository sanPhamRepository;
    private final DanhMucRepository danhMucRepository;

    public AdminSanPhamController(SanPhamAdminService sanPhamService, SanPhamRepository sanPhamRepository,
                                  DanhMucRepository danhMucRepository) {
        this.sanPhamService = sanPhamService;
        this.sanPhamRepository = sanPhamRepository;
        this.danhMucRepository = danhMucRepository;
    }

    @GetMapping
    public String danhSach(@RequestParam(required = false) String tuKhoa,
                           @RequestParam(required = false) Integer maDanhMuc,
                           @RequestParam(required = false) String thuongHieu,
                           @RequestParam(required = false) Boolean dangKinhDoanh,
                           @RequestParam(required = false) TinhTrangTonKho tinhTrang,
                           @RequestParam(defaultValue = "1") int trang,
                           Model model) {
        model.addAttribute("trang", sanPhamRepository.timKiem(tuKhoa, maDanhMuc, thuongHieu, dangKinhDoanh,
                tinhTrang == null ? null : tinhTrang.name(),
                PhanTrang.tao(trang, Sort.by(Sort.Direction.DESC, "maSanPham"))));
        model.addAttribute("tuKhoa", tuKhoa);
        model.addAttribute("maDanhMuc", maDanhMuc);
        model.addAttribute("thuongHieu", thuongHieu);
        model.addAttribute("dangKinhDoanh", dangKinhDoanh);
        model.addAttribute("tinhTrang", tinhTrang);
        model.addAttribute("dsTinhTrang", TinhTrangTonKho.values());
        model.addAttribute("dsDanhMuc", danhMucRepository.findAllByOrderByTenDanhMucAsc());
        model.addAttribute("dsThuongHieu", sanPhamRepository.danhSachThuongHieu());
        return "admin/san-pham/danh-sach";
    }

    @GetMapping("/them")
    public String formThem(Model model) {
        return hienForm(new SanPhamForm(), model);
    }

    @PostMapping("/them")
    public String them(@Valid @ModelAttribute("form") SanPhamForm form, BindingResult ketQua, Model model,
                       RedirectAttributes redirect) {
        form.setMaSanPham(null);
        sanPhamService.kiemTraTrung(form, ketQua);
        if (ketQua.hasErrors()) {
            return hienForm(form, model);
        }
        try {
            sanPhamService.them(form);
        } catch (NghiepVuException | DataIntegrityViolationException e) {
            return baoLoiForm(e, form, model);
        }
        redirect.addFlashAttribute("thongBao", "Đã thêm sản phẩm " + form.getTenSanPham() + ".");
        return VE_DANH_SACH;
    }

    @GetMapping("/{ma}/sua")
    public String formSua(@PathVariable Integer ma, Model model) {
        return hienForm(sanPhamService.layForm(ma), model);
    }

    @PostMapping("/{ma}/sua")
    public String sua(@PathVariable Integer ma, @Valid @ModelAttribute("form") SanPhamForm form, BindingResult ketQua,
                      Model model, RedirectAttributes redirect) {
        form.setMaSanPham(ma);
        sanPhamService.kiemTraTrung(form, ketQua);
        if (ketQua.hasErrors()) {
            return hienForm(form, model);
        }
        try {
            sanPhamService.capNhat(ma, form);
        } catch (OptimisticLockingFailureException e) {
            redirect.addFlashAttribute("loi",
                    "Sản phẩm vừa được người khác cập nhật. Dữ liệu dưới đây là mới nhất, vui lòng sửa lại.");
            return "redirect:/admin/san-pham/" + ma + "/sua";
        } catch (NghiepVuException | DataIntegrityViolationException e) {
            return baoLoiForm(e, form, model);
        }
        redirect.addFlashAttribute("thongBao", "Đã cập nhật sản phẩm " + form.getTenSanPham() + ".");
        return VE_DANH_SACH;
    }

    @PostMapping("/{ma}/xoa")
    public String xoa(@PathVariable Integer ma, RedirectAttributes redirect) {
        try {
            if (sanPhamService.xoa(ma)) {
                redirect.addFlashAttribute("thongBao", "Đã xóa sản phẩm.");
            } else {
                redirect.addFlashAttribute("canhBao", "Sản phẩm đã có đơn hàng hoặc chứng từ nên không xóa được, "
                        + "hệ thống đã chuyển sang ngừng kinh doanh.");
            }
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("loi", "Không thể xóa sản phẩm vì đang có dữ liệu liên quan.");
        }
        return VE_DANH_SACH;
    }

    private String hienForm(SanPhamForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("dsDanhMuc", danhMucRepository.findAllByOrderByTenDanhMucAsc());
        return VIEW_FORM;
    }

    private String baoLoiForm(RuntimeException e, SanPhamForm form, Model model) {
        model.addAttribute("loi", e instanceof NghiepVuException ? e.getMessage()
                : "Dữ liệu bị trùng hoặc không hợp lệ, vui lòng kiểm tra lại.");
        return hienForm(form, model);
    }
}
