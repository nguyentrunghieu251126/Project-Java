package project.dien_may.project_java.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.service.TonKhoKhongDuException;
import project.dien_may.project_java.service.TonKhoService;

@Controller
public class KiemThuKhoController {
    private final SanPhamRepository sanPhamRepository;
    private final TonKhoService tonKhoService;

    public KiemThuKhoController(SanPhamRepository sanPhamRepository, TonKhoService tonKhoService) {
        this.sanPhamRepository = sanPhamRepository;
        this.tonKhoService = tonKhoService;
    }

    @GetMapping("/kiem-thu-kho")
    public String trangKiemThu(Model model) {
        model.addAttribute("danhSachSanPham", sanPhamRepository.findByDangKinhDoanhTrue());
        return "KiemThuKho";
    }

    @GetMapping("/kiem-thu-kho/ton-kho/{ma}")
    @ResponseBody
    public TonKhoResponse tonKhoThucTe(@PathVariable Integer ma) {
        var sanPham = sanPhamRepository.findById(ma)
                .filter(item -> item.isDangKinhDoanh())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new TonKhoResponse(sanPham.getMaSanPham(), sanPham.getTenSanPham(), sanPham.getSoLuong());
    }

    @PostMapping("/kiem-thu-kho/mua")
    @ResponseBody
    public ResponseEntity<KetQuaMuaResponse> muaMotSanPham(@RequestBody MuaRequest request) {
        if (request == null || request.maSanPham() == null || request.maSanPham() <= 0
                || request.nguoiMua() == null || request.nguoiMua().isBlank()
                || request.nguoiMua().length() > 80) {
            return ResponseEntity.badRequest().body(new KetQuaMuaResponse(
                    false, "Khách hàng", 1, null, "Thông tin người mua hoặc sản phẩm không hợp lệ.", null));
        }

        Integer maSanPham = request.maSanPham();
        sanPhamRepository.findById(maSanPham)
                .filter(item -> item.isDangKinhDoanh())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String luongXuLy = Thread.currentThread().getName();
        try {
            tonKhoService.truTonKho(maSanPham, 1);
            return ResponseEntity.ok(new KetQuaMuaResponse(
                    true, request.nguoiMua(), 1, tonKhoService.getTonKho(maSanPham),
                    "Đặt mua thành công.", luongXuLy));
        } catch (TonKhoKhongDuException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new KetQuaMuaResponse(
                    false, request.nguoiMua(), 1, tonKhoService.getTonKho(maSanPham),
                    "Không đủ hàng trong kho.", luongXuLy));
        } catch (OptimisticLockingFailureException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new KetQuaMuaResponse(
                    false, request.nguoiMua(), 1, tonKhoService.getTonKho(maSanPham),
                    "Kho vừa được cập nhật bởi một giao dịch khác. Vui lòng làm mới số lượng kho rồi thử lại.",
                    luongXuLy));
        }
    }

    public record TonKhoResponse(Integer maSanPham, String tenSanPham, Integer tonKho) {
    }

    public record MuaRequest(Integer maSanPham, String nguoiMua) {
    }

    public record KetQuaMuaResponse(
            boolean thanhCong, String nguoiMua, int soLuong, Integer tonKho, String thongDiep, String luongXuLy) {
    }
}
