package project.dien_may.project_java.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import project.dien_may.project_java.entity.SoDiaChi;
import project.dien_may.project_java.service.muahang.SoDiaChiService;

import java.security.Principal;
import java.util.List;

/** TV3: đưa sổ địa chỉ vào trang Tài khoản (HoSoTaiKhoanController của TV2) mà không cần sửa controller đó. */
@ControllerAdvice(assignableTypes = HoSoTaiKhoanController.class)
public class SoDiaChiHoSoAdvice {

    private final SoDiaChiService soDiaChiService;

    public SoDiaChiHoSoAdvice(SoDiaChiService soDiaChiService) {
        this.soDiaChiService = soDiaChiService;
    }

    @ModelAttribute("soDiaChi")
    public List<SoDiaChi> soDiaChi(Principal principal) {
        return principal == null ? List.of() : soDiaChiService.danhSach(principal.getName());
    }
}
