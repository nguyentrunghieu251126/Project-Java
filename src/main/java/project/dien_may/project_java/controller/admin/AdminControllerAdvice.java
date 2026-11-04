package project.dien_may.project_java.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.util.UriComponentsBuilder;
import project.dien_may.project_java.entity.TaiKhoan;
import project.dien_may.project_java.repository.TaiKhoanRepository;

import java.security.Principal;

/** Phần dùng chung cho mọi trang quản trị. */
@ControllerAdvice(basePackageClasses = AdminControllerAdvice.class)
public class AdminControllerAdvice {

    private final TaiKhoanRepository taiKhoanRepository;

    public AdminControllerAdvice(TaiKhoanRepository taiKhoanRepository) {
        this.taiKhoanRepository = taiKhoanRepository;
    }

    /** Cắt khoảng trắng; chuỗi rỗng thành null, nên ô lọc để trống nghĩa là không lọc. */
    @InitBinder
    public void catKhoangTrang(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @ModelAttribute("hoTenNguoiDung")
    public String hoTenNguoiDung(Principal principal) {
        return principal == null ? "" : taiKhoanRepository.findByEmailIgnoreCase(principal.getName())
                .map(TaiKhoan::getHoTen).orElse(principal.getName());
    }

    /** URL trang hiện tại giữ nguyên bộ lọc, chỉ cần nối thêm số trang vào cuối. */
    @ModelAttribute("urlPhanTrang")
    public String urlPhanTrang(HttpServletRequest request) {
        String url = UriComponentsBuilder.newInstance()
                .path(request.getRequestURI())
                .query(request.getQueryString())
                .replaceQueryParam("trang")
                .build().toUriString();
        return url + (url.contains("?") ? "&trang=" : "?trang=");
    }
}
